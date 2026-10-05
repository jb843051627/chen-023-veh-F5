package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.custom.VehHandleBoard;
import com.fc.v2.model.custom.VehHandleOpenForm;
import com.fc.v2.model.custom.VehHandleQuery;
import com.fc.v2.model.custom.VehHandleReplyMsg;
import com.fc.v2.model.custom.VehHandleReplyReceipt;

/**
 * 严重报警处置单 Service接口（一张处置单从哪儿起、到哪儿止，只由这一处作答）。
 *
 * 旧的挪步口子 {@link #advance(Long, String)} / {@link #rollback(Long, String)} 模样不改：
 * 进一个号、还一张单。日后要换一种走法，另起一套接，不回头动它。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITVehHandleFlowService {

    /** 按主键查询单据（回读：卷上按语段按时刻序挂齐，下一站开不开照唯一一把尺回挂） */
    TVehHandleFlow selectTVehHandleFlowById(Long id);

    /** 列表查询（流转台账旧口子） */
    List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper);

    /** 值班屏台账：名单与在办卷数同一条 SQL 同一回点算 */
    VehHandleBoard openBoard(VehHandleQuery query);

    /** 立单（定性站四样齐才起；同车同类有在办卷的第二张起不来） */
    TVehHandleFlow openFlow(VehHandleOpenForm form);

    /** 推进一档（旧模样）：只许进紧邻站，下一站没开当没开，收口的单推不动；被拒返回 null */
    TVehHandleFlow advance(Long id, String remark);

    /** 回退一档（旧模样）：一次只退一站，被退站的攒头清零重攒，头一档退无可退；被拒返回 null */
    TVehHandleFlow rollback(Long id, String remark);

    /** 知会站落一笔知会笔记；末笔齐了当即进回告站并起算钟点；已齐不另起行 */
    TVehHandleFlow addNotifyNote(Long id, String text, Date at);

    /** 接企业回告报文：报文步次只当来意，入库与回读都按系统数出来的站，同站第二遍不另起新行 */
    VehHandleReplyReceipt acceptReply(VehHandleReplyMsg msg);

    /** 归档站落复查意见：意见落下即凭复查入档收口（封尾只读） */
    TVehHandleFlow submitReview(Long id, String opinion, Date at);

    /** 收口两说之一：这一类不归运输机构督办，任何在办站都收得口 */
    TVehHandleFlow closeOutside(Long id, String reason, Date at);

    /** 收口两说之一：所对业户已销户（名录上确为销户才收得） */
    TVehHandleFlow closeDeregistered(Long id, String reason, Date at);

    /** 修改内容：收口后只读不得再改，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 删除单据：收口的单撤不掉，被拒返回 false */
    boolean remove(Long id);
}
