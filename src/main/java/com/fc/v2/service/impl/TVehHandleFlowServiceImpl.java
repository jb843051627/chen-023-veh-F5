package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fc.v2.common.exception.VehHandleRejectException;
import com.fc.v2.mapper.auto.TVehHandleFlowMapper;
import com.fc.v2.mapper.auto.TVehHandleNoteMapper;
import com.fc.v2.mapper.auto.TVehEntMapper;
import com.fc.v2.mapper.custom.TVehHandleFlowBoardMapper;
import com.fc.v2.model.auto.TVehEnt;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.auto.TVehHandleNote;
import com.fc.v2.model.custom.VehHandleBoard;
import com.fc.v2.model.custom.VehHandleOpenForm;
import com.fc.v2.model.custom.VehHandleQuery;
import com.fc.v2.model.custom.VehHandleReplyMsg;
import com.fc.v2.model.custom.VehHandleReplyReceipt;
import com.fc.v2.service.ITVehHandleFlowService;
import com.fc.v2.service.VehHandleRuler;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 严重报警处置单 Service业务层处理（一张卷从哪儿起、到哪儿止，只由这一处作答）。
 *
 * 四站：定性 → 知会 → 回告 → 归档。立住的三件事：
 * 1. 往前走只留一处口子——{@link #moveTo} 是步次与情形同源的唯一落法，进只进紧邻站，
 *    报文里自报的步次只当来意，谁也不许事先替卷把站次定好；
 * 2. 收过口的动不了——status=已封尾即只读，推/退/改/撤/再接回告一律拒；
 * 3. 站站都回得来——{@link #rollback} 一次退一站，被退那站的攒头清零、旧按语留痕压末位重攒。
 *
 * 册面（台账/回读）与回告报文两边说的是同一张卷：当站、下一站开不开、收口后接不接、
 * 同站重报另不另起行，四问只认 {@link VehHandleRuler} 与本类给出的答案，此外没有第二处。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehHandleFlowServiceImpl implements ITVehHandleFlowService {

    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @javax.annotation.Resource
    private TVehHandleFlowMapper vehHandleFlowMapper;
    @javax.annotation.Resource
    private TVehHandleNoteMapper vehHandleNoteMapper;
    @javax.annotation.Resource
    private TVehEntMapper vehEntMapper;
    @javax.annotation.Resource
    private TVehHandleFlowBoardMapper boardMapper;

    // ---------------------------------------------------------------- 回读

    @Override
    public TVehHandleFlow selectTVehHandleFlowById(Long id) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        attachRead(r, new Date(), true);
        return r;
    }

    @Override
    public List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper) {
        return vehHandleFlowMapper.selectList(queryWrapper);
    }

    @Override
    public VehHandleBoard openBoard(VehHandleQuery query) {
        VehHandleQuery q = query == null ? new VehHandleQuery() : query;
        int pageNum = q.getPage() != null && q.getPage() > 0 ? q.getPage() : 1;
        int pageSize = q.getLimit() != null && q.getLimit() > 0 ? q.getLimit() : DEFAULT_PAGE_SIZE;

        // 分页切同一条 SQL 的同一批结果：全册在办卷数是这条 SELECT 同一快照里的标量子查询，
        // 与一张一张点出来的卷同一次算成，不随筛选借行，也不许别处另点一遍。
        PageHelper.startPage(pageNum, pageSize, false);
        List<TVehHandleFlow> rows = boardMapper.selectBoardPage(q);
        PageInfo<TVehHandleFlow> page = new PageInfo<TVehHandleFlow>(rows);

        VehHandleBoard board = new VehHandleBoard();
        board.setFlows(page.getList());
        Date now = new Date();
        if (!page.getList().isEmpty()) {
            TVehHandleFlow first = page.getList().get(0);
            // 本屏卷数与全册在办卷数都取自这一条 SELECT 的同一快照，不由服务层另点
            board.setTotalCount(first.getBoardTotal() == null ? page.getTotal() : first.getBoardTotal());
            board.setActiveCount(first.getBoardActiveCount() == null ? 0 : first.getBoardActiveCount());
            for (TVehHandleFlow r : page.getList()) {
                // 每卷的"下一站开不开"照唯一一把尺回挂，台账与单卷回读同一个说法
                attachRead(r, now, false);
            }
        }
        return board;
    }

    /** 回读挂齐：按语段按时刻序、业户全称、下一站开不开（册面与卷面同一处算） */
    private void attachRead(TVehHandleFlow r, Date at, boolean withNotes) {
        if (withNotes) {
            List<TVehHandleNote> notes = vehHandleNoteMapper.selectList(
                    new QueryWrapper<TVehHandleNote>()
                            .eq("flow_id", r.getId())
                            .eq("del_flag", 0)
                            .orderByAsc("note_at")
                            .orderByAsc("id"));
            r.setNotes(notes);
            TVehEnt ent = findEnt(r.getSiteNo());
            if (ent != null) {
                r.setSiteName(ent.getSiteName());
            }
        }
        String block = VehHandleRuler.nextBlockReason(r, at);
        r.setNextOpen(block == null);
        r.setNextBlockReason(block);
    }

    // ---------------------------------------------------------------- 立单（定性站）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow openFlow(VehHandleOpenForm form) {
        if (form == null) {
            throw new VehHandleRejectException("form", "空卷递进来，立不起来");
        }
        // 定性四样：车辆营运代号、报警那一刻、当刻那个数（车速还是连续驾驶时长）、报警类别，缺一样起不来
        String vehCode = trimToNull(form.getVehCode());
        if (vehCode == null) {
            throw new VehHandleRejectException("vehCode", "车辆营运代号没落，定性这一站起不来");
        }
        Date alarmAt = parseWallClock(form.getAlarmAt());
        if (alarmAt == null) {
            throw new VehHandleRejectException("alarmAt", "报警发生那一刻没落或写法不对，认 " + FMT_TS);
        }
        String measureKind = trimToNull(form.getMeasureKind());
        if (measureKind == null
                || (!VehHandleRuler.MEASURE_SPEED.equals(measureKind)
                && !VehHandleRuler.MEASURE_DURATION.equals(measureKind))) {
            throw new VehHandleRejectException("measureKind", "当刻那个数量的是车速(speed)还是连续驾驶时长(duration)，没落清楚");
        }
        BigDecimal alarmValue = form.getAlarmValue();
        if (alarmValue == null) {
            throw new VehHandleRejectException("alarmValue", "报警当刻那个数（车速还是连续驾驶时长）没落，定性起不来");
        }
        String alarmKind = trimToNull(form.getAlarmKind());
        if (alarmKind == null) {
            throw new VehHandleRejectException("alarmKind", "报警属哪一类没落，定性这一站起不来");
        }

        // 一辆车的同一类报警，同时只容一张没收口的卷：头一张仍在办、半道止住或存量未起的，第二张起不来
        Integer dup = vehHandleFlowMapper.selectCount(new QueryWrapper<TVehHandleFlow>()
                .eq("veh_code", vehCode)
                .eq("alarm_kind", alarmKind)
                .eq("del_flag", 0)
                .and(w -> w.isNull("status").or().ne("status", VehHandleRuler.STATUS_TERMINAL)));
        if (dup != null && dup > 0) {
            throw new VehHandleRejectException("vehCode",
                    "这辆车这一类报警已有一张没收口的卷，头一张不收，第二张起不来");
        }

        Date now = new Date();
        TVehHandleFlow r = new TVehHandleFlow();
        // 处置单代号系统排号，卷上自带号递来不认
        r.setBizNo("CL" + SnowflakeIdWorker.getUUID());
        r.setStage(VehHandleRuler.STAGE_DEFINE);
        // 建单即在办（未起只留给建了没动过的存量旧账），起一张在办数加一
        r.setStatus(VehHandleRuler.STATUS_ACTIVE);
        r.setVehCode(vehCode);
        r.setAlarmAt(alarmAt);
        r.setMeasureKind(measureKind);
        r.setAlarmValue(alarmValue);
        r.setAlarmKind(alarmKind);
        r.setSiteNo(trimToNull(form.getSiteNo()));
        r.setNotifyRequired(form.getNotifyRequired() == null || form.getNotifyRequired() < 1
                ? 1 : form.getNotifyRequired());
        r.setReplyWaitHours(form.getReplyWaitHours() == null || form.getReplyWaitHours() < 0
                ? 0 : form.getReplyWaitHours());
        r.setNotifyCount(0);
        r.setReplyCount(0);
        r.setContent(trimToNull(form.getContent()));
        r.setRemark(trimToNull(form.getRemark()));
        r.setDelFlag(0);
        vehHandleFlowMapper.insert(r);

        // 定性段：四样落成头一段按语（哪一站、什么话、哪一刻）
        StringBuilder openText = new StringBuilder();
        openText.append("定性：车").append(vehCode)
                .append("，报警于").append(VehHandleRuler.ts(alarmAt))
                .append("，当刻").append(VehHandleRuler.MEASURE_SPEED.equals(measureKind) ? "车速" : "连续驾驶时长")
                .append(alarmValue.stripTrailingZeros().toPlainString())
                .append(VehHandleRuler.MEASURE_SPEED.equals(measureKind) ? "km/h" : "小时")
                .append("，类别[").append(alarmKind).append("]");
        if (r.getContent() != null) {
            openText.append("；").append(r.getContent());
        }
        addNote(r.getId(), VehHandleRuler.STAGE_DEFINE, VehHandleRuler.NOTE_OPEN, openText.toString(), null, now);
        return attachAndReturn(r, now);
    }

    // ---------------------------------------------------------------- 进 / 退（旧口子，模样不改）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow advance(Long id, String remark) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        Date now = new Date();
        // 已封尾：归档在册留查，推不动
        if (VehHandleRuler.isTerminal(r)) {
            return null;
        }
        int stage = VehHandleRuler.stageOf(r);
        if (stage >= VehHandleRuler.STAGE_ARCHIVE) {
            // 已在归档站：没有下一站可进，收口只等复查意见落下
            return null;
        }
        // 下一站开不开只认尺子：知会看几笔全没全，回告守钟点（回话齐了钟点没走满也当没开）
        String block = VehHandleRuler.nextBlockReason(r, now);
        if (block != null) {
            return null;
        }
        // 只许进紧邻的那一档，跳站那一笔不作数——唯一落法 moveTo 一次只挪一档
        moveTo(r, stage + 1, now);
        r.setLastAction(trimToNull(remark));
        vehHandleFlowMapper.updateById(r);
        addNote(r.getId(), stage + 1, VehHandleRuler.NOTE_MOVE,
                "进" + stageName(stage + 1) + (remark == null ? "" : "：" + remark), null, now);
        return attachAndReturn(r, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow rollback(Long id, String remark) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 收过口的动不了：封尾卷连退也不许
        if (VehHandleRuler.isTerminal(r)) {
            return null;
        }
        int stage = VehHandleRuler.stageOf(r);
        if (stage <= VehHandleRuler.STAGE_DEFINE) {
            // 头一档退无可退
            return null;
        }
        int back = stage - 1;
        // 被退那一站先前写的按语留在卷上压到末位（不抹段），重走这一站从这一站重新攒：
        if (back == VehHandleRuler.STAGE_NOTIFY) {
            // 退回知会：知会几笔、回告几笔、钟点起算点全部清零，重走时重新攒
            r.setNotifyCount(0);
            r.setReplyCount(0);
            r.setReplyClaimStage(null);
            r.setNotifyDoneAt(null);
        } else if (back == VehHandleRuler.STAGE_REPLY) {
            // 退回回告：本趟回告清零重攒，钟点起算点不动（知会那几笔已齐，不必重知会）
            r.setReplyCount(0);
            r.setReplyClaimStage(null);
        }
        moveTo(r, back, new Date());
        r.setLastAction(trimToNull(remark));
        vehHandleFlowMapper.updateById(r);
        addNote(r.getId(), back, VehHandleRuler.NOTE_MOVE,
                "退回" + stageName(back) + "，本站重攒" + (remark == null ? "" : "：" + remark), null, new Date());
        return attachAndReturn(r, new Date());
    }

    /**
     * 步次与情形同源的唯一落法：挪到哪一档，情形就随那一档定。
     * 凡挪步（含退回头一档重办）都算在办；封尾只由收口那一处落，挪步本身永不封尾。
     * 进回告站时钉下钟点起算点（知会齐那一刻）。谁挪步都走这一处，不许绕开另写 stage/status。
     */
    private void moveTo(TVehHandleFlow r, int newStage, Date at) {
        r.setStage(newStage);
        r.setStatus(VehHandleRuler.STATUS_ACTIVE);
        if (newStage == VehHandleRuler.STAGE_REPLY && r.getNotifyDoneAt() == null) {
            r.setNotifyDoneAt(at);
        }
    }

    // ---------------------------------------------------------------- 知会站

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow addNotifyNote(Long id, String text, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (VehHandleRuler.isTerminal(r)) {
            throw new VehHandleRejectException("status", "这张卷已经收口，知会笔记落不进去了");
        }
        int stage = VehHandleRuler.stageOf(r);
        if (stage != VehHandleRuler.STAGE_NOTIFY) {
            throw new VehHandleRejectException("stage", "知会笔记只在知会站落，卷现在在" + stageName(stage));
        }
        Date moment = at == null ? new Date() : at;
        if (VehHandleRuler.notifyReady(r)) {
            // 知会那几笔已齐（卷仍停在知会站，进站只走 advance 那一处口子）：
            // 同一站再递不另起新行，卷上还是头几遍那几行字
            return attachAndReturn(r, moment);
        }
        String noteText = trimToNull(text);
        if (noteText == null) {
            throw new VehHandleRejectException("text", "知会笔记空着一句，这一笔记不全");
        }
        // 先落这一笔，再看齐不齐：几笔与段同进同退；齐了也不自动挪步——下一站由 advance 那道口进
        addNote(r.getId(), VehHandleRuler.STAGE_NOTIFY, VehHandleRuler.NOTE_NOTIFY, noteText, null, moment);
        r.setNotifyCount((r.getNotifyCount() == null ? 0 : r.getNotifyCount()) + 1);
        vehHandleFlowMapper.updateById(r);
        return attachAndReturn(r, moment);
    }

    // ---------------------------------------------------------------- 回告站（企业报文只当来意）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehHandleReplyReceipt acceptReply(VehHandleReplyMsg msg) {
        VehHandleReplyReceipt rc = new VehHandleReplyReceipt();
        if (msg == null || trimToNull(msg.getBizNo()) == null) {
            rc.setAccepted(false);
            rc.setMark("报文里没带处置单代号，接不住");
            return rc;
        }
        String bizNo = msg.getBizNo().trim();
        TVehHandleFlow r = vehHandleFlowMapper.selectOne(
                new QueryWrapper<TVehHandleFlow>().eq("biz_no", bizNo).eq("del_flag", 0).last("limit 1"));
        rc.setBizNo(bizNo);
        if (r == null) {
            rc.setAccepted(false);
            rc.setMark("查无这张处置单，报文原样退回");
            return rc;
        }
        Date moment = parseWallClock(msg.getAt());
        if (moment == null) {
            moment = new Date();
        }
        int stage = VehHandleRuler.stageOf(r);
        rc.setStage(stage);
        rc.setClaimStage(msg.getClaimStage());
        rc.setClaimMatched(msg.getClaimStage() != null && msg.getClaimStage() == stage);

        if (VehHandleRuler.isTerminal(r)) {
            // 收过口之后再递：不接，卷上一格也不动
            rc.setAccepted(false);
            rc.setNewLine(false);
            rc.setNextOpen(false);
            rc.setMark("这张卷已经收口封尾，回告再接不接");
            return rc;
        }
        if (stage != VehHandleRuler.STAGE_REPLY) {
            // 只在回告站接；报文说已办到后面几站也没用，下一站开着也当没开，绝不叫报文推着走
            rc.setAccepted(false);
            rc.setNewLine(false);
            String block = VehHandleRuler.nextBlockReason(r, moment);
            rc.setNextOpen(block == null);
            rc.setMark("卷现在在" + stageName(stage) + "，回话只在回告站接；报文自报步次只当来意，站次照系统数的算");
            return rc;
        }
        if (VehHandleRuler.replyArrived(r)) {
            // 同一站第二遍报上来：卷上留的还是头一遍那行字，不另起新行，来意也不替头一遍
            rc.setAccepted(true);
            rc.setNewLine(false);
            String block = VehHandleRuler.nextBlockReason(r, moment);
            rc.setNextOpen(block == null);
            rc.setMark(block == null ? "回告头一遍已在卷，第二遍不另起行；钟点已走满，下一站开着"
                    : "回告头一遍已在卷，第二遍不另起行；" + block);
            return rc;
        }

        String noteText = trimToNull(msg.getContent());
        addNote(r.getId(), VehHandleRuler.STAGE_REPLY, VehHandleRuler.NOTE_REPLY,
                noteText == null ? "企业回告（未附话）" : noteText, msg.getClaimStage(), moment);
        // 入库按系统数出来的站记：计数与当站都不睬报文步次，报文步次只挂在按语段的来意栏
        r.setReplyCount(1);
        r.setReplyClaimStage(msg.getClaimStage());
        vehHandleFlowMapper.updateById(r);

        rc.setAccepted(true);
        rc.setNewLine(true);
        String block = VehHandleRuler.nextBlockReason(r, moment);
        rc.setNextOpen(block == null);
        if (Boolean.FALSE.equals(rc.getClaimMatched())) {
            rc.setMark("报文自办到第" + msg.getClaimStage() + "步，系统数在回告站——按系统的算；"
                    + (block == null ? "钟点已走满，下一站开着" : block));
        } else {
            rc.setMark(block == null ? "回告头一遍落卷，钟点已走满，下一站开着"
                    : "回告头一遍落卷，" + block);
        }
        return rc;
    }

    // ---------------------------------------------------------------- 归档站与收口两说

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow submitReview(Long id, String opinion, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (VehHandleRuler.isTerminal(r)) {
            throw new VehHandleRejectException("status", "这张卷已经收口，复查意见落不进去了");
        }
        if (VehHandleRuler.stageOf(r) != VehHandleRuler.STAGE_ARCHIVE) {
            throw new VehHandleRejectException("stage",
                    "复查意见只在归档站落，卷现在在" + stageName(VehHandleRuler.stageOf(r)));
        }
        String text = trimToNull(opinion);
        if (text == null) {
            throw new VehHandleRejectException("reviewOpinion", "入档那一站只看复查意见，意见空着收不了口");
        }
        Date moment = at == null ? new Date() : at;
        r.setReviewOpinion(text);
        vehHandleFlowMapper.updateById(r);
        addNote(r.getId(), VehHandleRuler.STAGE_ARCHIVE, VehHandleRuler.NOTE_REVIEW, text, null, moment);
        closeFlow(r, VehHandleRuler.CLOSE_REVIEW, "复查意见已落，复查入档收口", moment);
        return attachAndReturn(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow closeOutside(Long id, String reason, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (VehHandleRuler.isTerminal(r)) {
            return null;
        }
        Date moment = at == null ? new Date() : at;
        String text = trimToNull(reason);
        closeFlow(r, VehHandleRuler.CLOSE_OUTSIDE,
                "这一类不归运输机构督办，就地收口" + (text == null ? "" : "：" + text), moment);
        return attachAndReturn(r, moment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehHandleFlow closeDeregistered(Long id, String reason, Date at) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (VehHandleRuler.isTerminal(r)) {
            return null;
        }
        TVehEnt ent = findEnt(r.getSiteNo());
        if (ent == null) {
            throw new VehHandleRejectException("siteNo", "卷上没落业户代号或名录查无此户，凭销户收不得这张口");
        }
        if (ent.getStatus() == null || ent.getStatus() != 1) {
            throw new VehHandleRejectException("siteNo", "这户在名录上还在册，凭销户收不得这张口");
        }
        Date moment = at == null ? new Date() : at;
        String text = trimToNull(reason);
        closeFlow(r, VehHandleRuler.CLOSE_DEREGISTERED,
                "所对业户[" + ent.getSiteNo() + "]已销户，就地收口" + (text == null ? "" : "：" + text), moment);
        return attachAndReturn(r, moment);
    }

    /**
     * 收口唯一落法：情形翻封尾、钉收口凭据，站次停在当下那一站不跳。
     * 封尾即只读——此后推/退/改/撤/再接回告全在各口子头上拒掉。收一张在办数减一。
     */
    private void closeFlow(TVehHandleFlow r, String closeKind, String mark, Date at) {
        r.setStatus(VehHandleRuler.STATUS_TERMINAL);
        r.setCloseKind(closeKind);
        r.setLastAction("收口:" + closeKind);
        vehHandleFlowMapper.updateById(r);
        addNote(r.getId(), VehHandleRuler.stageOf(r), VehHandleRuler.NOTE_CLOSE, mark, null, at);
    }

    // ---------------------------------------------------------------- 旧格：改 / 撤

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 收口即只读：修改的位置一律锁死，要改没有第二处口子
        if (VehHandleRuler.isTerminal(r)) {
            return false;
        }
        r.setContent(remark);
        return vehHandleFlowMapper.updateById(r) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        TVehHandleFlow r = vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 收口的单撤不掉、事后挪一挪也不许；只有没在办的才撤得掉
        if (VehHandleRuler.isTerminal(r)) {
            return false;
        }
        vehHandleNoteMapper.delete(new QueryWrapper<TVehHandleNote>().eq("flow_id", id));
        return vehHandleFlowMapper.deleteById(id) > 0;
    }

    // ---------------------------------------------------------------- 杂件

    /** 落一段按语：哪一站、哪一种话、什么话、哪一刻、报文来意（只回告段有） */
    private void addNote(Long flowId, int stage, String kind, String text, Integer claimStage, Date at) {
        TVehHandleNote n = new TVehHandleNote();
        n.setFlowId(flowId);
        n.setStage(stage);
        n.setNoteKind(kind);
        n.setNoteText(text);
        n.setClaimStage(claimStage);
        n.setNoteAt(at);
        n.setDelFlag(0);
        vehHandleNoteMapper.insert(n);
    }

    private TVehHandleFlow attachAndReturn(TVehHandleFlow r, Date at) {
        TVehHandleFlow fresh = vehHandleFlowMapper.selectById(r.getId());
        if (fresh != null) {
            attachRead(fresh, at, true);
            return fresh;
        }
        return r;
    }

    private TVehEnt findEnt(String siteNo) {
        String no = trimToNull(siteNo);
        if (no == null) {
            return null;
        }
        List<TVehEnt> rows = vehEntMapper.selectList(
                new QueryWrapper<TVehEnt>().eq("site_no", no).eq("del_flag", 0).last("limit 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 墙钟字符串解析：与全册同一套时刻读法，不交给 JDBC 绑参 */
    private static Date parseWallClock(String text) {
        String t = trimToNull(text);
        if (t == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FMT_TS);
        sdf.setLenient(false);
        try {
            return sdf.parse(t);
        } catch (ParseException e) {
            return null;
        }
    }

    private static String stageName(int stage) {
        switch (stage) {
            case VehHandleRuler.STAGE_DEFINE: return "定性站";
            case VehHandleRuler.STAGE_NOTIFY: return "知会站";
            case VehHandleRuler.STAGE_REPLY: return "回告站";
            case VehHandleRuler.STAGE_ARCHIVE: return "归档站";
            default: return "第" + stage + "站";
        }
    }
}
