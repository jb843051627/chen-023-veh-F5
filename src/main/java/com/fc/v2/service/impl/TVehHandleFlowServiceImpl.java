package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TVehEntMapper;
import com.fc.v2.mapper.auto.TVehHandleFlowMapper;
import com.fc.v2.mapper.auto.TVehHandleNoteMapper;
import com.fc.v2.model.auto.TVehEnt;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.auto.TVehHandleNote;
import com.fc.v2.model.custom.VehHandleFlowForm;
import com.fc.v2.model.custom.VehHandleFlowView;
import com.fc.v2.service.ITVehHandleFlowService;
import com.fc.v2.service.VehHandleRuler;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 严重报警处置单（四站状态机）——全册唯一的口子。
 *
 * 三件事在这一处立住：
 * 1. 往前走只留一处口子：进只许进紧邻一站（跳站不作数），报文步次只当来意，
 *    入库与回读的步次都由系统按已经过的站口数落（stage 本身即那个数）；
 * 2. 收过口的动不了：复查入档、非运输机构督办、业户销户三说收口即封尾，
 *    active_slot 让出（置 NULL），此后拒推、拒退、拒改、拒撤；
 * 3. 站站都回得来：一次退一站，被退站旧按语留在卷中原位，重走从该站新一回重新攒
 *    （新段一律压在卷末）。
 *
 * 另：一辆车同一类报警同时只容一张没收口的卷（库上唯一键兜同秒双进）；
 * 同一来路同站第二遍不另起新行（头一行字原封不动），同秒两个来路各记各的；
 * 屏上在办张数由 {@link #countOpen()} 回库现点。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TVehHandleFlowServiceImpl implements ITVehHandleFlowService {

    @javax.annotation.Resource
    private TVehHandleFlowMapper vehHandleFlowMapper;
    @javax.annotation.Resource
    private TVehHandleNoteMapper vehHandleNoteMapper;
    @javax.annotation.Resource
    private TVehEntMapper vehEntMapper;
    /** 按语段在独立新事务里落卷（撞同站同来路只回滚那一笔，不连累主单） */
    @javax.annotation.Resource
    private VehHandleNoteRecorder noteRecorder;

    @Override
    public TVehHandleFlow selectTVehHandleFlowById(Long id) {
        return this.vehHandleFlowMapper.selectById(id);
    }

    @Override
    public List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper) {
        return this.vehHandleFlowMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow open(VehHandleFlowForm form, Date at) {
        if (form == null || !fourEssentialsPresent(form)) {
            // 定性四样缺一样起不来：车辆营运代号、报警那一刻、当刻那个数、报警类别
            return null;
        }
        if (form.getNoticeCount() != null && form.getNoticeCount() < 0) {
            return null;
        }
        Date moment = at == null ? new Date() : at;
        String vehCode = form.getVehCode().trim();
        String alarmKind = form.getAlarmKind().trim();

        // 头一张仍在办或半道止住的，第二张起不来（先礼后兵：查一遍，库上唯一键再兜同秒双进）
        Integer openCount = vehHandleFlowMapper.selectCount(new QueryWrapper<TVehHandleFlow>()
                .eq("veh_code", vehCode)
                .eq("alarm_kind", alarmKind)
                .eq("active_slot", 1)
                .eq("del_flag", 0));
        if (openCount != null && openCount > 0) {
            return null;
        }

        TVehHandleFlow r = new TVehHandleFlow();
        r.setBizNo("CZ" + SnowflakeIdWorker.getUUID());
        r.setVehCode(vehCode);
        r.setAlarmKind(alarmKind);
        r.setAlarmAt(form.getAlarmAt());
        r.setMetricValue(form.getMetricValue());
        r.setSiteNo(form.getSiteNo() == null ? null : form.getSiteNo().trim());
        r.setStage(VehHandleRuler.STAGE_NAMING);
        r.setStatus(VehHandleRuler.STATUS_NOT_STARTED);
        r.setNoticeCount(form.getNoticeCount() == null ? 0 : form.getNoticeCount());
        r.setNoticeDone(0);
        r.setReplyDueHours(form.getReplyDueHours());
        r.setActiveSlot(1);
        r.setVisitNo(1);
        r.setDelFlag(0);
        try {
            vehHandleFlowMapper.insert(r);
        } catch (DuplicateKeyException e) {
            // 同一秒钟两条立单来路挤进来：库上唯一键只放一张，另一张当起不来
            return null;
        }
        // 定性站落头一段（四样在册，按语随卷）
        appendNote(r, VehHandleRuler.STAGE_NAMING, 1,
                form.sourceNoOrManual(), form.getNote(), moment);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleFlowView knock(Long id, VehHandleFlowForm form, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null || isTerminal(r)) {
            return null;
        }
        Date moment = at == null ? new Date() : at;
        int stage = currentStage(r);
        int visit = currentVisit(r);

        // 报文步次只当来意留痕，不挪一站；入库与回读的步次仍照系统数的
        if (form != null) {
            r.setClaimedStage(form.getClaimedStage());
        }
        String source = form == null ? VehHandleRuler.SOURCE_MANUAL : form.sourceNoOrManual();
        String text = form == null ? null : form.getNote();
        mergeNote(r, stage, visit, source, text, moment);

        if (stage == VehHandleRuler.STAGE_NOTICE) {
            // 知会笔数照本回卷上现点，不另信手填的数
            r.setNoticeDone(countNotes(r.getId(), stage, visit));
        }
        vehHandleFlowMapper.updateById(r);
        return buildView(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleFlowView advance(Long id, Integer claimedStage, String note, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null || isTerminal(r)) {
            return null;
        }
        int stage = currentStage(r);
        if (stage >= VehHandleRuler.STAGE_ARCHIVE) {
            return null;
        }
        if (stage == VehHandleRuler.STAGE_REPLY) {
            // 回告之后是复查入档：入档只看复查意见，走 archive 那道门，不由推进空迈
            return null;
        }
        // 报文说已办到第几步只当来意：不是紧邻的下一站，这笔跳站不作数
        if (claimedStage != null && !VehHandleRuler.claimedMatchesNext(stage, claimedStage)) {
            return null;
        }
        Date moment = at == null ? new Date() : at;
        String gate = gateBlocked(r, stage, moment);
        if (gate != null) {
            // 下一站开着也当没开：门槛不齐/钟点没走满
            return null;
        }

        int target = stage + 1;
        int targetVisit = nextVisitFor(r.getId(), target);

        UpdateWrapper<TVehHandleFlow> uw = new UpdateWrapper<TVehHandleFlow>()
                .eq("id", r.getId())
                .eq("stage", stage)
                .eq("del_flag", 0)
                .set("stage", target)
                .set("status", VehHandleRuler.statusAfterMove(target))
                .set("visit_no", targetVisit)
                .set("claimed_stage", claimedStage)
                .set("last_action", "进" + VehHandleRuler.stageName(target));
        if (target == VehHandleRuler.STAGE_REPLY) {
            // 知会出站那一刻起算回告钟点
            uw.set("notice_at", moment);
            r.setNoticeAt(moment);
        }
        int n = vehHandleFlowMapper.update(null, uw);
        if (n == 0) {
            // 同一秒钟被另一条来路先挪了：本站 CAS 没抢到，这笔不作数
            return null;
        }
        r.setStage(target);
        r.setStatus(VehHandleRuler.statusAfterMove(target));
        r.setVisitNo(targetVisit);
        r.setClaimedStage(claimedStage);
        r.setLastAction("进" + VehHandleRuler.stageName(target));
        // 进新站的抵达段由系统挂（system 来路）：不与报文段并线、不计知会笔数
        appendNote(r, target, targetVisit, VehHandleRuler.SOURCE_MOVE, note, moment);
        return buildView(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleFlowView rollback(Long id, String note, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null || isTerminal(r)) {
            // 收过口的挪一挪也不许
            return null;
        }
        int stage = currentStage(r);
        if (stage <= VehHandleRuler.STAGE_NAMING) {
            // 已停在头一站，不再往外退
            return null;
        }
        Date moment = at == null ? new Date() : at;
        int target = stage - 1;
        int targetVisit = nextVisitFor(r.getId(), target);

        UpdateWrapper<TVehHandleFlow> uw = new UpdateWrapper<TVehHandleFlow>()
                .eq("id", r.getId())
                .eq("stage", stage)
                .eq("del_flag", 0)
                .set("stage", target)
                .set("status", VehHandleRuler.STATUS_ACTIVE)
                .set("visit_no", targetVisit)
                .set("last_action", "退" + VehHandleRuler.stageName(target));
        if (target == VehHandleRuler.STAGE_NOTICE) {
            // 重走知会：钟点与笔数从头再攒，等这一会知会出站重新起算
            uw.set("notice_at", null).set("notice_done", 0);
            r.setNoticeAt(null);
            r.setNoticeDone(0);
        }
        int n = vehHandleFlowMapper.update(null, uw);
        if (n == 0) {
            return null;
        }
        r.setStage(target);
        r.setStatus(VehHandleRuler.STATUS_ACTIVE);
        r.setVisitNo(targetVisit);
        r.setLastAction("退" + VehHandleRuler.stageName(target));
        // 被退那站先前的按语压在卷中原位不动；退站缘由由系统挂到退回站新一回，重走从这重新攒
        appendNote(r, target, targetVisit, VehHandleRuler.SOURCE_MOVE, note, moment);
        return buildView(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleFlowView closeSpecial(Long id, String closeKind, String note, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null || isTerminal(r) || !VehHandleRuler.validSpecialClose(closeKind)) {
            return null;
        }
        if (VehHandleRuler.CLOSE_CANCELLED.equals(closeKind) && !entCancelled(r.getSiteNo())) {
            // 销户那两说照业户总名录核验：名录上没有、或还在册的，不能凭一句话收口
            return null;
        }
        Date moment = at == null ? new Date() : at;
        int n = closeRow(r, closeKind, moment);
        if (n == 0) {
            return null;
        }
        applyClosedOnMemory(r, closeKind, moment);
        appendNote(r, currentStage(r), currentVisit(r), VehHandleRuler.SOURCE_MOVE, note, moment);
        return buildView(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleFlowView archive(Long id, String reviewOpinion, String note, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null || isTerminal(r)) {
            return null;
        }
        int stage = currentStage(r);
        if (stage != VehHandleRuler.STAGE_REPLY) {
            // 没走到回告之后，谈不上复查入档
            return null;
        }
        Date moment = at == null ? new Date() : at;
        if (!VehHandleRuler.replyHoursSatisfied(r.getNoticeAt(), r.getReplyDueHours(), moment)) {
            // 钟点没走满，回话递得再齐也不入档
            return null;
        }
        if (reviewOpinion == null || reviewOpinion.trim().isEmpty()) {
            // 入档站只看复查意见落没落
            return null;
        }
        int n = vehHandleFlowMapper.update(null, new UpdateWrapper<TVehHandleFlow>()
                .eq("id", r.getId())
                .eq("stage", stage)
                .eq("del_flag", 0)
                .set("stage", VehHandleRuler.STAGE_ARCHIVE)
                .set("status", VehHandleRuler.STATUS_TERMINAL)
                .set("review_opinion", reviewOpinion.trim())
                .set("close_kind", VehHandleRuler.CLOSE_ARCHIVE)
                .set("closed_at", moment)
                .set("active_slot", null)
                .set("last_action", "复查入档"));
        if (n == 0) {
            return null;
        }
        r.setStage(VehHandleRuler.STAGE_ARCHIVE);
        r.setStatus(VehHandleRuler.STATUS_TERMINAL);
        r.setReviewOpinion(reviewOpinion.trim());
        applyClosedOnMemory(r, VehHandleRuler.CLOSE_ARCHIVE, moment);
        appendNote(r, VehHandleRuler.STAGE_ARCHIVE, 1, VehHandleRuler.SOURCE_MOVE, note, moment);
        return buildView(r, moment);
    }

    @Override
    public VehHandleFlowView getView(Long id, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        return buildView(r, at);
    }

    @Override
    public int countOpen() {
        // 回库现点：active_slot=1 的在办卷（含未起与半道止住），不拿屏上缓存的数凑
        Integer n = vehHandleFlowMapper.selectCount(new QueryWrapper<TVehHandleFlow>()
                .eq("active_slot", 1)
                .eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 收过口即只读，修改的位置一律锁死
        if (isTerminal(r)) {
            return false;
        }
        r.setContent(remark);
        return vehHandleFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 收过口的整张撤不掉（归档在册留查）；未起/在办的才撤得掉
        if (isTerminal(r)) {
            return false;
        }
        return vehHandleFlowMapper.deleteById(id) > 0;
    }

    // ------------------------------------------------------------------
    // 内里规矩：四样、门槛、收口、按语、回读
    // ------------------------------------------------------------------

    /** 定性四样：车辆营运代号、报警那一刻、当刻那个数、报警属哪一类，缺一样起不来 */
    private boolean fourEssentialsPresent(VehHandleFlowForm f) {
        return f.getVehCode() != null && !f.getVehCode().trim().isEmpty()
                && f.getAlarmAt() != null
                && f.getMetricValue() != null
                && f.getAlarmKind() != null && !f.getAlarmKind().trim().isEmpty()
                && VehHandleRuler.validAlarmKind(f.getAlarmKind().trim());
    }

    private boolean isTerminal(TVehHandleFlow r) {
        return r.getStatus() != null && r.getStatus() == VehHandleRuler.STATUS_TERMINAL;
    }

    private int currentStage(TVehHandleFlow r) {
        return r.getStage() == null ? VehHandleRuler.STAGE_NAMING : r.getStage();
    }

    private int currentVisit(TVehHandleFlow r) {
        return r.getVisitNo() == null || r.getVisitNo() < 1 ? 1 : r.getVisitNo();
    }

    /**
     * 各站出门的门槛只此一处判，回读视图与推进共用同一句话：
     * 定性四样齐；知会该齐的笔记齐；回告钟点走满（入档另看复查意见）。
     * 返回 null 为开着；否则是不开的缘由。
     */
    private String gateBlocked(TVehHandleFlow r, int stage, Date at) {
        if (stage == VehHandleRuler.STAGE_NAMING) {
            if (isBlank(r.getVehCode()) || r.getAlarmAt() == null
                    || r.getMetricValue() == null || isBlank(r.getAlarmKind())) {
                return "定性四样没齐，下一站开着也当没开";
            }
            return null;
        }
        if (stage == VehHandleRuler.STAGE_NOTICE) {
            int need = r.getNoticeCount() == null ? 0 : r.getNoticeCount();
            int done = r.getNoticeDone() == null ? 0 : r.getNoticeDone();
            if (done < need) {
                return "知会那几笔还没记全（" + done + "/" + need + "）";
            }
            return null;
        }
        if (stage == VehHandleRuler.STAGE_REPLY) {
            if (!VehHandleRuler.replyHoursSatisfied(r.getNoticeAt(), r.getReplyDueHours(), at)) {
                return "回告钟点没走满，企业回话递得再齐也进不去下一站";
            }
            return null;
        }
        return "已到末站";
    }

    /** 业户总名录上是否已销户（status=1）；查无此代号不算销户收口成立 */
    private boolean entCancelled(String siteNo) {
        if (siteNo == null || siteNo.trim().isEmpty()) {
            return false;
        }
        List<TVehEnt> rows = vehEntMapper.selectList(new QueryWrapper<TVehEnt>()
                .eq("site_no", siteNo.trim())
                .eq("del_flag", 0)
                .orderByDesc("status"));
        for (TVehEnt e : rows) {
            if (e.getStatus() != null && e.getStatus() == 1) {
                return true;
            }
        }
        return false;
    }

    /** 收口落库：情形封尾、收口名目与时刻落格、在办口让出（NULL）；CAS 防同秒双收 */
    private int closeRow(TVehHandleFlow r, String closeKind, Date at) {
        return vehHandleFlowMapper.update(null, new UpdateWrapper<TVehHandleFlow>()
                .eq("id", r.getId())
                .eq("del_flag", 0)
                .ne("status", VehHandleRuler.STATUS_TERMINAL)
                .set("status", VehHandleRuler.STATUS_TERMINAL)
                .set("close_kind", closeKind)
                .set("closed_at", at)
                .set("active_slot", null)
                .set("last_action", VehHandleRuler.closeKindName(closeKind) + "收口"));
    }

    private void applyClosedOnMemory(TVehHandleFlow r, String closeKind, Date at) {
        r.setStatus(VehHandleRuler.STATUS_TERMINAL);
        r.setCloseKind(closeKind);
        r.setClosedAt(at);
        r.setActiveSlot(null);
    }

    // ---- 按语卷 -------------------------------------------------------

    /**
     * 同站同来路第二遍：卷上留的还是头一遍那行字，不另起新行（字与时刻都不动）。
     * 两个来路（source_no 不同）各起一行，段序压到卷末，同一秒钟也不并、不漏。
     */
    private void mergeNote(TVehHandleFlow r, int stage, int visit, String source, String text, Date at) {
        List<TVehHandleNote> rows = listNotes(r.getId());
        for (TVehHandleNote n : rows) {
            if (n.getStage() == stage && n.getVisitNo() == visit
                    && source.equals(n.getSourceNo())) {
                // 头一遍那行字原封不动
                return;
            }
        }
        insertNote(r.getId(), stage, visit, source, text, at, rows);
    }

    /** 进新站/退站/收口挂段：新位置必有新段（同秒两条来路靠 source_no 各自立住） */
    private void appendNote(TVehHandleFlow r, int stage, int visit, String source, String text, Date at) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        List<TVehHandleNote> rows = listNotes(r.getId());
        for (TVehHandleNote n : rows) {
            if (n.getStage() == stage && n.getVisitNo() == visit
                    && source.equals(n.getSourceNo())) {
                return;
            }
        }
        insertNote(r.getId(), stage, visit, source, text, at, rows);
    }

    private void insertNote(Long flowId, int stage, int visit, String source, String text, Date at,
                            List<TVehHandleNote> existing) {
        noteRecorder.recordIfAbsent(flowId, stage, visit, source, text, at,
                maxSegOrdinal(existing) + 1);
    }

    private List<TVehHandleNote> listNotes(Long flowId) {
        try {
            return vehHandleNoteMapper.selectList(new QueryWrapper<TVehHandleNote>()
                    .eq("flow_id", flowId)
                    .eq("del_flag", 0));
        } catch (Exception e) {
            return new ArrayList<TVehHandleNote>();
        }
    }

    private int countNotes(Long flowId, int stage, int visit) {
        int c = 0;
        for (TVehHandleNote n : listNotes(flowId)) {
            if (n.getStage() != null && n.getStage() == stage
                    && n.getVisitNo() != null && n.getVisitNo() == visit
                    && !VehHandleRuler.SOURCE_MOVE.equals(n.getSourceNo())) {
                // 系统事件段（进/退抵达）不计入知会笔数，只数报上来的来路
                c++;
            }
        }
        return c;
    }

    /** 重走到某站是第几回：照卷上该站已有回数往下数，头一回为 1 */
    private int nextVisitFor(Long flowId, int stage) {
        int max = 0;
        for (TVehHandleNote n : listNotes(flowId)) {
            if (n.getStage() != null && n.getStage() == stage
                    && n.getVisitNo() != null && n.getVisitNo() > max) {
                max = n.getVisitNo();
            }
        }
        return max + 1;
    }

    private int maxSegOrdinal(List<TVehHandleNote> rows) {
        int max = 0;
        for (TVehHandleNote n : rows) {
            if (n.getSegOrdinal() != null && n.getSegOrdinal() > max) {
                max = n.getSegOrdinal();
            }
        }
        return max;
    }

    // ---- 回读（册面与回读同一份答案）----------------------------------

    private VehHandleFlowView buildView(TVehHandleFlow r, Date at) {
        VehHandleFlowView v = new VehHandleFlowView();
        v.setId(r.getId());
        v.setBizNo(r.getBizNo());
        v.setVehCode(r.getVehCode());
        v.setAlarmKind(r.getAlarmKind());
        v.setAlarmAt(VehHandleRuler.ts(r.getAlarmAt()));
        v.setMetricValue(r.getMetricValue());
        v.setSiteNo(r.getSiteNo());

        int stage = currentStage(r);
        int status = r.getStatus() == null ? VehHandleRuler.STATUS_NOT_STARTED : r.getStatus();
        // 当前站只照系统数的回
        v.setStage(stage);
        v.setStageName(VehHandleRuler.stageName(stage));
        v.setStatus(status);
        v.setStatusName(VehHandleRuler.statusName(status));
        v.setVisitNo(currentVisit(r));
        v.setClaimedStage(r.getClaimedStage());
        v.setClaimedDiffers(r.getClaimedStage() != null && r.getClaimedStage() != stage);

        v.setNoticeCount(r.getNoticeCount());
        v.setNoticeDone(r.getNoticeDone());
        v.setReplyHoursMet(VehHandleRuler.replyHoursSatisfied(
                r.getNoticeAt(), r.getReplyDueHours(), at));
        v.setReviewOpinionPresent(!isBlank(r.getReviewOpinion()));
        v.setClosed(status == VehHandleRuler.STATUS_TERMINAL);
        v.setCloseKind(r.getCloseKind());
        v.setCloseKindName(VehHandleRuler.closeKindName(r.getCloseKind()));
        v.setClosedAt(VehHandleRuler.ts(r.getClosedAt()));

        if (status == VehHandleRuler.STATUS_TERMINAL || stage >= VehHandleRuler.STAGE_ARCHIVE) {
            v.setNextOpen(Boolean.FALSE);
            v.setNextReason("已收口封尾，再递不接");
        } else {
            int next = stage + 1;
            v.setNextStage(next);
            v.setNextStageName(VehHandleRuler.stageName(next));
            if (next == VehHandleRuler.STAGE_ARCHIVE) {
                // 入档那一站只看复查意见落没落（钟点是到门口的前置门槛）
                String gate = gateBlocked(r, stage, at);
                v.setNextOpen(gate == null && !isBlank(r.getReviewOpinion()));
                v.setNextReason(gate != null ? gate
                        : (isBlank(r.getReviewOpinion()) ? "复查意见没落，入不了档" : null));
            } else {
                String gate = gateBlocked(r, stage, at);
                v.setNextOpen(gate == null);
                v.setNextReason(gate);
            }
        }

        List<TVehHandleNote> rows = listNotes(r.getId());
        Collections.sort(rows, new Comparator<TVehHandleNote>() {
            @Override
            public int compare(TVehHandleNote a, TVehHandleNote b) {
                int bySeg = Integer.valueOf(a.getSegOrdinal() == null ? 0 : a.getSegOrdinal())
                        .compareTo(b.getSegOrdinal() == null ? 0 : b.getSegOrdinal());
                if (bySeg != 0) {
                    return bySeg;
                }
                // 同秒两段：来路号不同，各记各的，先后也给个定序
                return String.valueOf(a.getSourceNo()).compareTo(String.valueOf(b.getSourceNo()));
            }
        });
        for (TVehHandleNote n : rows) {
            VehHandleFlowView.NoteView nv = new VehHandleFlowView.NoteView();
            nv.setSegOrdinal(n.getSegOrdinal());
            nv.setStage(n.getStage());
            nv.setStageName(VehHandleRuler.stageName(n.getStage()));
            nv.setVisitNo(n.getVisitNo());
            nv.setSourceNo(n.getSourceNo());
            nv.setNoteText(n.getNoteText());
            nv.setWrittenAt(VehHandleRuler.ts(n.getWrittenAt()));
            v.getNotes().add(nv);
        }
        return v;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}

