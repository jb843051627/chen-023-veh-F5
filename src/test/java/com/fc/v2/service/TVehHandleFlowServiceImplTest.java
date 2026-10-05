package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fc.v2.common.exception.VehHandleRejectException;
import com.fc.v2.mapper.auto.TVehHandleFlowMapper;
import com.fc.v2.mapper.auto.TVehHandleNoteMapper;
import com.fc.v2.mapper.auto.TVehEntMapper;
import com.fc.v2.mapper.custom.TVehHandleFlowBoardMapper;
import com.fc.v2.model.auto.TVehEnt;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.auto.TVehHandleNote;
import com.fc.v2.model.custom.VehHandleOpenForm;
import com.fc.v2.model.custom.VehHandleQuery;
import com.fc.v2.model.custom.VehHandleReplyMsg;
import com.fc.v2.model.custom.VehHandleReplyReceipt;
import com.fc.v2.service.impl.TVehHandleFlowServiceImpl;

/**
 * 严重报警处置单全口径测试（不启 Spring，桩掉数据层）。
 * 钉的事：定性四样缺一起不来；同车同类只容一张在办卷；进只进紧邻站、跳站不作数；
 * 知会几笔齐才进回告；回告钟点没走满下一站当没开；报文步次只当来意，入库回读听系统数的；
 * 同站第二遍不另起行；同秒两件事各记各段；退回一站攒头清零、旧按语不抹；
 * 复查入档与收口两说封尾只读、撤不掉；在办数与名单同一回点算。
 */
@ExtendWith(MockitoExtension.class)
public class TVehHandleFlowServiceImplTest {

    @Mock
    private TVehHandleFlowMapper vehHandleFlowMapper;
    @Mock
    private TVehHandleNoteMapper vehHandleNoteMapper;
    @Mock
    private TVehEntMapper vehEntMapper;
    @Mock
    private TVehHandleFlowBoardMapper boardMapper;

    @InjectMocks
    private TVehHandleFlowServiceImpl service;

    private TVehHandleFlow store;

    @BeforeEach
    public void wireRoundtrip() {
        // 模拟落库：insert 派 id、selectById/selectOne 回同一实体，updateById 不动
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.insert(any())).thenAnswer(inv -> {
            TVehHandleFlow r = inv.getArgument(0);
            r.setId(1L);
            store = r;
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.selectById(1L)).thenAnswer(inv -> store);
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.updateById(any())).thenAnswer(inv -> {
            store = inv.getArgument(0);
            return 1;
        });
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.selectOne(any())).thenAnswer(inv -> store);
        org.mockito.Mockito.lenient().when(vehHandleNoteMapper.selectList(any()))
                .thenReturn(new ArrayList<TVehHandleNote>());
        org.mockito.Mockito.lenient().when(vehEntMapper.selectList(any()))
                .thenReturn(new ArrayList<TVehEnt>());
        // 同车同类默认没有第二张在办卷；相斥用例另桩
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.selectCount(any())).thenReturn(0);
    }

    private VehHandleOpenForm form() {
        VehHandleOpenForm f = new VehHandleOpenForm();
        f.setVehCode("JC1001");
        f.setAlarmAt("2026-10-05 08:30:00");
        f.setMeasureKind("speed");
        f.setAlarmValue(new BigDecimal("92.500"));
        f.setAlarmKind("超速");
        f.setNotifyRequired(2);
        f.setReplyWaitHours(2);
        return f;
    }

    // ------------------------------------------------ 定性立单

    @Test
    public void openNeedsAllFourItems() {
        VehHandleOpenForm noVeh = form();
        noVeh.setVehCode(null);
        assertThrows(VehHandleRejectException.class, () -> service.openFlow(noVeh));

        VehHandleOpenForm badAt = form();
        badAt.setAlarmAt("not-a-time");
        assertThrows(VehHandleRejectException.class, () -> service.openFlow(badAt));

        VehHandleOpenForm badKind = form();
        badKind.setMeasureKind("weight");
        assertThrows(VehHandleRejectException.class, () -> service.openFlow(badKind));

        VehHandleOpenForm noValue = form();
        noValue.setAlarmValue(null);
        assertThrows(VehHandleRejectException.class, () -> service.openFlow(noValue));

        VehHandleOpenForm noAlarmKind = form();
        noAlarmKind.setAlarmKind(" ");
        assertThrows(VehHandleRejectException.class, () -> service.openFlow(noAlarmKind));

        verify(vehHandleFlowMapper, never()).insert(any());
    }

    @Test
    public void openCreatesActiveFlowAtDefineStationAndLogsOpenNote() {
        TVehHandleFlow r = service.openFlow(form());
        assertEquals(0, r.getStage().intValue());
        assertEquals(1, r.getStatus().intValue(), "建单即在办，未起只留给存量旧账");
        assertTrue(r.getBizNo().startsWith("CL"));
        assertEquals("JC1001", r.getVehCode());
        assertEquals(0, r.getNotifyCount().intValue());
        assertEquals(0, r.getReplyCount().intValue());
        // 头一段按语是定性段
        ArgumentCaptor<TVehHandleNote> cap = ArgumentCaptor.forClass(TVehHandleNote.class);
        verify(vehHandleNoteMapper).insert(cap.capture());
        assertEquals(VehHandleRuler.NOTE_OPEN, cap.getValue().getNoteKind());
        assertEquals(0, cap.getValue().getStage().intValue());
    }

    @Test
    public void sameVehicleSameKindAllowsOnlyOneOpenFlow() {
        when(vehHandleFlowMapper.selectCount(any())).thenReturn(1);
        VehHandleRejectException e = assertThrows(VehHandleRejectException.class,
                () -> service.openFlow(form()));
        assertTrue(e.getMessage().contains("第二张起不来"));
        verify(vehHandleFlowMapper, never()).insert(any());
    }

    // ------------------------------------------------ 进：紧邻站与各站守门

    @Test
    public void advanceGoesOneStationOnly() {
        service.openFlow(form());
        TVehHandleFlow r = service.advance(1L, "发知会");
        assertEquals(1, r.getStage().intValue());
        assertEquals(1, r.getStatus().intValue());
    }

    @Test
    public void advanceIntoReplyNeedsAllNotifyNotes() {
        service.openFlow(form());
        service.advance(1L, null);
        // 知会只落一笔（应齐两笔），推进不作数
        service.addNotifyNote(1L, "电话知会", new Date());
        assertNull(service.advance(1L, "想跳"));
        assertEquals(1, store.getStage().intValue());
    }

    @Test
    public void lastNotifyNoteDoesNotMoveItselfAdvanceOpensNextStationAndStartsClock() {
        service.openFlow(form());
        service.advance(1L, null); // 进知会站
        service.addNotifyNote(1L, "电话知会", new Date());
        TVehHandleFlow afterLast = service.addNotifyNote(1L, "书面知会", new Date());
        // 末笔齐了卷也停在知会站——往前走只留 advance 一道口子
        assertEquals(1, afterLast.getStage().intValue());
        assertEquals(2, afterLast.getNotifyCount().intValue());
        assertNull(afterLast.getNotifyDoneAt());

        Date at = new Date();
        TVehHandleFlow inReply = service.advance(1L, "知会齐了");
        assertEquals(2, inReply.getStage().intValue());
        assertEquals(at.getTime() / 1000, inReply.getNotifyDoneAt().getTime() / 1000,
                "钟点从进回告站那一刻起算");
        // 定性、进知会、两笔知会、进回告共五段；末笔知会与进站即便挤在同一秒也各是各的行
        verify(vehHandleNoteMapper, times(5)).insert(any(TVehHandleNote.class));
    }

    @Test
    public void duplicateNotifyAtReadyStationAddsNoLine() {
        service.openFlow(form());
        service.advance(1L, null);
        service.addNotifyNote(1L, "电话知会", new Date());
        service.addNotifyNote(1L, "书面知会", new Date());
        long notesReady = countInsertedNotes();
        // 已齐仍在知会站，再递一笔：不抛、不计数、卷上不另起行
        TVehHandleFlow r = service.addNotifyNote(1L, "又来一句", new Date());
        assertEquals(2, r.getNotifyCount().intValue());
        assertEquals(notesReady, countInsertedNotes());
    }

    @Test
    public void advanceIntoArchiveNeedsReplyAndElapsedHours() {
        openAtReplyStage();
        // 回话没到：推不动
        assertNull(service.advance(1L, null));

        acceptFirstReply(new Date(store.getNotifyDoneAt().getTime()), 9);
        // 回话齐了，钟点没走满（要守两个钟点）：下一站开着也当没开
        assertNull(service.advance(1L, null));
        assertEquals(2, store.getStage().intValue());

        // 钟点走到两个钟头以后（把起算点拨到三个钟头前），才进得了归档站；进站本身不封尾
        store.setNotifyDoneAt(new Date(System.currentTimeMillis() - 3 * 3600_000L));
        TVehHandleFlow r = service.advance(1L, null);
        assertEquals(3, r.getStage().intValue());
        assertEquals(1, r.getStatus().intValue());
    }

    // ------------------------------------------------ 回告报文只当来意

    private VehHandleReplyReceipt acceptFirstReply(Date at, int claimStage) {
        VehHandleReplyMsg msg = new VehHandleReplyMsg();
        msg.setBizNo(store.getBizNo());
        msg.setClaimStage(claimStage);
        msg.setContent("企业回话：已整改");
        msg.setAt(VehHandleRuler.ts(at));
        return service.acceptReply(msg);
    }

    @Test
    public void replyIsAcceptedOnlyAtReplyStationAndClaimIsJustIntent() {
        openAtReplyStage();
        VehHandleReplyReceipt rc = acceptFirstReply(new Date(store.getNotifyDoneAt().getTime() + 3600_000L), 9);
        assertTrue(rc.getAccepted());
        assertTrue(rc.getNewLine());
        assertFalse(rc.getClaimMatched(), "报文自报第9步与系统数的回告站不一致");
        assertFalse(rc.getNextOpen(), "钟点没走满，下一站当没开");
        // 入库按系统数出来的站记：当站仍是回告站，报文步次只挂来意栏
        assertEquals(2, store.getStage().intValue());
        assertEquals(9, store.getReplyClaimStage().intValue());
        assertEquals(1, store.getReplyCount().intValue());
        ArgumentCaptor<TVehHandleNote> cap = ArgumentCaptor.forClass(TVehHandleNote.class);
        verify(vehHandleNoteMapper, atLeastOnce()).insert(cap.capture());
        TVehHandleNote replyNote = cap.getAllValues().stream()
                .filter(n -> VehHandleRuler.NOTE_REPLY.equals(n.getNoteKind())).findFirst().orElse(null);
        assertEquals(2, replyNote.getStage().intValue(), "回告段落的是系统数出来的当站");
        assertEquals(9, replyNote.getClaimStage().intValue(), "来意步次只留在段上");
    }

    @Test
    public void replyBeforeReplyStationIsRejectedHoweverBigTheClaim() {
        service.openFlow(form());
        service.advance(1L, null); // 还在知会站
        VehHandleReplyMsg msg = new VehHandleReplyMsg();
        msg.setBizNo(store.getBizNo());
        msg.setClaimStage(3);
        msg.setContent("报文说自己已办到归档");
        VehHandleReplyReceipt rc = service.acceptReply(msg);
        assertFalse(rc.getAccepted());
        assertFalse(rc.getNewLine());
        assertEquals(1, store.getStage().intValue(), "绝不叫报文推着走站");
        ArgumentCaptor<TVehHandleNote> cap = ArgumentCaptor.forClass(TVehHandleNote.class);
        verify(vehHandleNoteMapper, atLeastOnce()).insert(cap.capture());
        assertTrue(cap.getAllValues().stream().noneMatch(n -> VehHandleRuler.NOTE_REPLY.equals(n.getNoteKind())),
                "被拒的回话卷上一段也不留");
        assertEquals(0, store.getReplyCount() == null ? 0 : store.getReplyCount().intValue());
    }

    @Test
    public void secondReplyAtSameStationKeepsTheFirstLine() {
        openAtReplyStage();
        acceptFirstReply(new Date(store.getNotifyDoneAt().getTime() + 3 * 3600_000L), 2);
        long notesAfterFirst = countInsertedNotes();
        VehHandleReplyReceipt rc = acceptFirstReply(new Date(store.getNotifyDoneAt().getTime() + 4 * 3600_000L), 2);
        assertTrue(rc.getAccepted());
        assertFalse(rc.getNewLine(), "同站第二遍不另起新行");
        assertEquals(notesAfterFirst, countInsertedNotes());
        assertEquals(1, store.getReplyCount().intValue());
        assertTrue(rc.getNextOpen());
    }

    @Test
    public void replyToUnknownBizOrClosedFlowIsRejected() {
        // 库里没有这张卷：原样退回
        VehHandleReplyMsg miss = new VehHandleReplyMsg();
        miss.setBizNo("NO-SUCH");
        org.mockito.Mockito.lenient().when(vehHandleFlowMapper.selectOne(any())).thenReturn(null);
        assertFalse(service.acceptReply(miss).getAccepted());
    }

    // ------------------------------------------------ 退：一站、清零、旧段不抹

    @Test
    public void rollbackOneStationClearsThatStationAndKeepsActive() {
        openAtReplyStage();
        acceptFirstReply(new Date(store.getNotifyDoneAt().getTime() + 3 * 3600_000L), 2);
        TVehHandleFlow r = service.rollback(1L, "知会没说清");
        assertEquals(1, r.getStage().intValue());
        assertEquals(1, r.getStatus().intValue());
        assertEquals(0, r.getNotifyCount().intValue(), "退回知会：几笔重新攒");
        assertEquals(0, r.getReplyCount().intValue());
        assertNull(r.getNotifyDoneAt());
        assertNull(r.getReplyClaimStage());
        // 旧按语一段不抹，退回另落一段 MOVE（段只增不删）
        verify(vehHandleNoteMapper, never()).delete(any());
    }

    @Test
    public void rollbackFromArchiveClearsOnlyReplyButKeepsClockStart() {
        openAtReplyStage();
        // 钟点起算点拨到三个钟头前，回话落了钟点也走满
        store.setNotifyDoneAt(new Date(System.currentTimeMillis() - 3 * 3600_000L));
        acceptFirstReply(new Date(), 2);
        service.advance(1L, null); // 进归档站
        assertEquals(3, store.getStage().intValue());
        TVehHandleFlow r = service.rollback(1L, null);
        assertEquals(2, r.getStage().intValue());
        assertEquals(0, r.getReplyCount().intValue(), "本趟回告清零重攒");
        assertEquals(2, r.getNotifyCount().intValue(), "知会那几笔不必重来");
    }

    @Test
    public void rollbackAtFirstStationAndForMissingFlowIsRejected() {
        service.openFlow(form());
        assertNull(service.rollback(1L, "退无可退"));
        assertNull(service.advance(99L, null));
    }

    // ------------------------------------------------ 复查入档与收口两说

    @Test
    public void reviewClosesAndSealsTheFlow() {
        reachArchive();
        TVehHandleFlow r = service.submitReview(1L, "复查无异常，入档", new Date());
        assertEquals(2, r.getStatus().intValue());
        assertEquals(VehHandleRuler.CLOSE_REVIEW, r.getCloseKind());
        // 收口即只读：推、退、改、撤全拒
        assertNull(service.advance(1L, "再推"));
        assertNull(service.rollback(1L, "再退"));
        assertFalse(service.updateContent(1L, "改内容"));
        assertFalse(service.remove(1L));
        verify(vehHandleFlowMapper, never()).deleteById(any());
    }

    @Test
    public void reviewNeedsOpinionAndArchiveStation() {
        openAtReplyStage();
        assertThrows(VehHandleRejectException.class, () -> service.submitReview(1L, "x", new Date()));
        reachArchive();
        assertThrows(VehHandleRejectException.class, () -> service.submitReview(1L, "  ", new Date()));
    }

    @Test
    public void closeOutsideWorksFromAnyActiveStationAndSeals() {
        service.openFlow(form());
        TVehHandleFlow r = service.closeOutside(1L, "移交公安", new Date());
        assertEquals(2, r.getStatus().intValue());
        assertEquals(VehHandleRuler.CLOSE_OUTSIDE, r.getCloseKind());
        assertFalse(service.remove(1L));
        // 收口后回告报文也不接
        VehHandleReplyMsg msg = new VehHandleReplyMsg();
        msg.setBizNo(r.getBizNo());
        assertFalse(service.acceptReply(msg).getAccepted());
    }

    @Test
    public void closeDeregisteredChecksTheRoster() {
        service.openFlow(form());
        // 卷上没落业户、名录查无：凭销户收不得
        assertThrows(VehHandleRejectException.class,
                () -> service.closeDeregistered(1L, null, new Date()));

        TVehEnt active = new TVehEnt();
        active.setSiteNo("YH09");
        active.setStatus(0);
        when(vehEntMapper.selectList(any())).thenReturn(Collections.singletonList(active));
        store.setSiteNo("YH09");
        assertThrows(VehHandleRejectException.class,
                () -> service.closeDeregistered(1L, null, new Date()));

        TVehEnt gone = new TVehEnt();
        gone.setSiteNo("YH09");
        gone.setStatus(1);
        when(vehEntMapper.selectList(any())).thenReturn(Collections.singletonList(gone));
        TVehHandleFlow r = service.closeDeregistered(1L, "已停业销户", new Date());
        assertEquals(2, r.getStatus().intValue());
        assertEquals(VehHandleRuler.CLOSE_DEREGISTERED, r.getCloseKind());
    }

    // ------------------------------------------------ 台账同算

    @Test
    public void boardCountComesFromTheSameSnapshotAsRows() {
        TVehHandleFlow row = new TVehHandleFlow();
        row.setId(7L);
        row.setStage(1);
        row.setStatus(1);
        row.setBoardTotal(41);
        row.setBoardActiveCount(7);
        when(boardMapper.selectBoardPage(any(VehHandleQuery.class)))
                .thenReturn(Arrays.asList(row));
        com.fc.v2.model.custom.VehHandleBoard board =
                service.openBoard(new VehHandleQuery());
        assertEquals(41L, board.getTotalCount());
        assertEquals(7, board.getActiveCount(), "屏上显的与回库点的是同一次算的");
        assertEquals(1, board.getFlows().size());
    }

    // ------------------------------------------------ 帮手

    /** 立单 → 知会两笔齐 → 推进回告站（钟点刚起算） */
    private void openAtReplyStage() {
        service.openFlow(form());
        service.advance(1L, null); // 进知会站
        service.addNotifyNote(1L, "电话知会", new Date());
        service.addNotifyNote(1L, "书面知会", new Date());
        service.advance(1L, null); // 知会齐，进回告站，钟点自此起算
        assertEquals(2, store.getStage().intValue());
    }

    /** 一路走到归档站：回告已落、钟点已走满 */
    private void reachArchive() {
        openAtReplyStage();
        // 知会齐那一刻拨到三个钟头前，钟点走满（推进守门按问话当下比）
        store.setNotifyDoneAt(new Date(System.currentTimeMillis() - 3 * 3600_000L));
        acceptFirstReply(new Date(), 2);
        service.advance(1L, null);
        assertEquals(3, store.getStage().intValue());
    }

    private long countInsertedNotes() {
        ArgumentCaptor<TVehHandleNote> cap = ArgumentCaptor.forClass(TVehHandleNote.class);
        verify(vehHandleNoteMapper, atLeastOnce()).insert(cap.capture());
        return cap.getAllValues().size();
    }
}
