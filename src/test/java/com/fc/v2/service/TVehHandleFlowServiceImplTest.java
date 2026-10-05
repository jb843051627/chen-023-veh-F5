package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.fc.v2.mapper.auto.TVehEntMapper;
import com.fc.v2.mapper.auto.TVehHandleFlowMapper;
import com.fc.v2.mapper.auto.TVehHandleNoteMapper;
import com.fc.v2.model.auto.TVehEnt;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.auto.TVehHandleNote;
import com.fc.v2.model.custom.VehHandleFlowForm;
import com.fc.v2.model.custom.VehHandleFlowView;
import com.fc.v2.service.impl.TVehHandleFlowServiceImpl;
import com.fc.v2.service.impl.VehHandleNoteRecorder;

/**
 * 严重报警处置单口径单测（不启 Spring，桩掉数据层）。
 * 钉住：定性四样齐才起、同车同类一卷、报文步次只当来意、进紧邻退一站、
 * 知会笔数/回告钟点/复查意见三道门槛、同站同来路不另起新行、同秒两来路各记、
 * 非督办/销户两说收口、收口只读、CAS 同秒双挪、回库现点、回读同源。
 */
@ExtendWith(MockitoExtension.class)
public class TVehHandleFlowServiceImplTest {

    @Mock
    private TVehHandleFlowMapper flowMapper;
    @Mock
    private TVehHandleNoteMapper noteMapper;
    @Mock
    private TVehEntMapper entMapper;

    private TVehHandleFlowServiceImpl service;

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Date t(String s) {
        try {
            return FMT.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setUp() {
        service = new TVehHandleFlowServiceImpl();
        VehHandleNoteRecorder recorder = new VehHandleNoteRecorder();
        ReflectionTestUtils.setField(recorder, "vehHandleNoteMapper", noteMapper);
        ReflectionTestUtils.setField(service, "vehHandleFlowMapper", flowMapper);
        ReflectionTestUtils.setField(service, "vehHandleNoteMapper", noteMapper);
        ReflectionTestUtils.setField(service, "vehEntMapper", entMapper);
        ReflectionTestUtils.setField(service, "noteRecorder", recorder);
        // CAS 更新默认抢赢；同秒双挪用例再改成 0
        lenientUpdateOne();
    }

    private void lenientUpdateOne() {
        org.mockito.Mockito.lenient().when(flowMapper.update(isNull(), any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(flowMapper.updateById(any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(flowMapper.deleteById(any())).thenReturn(1);
    }

    private VehHandleFlowForm form() {
        VehHandleFlowForm f = new VehHandleFlowForm();
        f.setVehCode("YC001");
        f.setAlarmKind(VehHandleRuler.ALARM_SPEED);
        f.setAlarmAt(t("2026-10-05 08:00:00"));
        f.setMetricValue(new BigDecimal("109.50"));
        f.setSiteNo("YH00");
        f.setNoticeCount(2);
        f.setReplyDueHours(24);
        f.setNote("超速属实，定性");
        return f;
    }

    private TVehHandleFlow flow(int stage, int status) {
        TVehHandleFlow r = new TVehHandleFlow();
        r.setId(7L);
        r.setBizNo("CZ1");
        r.setVehCode("YC001");
        r.setAlarmKind(VehHandleRuler.ALARM_SPEED);
        r.setAlarmAt(t("2026-10-05 08:00:00"));
        r.setMetricValue(new BigDecimal("109.50"));
        r.setSiteNo("YH00");
        r.setStage(stage);
        r.setStatus(status);
        r.setNoticeCount(2);
        r.setNoticeDone(0);
        r.setReplyDueHours(24);
        r.setActiveSlot(1);
        r.setVisitNo(1);
        r.setDelFlag(0);
        return r;
    }

    private TVehHandleNote note(long id, int stage, int visit, String source, int seg, String at) {
        TVehHandleNote n = new TVehHandleNote();
        n.setId(id);
        n.setFlowId(7L);
        n.setStage(stage);
        n.setVisitNo(visit);
        n.setSourceNo(source);
        n.setSegOrdinal(seg);
        n.setNoteText("话" + id);
        n.setWrittenAt(t(at));
        return n;
    }

    private final List<TVehHandleNote> noteStore = new ArrayList<TVehHandleNote>();

    private void mockNotes(TVehHandleNote... notes) {
        noteStore.clear();
        noteStore.addAll(Arrays.asList(notes));
        org.mockito.Mockito.lenient().when(noteMapper.selectList(any())).thenAnswer(inv -> {
            // 回一份快照，像真库：insert 之后再查能查着新行
            return new ArrayList<TVehHandleNote>(noteStore);
        });
        org.mockito.Mockito.lenient().when(noteMapper.insert(any())).thenAnswer(inv -> {
            TVehHandleNote n = inv.getArgument(0);
            if (n == null) {
                // 重新打桩时 Mockito 以空参唤一声旧答案，不当真
                return 0;
            }
            if (n.getId() == null) {
                n.setId(100L + noteStore.size() + 1);
            }
            noteStore.add(n);
            return 1;
        });
    }

    // ---- 立单 --------------------------------------------------------

    @Test
    public void openNeedsAllFourEssentials() {
        Date now = t("2026-10-05 08:05:00");
        String[] cases = {"vehCode", "alarmAt", "metricValue", "alarmKind"};
        for (String missing : cases) {
            VehHandleFlowForm f = form();
            if ("vehCode".equals(missing)) {
                f.setVehCode("  ");
            } else if ("alarmAt".equals(missing)) {
                f.setAlarmAt(null);
            } else if ("metricValue".equals(missing)) {
                f.setMetricValue(null);
            } else {
                f.setAlarmKind("  ");
            }
            assertNull(service.open(f, now), "缺" + missing + "起不来");
        }
        // 类别不认（既不是车速也不是疲劳驾驶）也起不来
        VehHandleFlowForm badKind = form();
        badKind.setAlarmKind("smoke");
        assertNull(service.open(badKind, now));
        verify(flowMapper, never()).insert(any());
    }

    @Test
    public void openStartsAtNamingStageAndWritesFirstNote() {
        when(flowMapper.selectCount(any())).thenReturn(0);
        mockNotes();
        TVehHandleFlow r = service.open(form(), t("2026-10-05 08:05:00"));
        assertEquals(0, r.getStage().intValue());
        assertEquals(0, r.getStatus().intValue());
        assertEquals(1, r.getActiveSlot().intValue());
        assertEquals(1, r.getVisitNo().intValue());
        verify(noteMapper).insert(any());
    }

    @Test
    public void secondOpenForSameVehAndKindIsBlocked() {
        when(flowMapper.selectCount(any())).thenReturn(1);
        assertNull(service.open(form(), t("2026-10-05 08:06:00")));
        verify(flowMapper, never()).insert(any());
    }

    @Test
    public void simultaneousOpenRacingOnUniqueKeyLoses() {
        when(flowMapper.selectCount(any())).thenReturn(0);
        when(flowMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_veh_handle_open"));
        assertNull(service.open(form(), t("2026-10-05 08:06:30")));
        // 抢输的一张不立、按语也不落
        verify(noteMapper, never()).insert(any());
    }

    // ---- 敲门/按语/来意 ----------------------------------------------

    @Test
    public void secondKnockSameSourceKeepsFirstLineNoNewRow() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 头一遍：卷空，落一行
        mockNotes();
        VehHandleFlowForm f = new VehHandleFlowForm();
        f.setNote("定性原话");
        f.setSourceNo("dutyA");
        VehHandleFlowView v1 = service.knock(7L, f, t("2026-10-05 08:10:00"));
        assertEquals(1, v1.getNotes().size());

        // 第二遍同站同来路：卷上已有 A 那行，不另起新行
        mockNotes(note(1, 0, 1, "dutyA", 1, "2026-10-05 08:10:00"));
        VehHandleFlowForm f2 = new VehHandleFlowForm();
        f2.setNote("定性原话改了也不认");
        f2.setSourceNo("dutyA");
        VehHandleFlowView v2 = service.knock(7L, f2, t("2026-10-05 08:10:00"));
        assertEquals(1, v2.getNotes().size());
        assertEquals("话1", v2.getNotes().get(0).getNoteText());
        // noteMapper.insert 全测试只在头一遍发生一次
        verify(noteMapper, org.mockito.Mockito.times(1)).insert(any());
    }

    @Test
    public void twoSourcesAtSameSecondAreBothKeptNotMerged() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        VehHandleFlowForm a = new VehHandleFlowForm();
        a.setNote("来路甲");
        a.setSourceNo("srcJia");
        service.knock(7L, a, t("2026-10-05 08:10:00"));

        mockNotes(note(1, 0, 1, "srcJia", 1, "2026-10-05 08:10:00"));
        VehHandleFlowForm b = new VehHandleFlowForm();
        b.setNote("来路乙");
        b.setSourceNo("srcYi");
        VehHandleFlowView v = service.knock(7L, b, t("2026-10-05 08:10:00"));
        assertEquals(2, v.getNotes().size());
        verify(noteMapper, org.mockito.Mockito.times(2)).insert(any());
    }

    @Test
    public void claimedStageIsOnlyAnIntentAndSystemStageWins() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        VehHandleFlowForm f = new VehHandleFlowForm();
        f.setClaimedStage(3);
        f.setNote("报文说已到归档");
        VehHandleFlowView v = service.knock(7L, f, t("2026-10-05 08:11:00"));
        // 步次仍停在定性站，来意与系统数的不一致照实回读
        assertEquals(0, v.getStage().intValue());
        assertEquals(3, v.getClaimedStage().intValue());
        assertTrue(v.getClaimedDiffers());
        verify(flowMapper).updateById(any());
    }

    @Test
    public void knockOnClosedFlowRejected() {
        TVehHandleFlow r = flow(3, 2);
        when(flowMapper.selectById(7L)).thenReturn(r);
        assertNull(service.knock(7L, new VehHandleFlowForm(), t("2026-10-05 09:00:00")));
        verify(noteMapper, never()).insert(any());
    }

    // ---- 进 ----------------------------------------------------------

    @Test
    public void advanceGoesOnlyToAdjacentStageAndMovesStatusTogether() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        VehHandleFlowView v = service.advance(7L, null, "知会去", t("2026-10-05 08:20:00"));
        assertEquals(1, v.getStage().intValue());
        assertEquals(1, v.getStatus().intValue());
        assertEquals("知会", v.getStageName());
    }

    @Test
    public void advanceJumpByClaimedStageDoesNotCount() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 报文说办到回告(2)：跳站那一笔不作数
        assertNull(service.advance(7L, 2, "跳到回告", t("2026-10-05 08:21:00")));
        verify(flowMapper, never()).update(isNull(), any());
    }

    @Test
    public void noticeGateWaitsForAllNoticeNotes() {
        TVehHandleFlow r = flow(1, 1);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 两笔该齐，只记了一笔：下一站（回告）开着也当没开
        mockNotes(note(1, 1, 1, "dutyA", 1, "2026-10-05 08:30:00"));
        r.setNoticeDone(1);
        assertNull(service.advance(7L, null, "催回告", t("2026-10-05 09:00:00")));

        // 两笔齐了，才进回告；知会出站那一刻钉上，钟点自这儿起算
        mockNotes(note(1, 1, 1, "dutyA", 1, "2026-10-05 08:30:00"),
                  note(2, 1, 1, "dutyB", 2, "2026-10-05 08:31:00"));
        r.setNoticeDone(2);
        Date out = t("2026-10-05 09:05:00");
        VehHandleFlowView v = service.advance(7L, 2, "等回告", out);
        assertEquals(2, v.getStage().intValue());
        assertEquals(out, r.getNoticeAt());
    }

    @Test
    public void advanceFromReplyStageIsBlockedArchiveOnlyViaOpinion() {
        TVehHandleFlow r = flow(2, 1);
        r.setNoticeAt(t("2026-10-05 09:00:00"));
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        // 回告之后是复查入档，不由推进空迈
        assertNull(service.advance(7L, 3, "空迈归档", t("2026-10-07 09:00:00")));
        verify(flowMapper, never()).update(isNull(), any());
    }

    @Test
    public void concurrentAdvanceAtSameSecondOnlyOneWins() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        when(flowMapper.update(isNull(), any())).thenReturn(0);
        mockNotes();
        // 本站 CAS 没抢到：这笔不作数，按语也不落
        assertNull(service.advance(7L, null, "抢输", t("2026-10-05 08:20:01")));
        verify(noteMapper, never()).insert(any());
    }

    @Test
    public void systemMoveSegmentDoesNotCountAsNoticeNote() {
        TVehHandleFlow r = flow(1, 1);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 卷上只有系统进知会时挂的事件段：一笔知会都不算
        mockNotes(note(1, 1, 1, VehHandleRuler.SOURCE_MOVE, 1, "2026-10-05 08:20:00"));
        VehHandleFlowForm f = new VehHandleFlowForm();
        f.setNote("第一笔知会");
        f.setSourceNo("dutyA");
        VehHandleFlowView v = service.knock(7L, f, t("2026-10-05 08:30:00"));
        // 只数报文段：系统段不计，知会已记 1/2，下一站仍关着
        assertEquals(1, v.getNoticeDone().intValue());
        assertFalse(v.getNextOpen());
    }

    // ---- 退 ----------------------------------------------------------

    @Test
    public void rollbackGoesOneStageBackAndRestartsVisitWithOldNotesKept() {
        TVehHandleFlow r = flow(2, 1);
        r.setNoticeAt(t("2026-10-05 09:00:00"));
        r.setNoticeDone(2);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 知会站第一回的旧按语仍在卷上
        mockNotes(note(1, 1, 1, "dutyA", 1, "2026-10-05 08:30:00"),
                  note(2, 2, 1, "ent", 2, "2026-10-05 09:10:00"));
        VehHandleFlowView v = service.rollback(7L, "回告不实，重知会",
                t("2026-10-05 10:00:00"));
        assertEquals(1, v.getStage().intValue());
        assertEquals(1, v.getStatus().intValue());
        // 重走知会是第二回；钟点起算与笔数从头再攒
        assertEquals(2, r.getVisitNo().intValue());
        assertNull(r.getNoticeAt());
        assertEquals(0, r.getNoticeDone().intValue());
        // 旧段两笔都还在卷上，新段压到末位（第 3 段）
        assertEquals(3, v.getNotes().size());
        assertEquals(3, v.getNotes().get(2).getSegOrdinal().intValue());
        assertEquals(2, v.getNotes().get(2).getVisitNo().intValue());
    }

    @Test
    public void rollbackAtFirstStageRejected() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        assertNull(service.rollback(7L, "退无可退", t("2026-10-05 08:40:00")));
        verify(flowMapper, never()).update(isNull(), any());
    }

    // ---- 收口三说 ----------------------------------------------------

    @Test
    public void archiveNeedsHoursElapsedAndReviewOpinion() {
        TVehHandleFlow r = flow(2, 1);
        r.setNoticeAt(t("2026-10-05 09:00:00"));
        when(flowMapper.selectById(7L)).thenReturn(r);

        // 钟点没走满：企业回话递得再齐也入不了档
        mockNotes();
        assertNull(service.archive(7L, "复查没问题", "钟点未满先递", t("2026-10-06 08:59:59")));

        // 压线那刻走满了，但复查意见空着：入档只看这一格
        assertNull(service.archive(7L, "   ", "意见空着", t("2026-10-06 09:00:00")));

        // 两样都齐：入档即封尾、在办口让出
        VehHandleFlowView v = service.archive(7L, "复查合格，入档", "入档",
                t("2026-10-06 09:00:00"));
        assertEquals(3, v.getStage().intValue());
        assertEquals(2, v.getStatus().intValue());
        assertEquals(VehHandleRuler.CLOSE_ARCHIVE, v.getCloseKind());
        assertTrue(v.getClosed());
        assertNull(r.getActiveSlot());
    }

    @Test
    public void closeSpecialNotTransportClosesFromAnyStage() {
        TVehHandleFlow r = flow(1, 1);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        VehHandleFlowView v = service.closeSpecial(7L, VehHandleRuler.CLOSE_NOT_TRANSPORT,
                "不归本机构督办", t("2026-10-05 12:00:00"));
        assertEquals(2, v.getStatus().intValue());
        assertEquals("不归运输机构督办", v.getCloseKindName());
        assertFalse(v.getNextOpen());
    }

    @Test
    public void closeSpecialCancelledVerifiedAgainstEntBook() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();

        // 名录上还在册：不能凭销户收口
        TVehEnt active = new TVehEnt();
        active.setSiteNo("YH00");
        active.setStatus(0);
        when(entMapper.selectList(any())).thenReturn(java.util.Collections.singletonList(active));
        assertNull(service.closeSpecial(7L, VehHandleRuler.CLOSE_CANCELLED,
                "销户", t("2026-10-05 12:10:00")));

        // 名录标了销户：收口成立
        TVehEnt gone = new TVehEnt();
        gone.setSiteNo("YH00");
        gone.setStatus(1);
        when(entMapper.selectList(any())).thenReturn(java.util.Collections.singletonList(gone));
        VehHandleFlowView v = service.closeSpecial(7L, VehHandleRuler.CLOSE_CANCELLED,
                "业户已销户", t("2026-10-05 12:11:00"));
        assertEquals(VehHandleRuler.CLOSE_CANCELLED, v.getCloseKind());
        assertEquals(2, v.getStatus().intValue());
    }

    @Test
    public void bogusCloseKindRejected() {
        TVehHandleFlow r = flow(0, 0);
        when(flowMapper.selectById(7L)).thenReturn(r);
        assertNull(service.closeSpecial(7L, "whatever", "x", t("2026-10-05 12:20:00")));
        verify(flowMapper, never()).update(isNull(), any());
    }

    // ---- 收口即只读 --------------------------------------------------

    @Test
    public void closedFlowIsFrozenAdvanceRollbackChangeRemoveKnockAllRejected() {
        TVehHandleFlow r = flow(3, 2);
        r.setCloseKind(VehHandleRuler.CLOSE_ARCHIVE);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        assertNull(service.advance(7L, null, "再推", t("2026-10-06 10:00:00")));
        assertNull(service.rollback(7L, "再退", t("2026-10-06 10:00:00")));
        assertNull(service.knock(7L, new VehHandleFlowForm(), t("2026-10-06 10:00:00")));
        assertNull(service.archive(7L, "再入档", "入档再议", t("2026-10-06 10:00:00")));
        assertNull(service.closeSpecial(7L, VehHandleRuler.CLOSE_NOT_TRANSPORT,
                "再收口", t("2026-10-06 10:00:00")));
        assertFalse(service.updateContent(7L, "改内容"));
        assertFalse(service.remove(7L));
        verify(flowMapper, never()).update(isNull(), any());
        verify(flowMapper, never()).updateById(any());
        verify(flowMapper, never()).deleteById(any());
    }

    @Test
    public void activeFlowContentEditableAndRemovable() {
        TVehHandleFlow r = flow(1, 1);
        when(flowMapper.selectById(7L)).thenReturn(r);
        assertTrue(service.updateContent(7L, "补一句"));
        assertTrue(service.remove(7L));
    }

    @Test
    public void missingFlowAnswersNullOrFalse() {
        when(flowMapper.selectById(9L)).thenReturn(null);
        assertNull(service.advance(9L, null, "x", t("2026-10-05 08:00:00")));
        assertNull(service.rollback(9L, "x", t("2026-10-05 08:00:00")));
        assertNull(service.knock(9L, new VehHandleFlowForm(), t("2026-10-05 08:00:00")));
        assertNull(service.archive(9L, "x", "再议", t("2026-10-05 08:00:00")));
        assertNull(service.closeSpecial(9L, VehHandleRuler.CLOSE_NOT_TRANSPORT,
                "x", t("2026-10-05 08:00:00")));
        assertNull(service.getView(9L, t("2026-10-05 08:00:00")));
        assertFalse(service.remove(9L));
        assertFalse(service.updateContent(9L, "x"));
    }

    // ---- 回读同源 + 回库现点 -----------------------------------------

    @Test
    public void viewAnswersTheFourQuestionsFromSystemStageOnly() {
        TVehHandleFlow r = flow(1, 1);
        r.setNoticeDone(1);
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes(note(1, 1, 1, "dutyA", 1, "2026-10-05 08:30:00"));
        VehHandleFlowView v = service.getView(7L, t("2026-10-05 09:00:00"));
        assertEquals(1, v.getStage().intValue());
        assertEquals("知会", v.getStageName());
        assertEquals(2, v.getNextStage().intValue());
        assertFalse(v.getNextOpen());
        assertTrue(v.getNextReason().contains("没记全"));
        assertFalse(v.getClosed());

        // 笔数补齐：下一站（回告）开门
        r.setNoticeDone(2);
        VehHandleFlowView open = service.getView(7L, t("2026-10-05 09:00:00"));
        assertTrue(open.getNextOpen());
        assertNull(open.getNextReason());
    }

    @Test
    public void viewAtReplyStageShowsHoursAndOpinionGate() {
        TVehHandleFlow r = flow(2, 1);
        r.setNoticeAt(t("2026-10-05 09:00:00"));
        when(flowMapper.selectById(7L)).thenReturn(r);
        mockNotes();
        // 钟点没走满
        VehHandleFlowView early = service.getView(7L, t("2026-10-06 08:00:00"));
        assertFalse(early.getReplyHoursMet());
        assertFalse(early.getNextOpen());
        // 钟点走满、复查意见没落：入档站仍关着
        VehHandleFlowView due = service.getView(7L, t("2026-10-06 09:00:00"));
        assertTrue(due.getReplyHoursMet());
        assertFalse(due.getNextOpen());
        assertTrue(due.getNextReason().contains("复查意见"));
        // 意见落了：开门
        r.setReviewOpinion("复查合格");
        VehHandleFlowView ready = service.getView(7L, t("2026-10-06 09:00:00"));
        assertTrue(ready.getNextOpen());
    }

    @Test
    public void countOpenIsRecountedFromStore() {
        when(flowMapper.selectCount(any())).thenReturn(3);
        assertEquals(3, service.countOpen());
        // 立一张加一、收一张减一是库里 active_slot 的同一处账；屏上与回库同一次算
        verify(flowMapper).selectCount(any());
    }

    @Test
    public void noteSegmentCountMatchesWalkbackFromArchive() {
        TVehHandleFlow r = flow(3, 2);
        r.setCloseKind(VehHandleRuler.CLOSE_ARCHIVE);
        when(flowMapper.selectById(7L)).thenReturn(r);
        // 定性、知会、回告、归档各一段——从归档往回数当班簿几笔，段数对得住
        mockNotes(note(1, 0, 1, "dutyA", 1, "2026-10-05 08:05:00"),
                  note(2, 1, 1, "dutyA", 2, "2026-10-05 08:30:00"),
                  note(3, 2, 1, "ent", 3, "2026-10-05 12:00:00"),
                  note(4, 3, 1, "dutyA", 4, "2026-10-06 09:05:00"));
        VehHandleFlowView v = service.getView(7L, t("2026-10-06 09:10:00"));
        assertEquals(4, v.getNotes().size());
        assertEquals(0, v.getNotes().get(0).getStage().intValue());
        assertEquals(3, v.getNotes().get(3).getStage().intValue());
    }
}
