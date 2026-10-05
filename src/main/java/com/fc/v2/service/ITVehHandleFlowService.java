package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.custom.VehHandleFlowForm;
import com.fc.v2.model.custom.VehHandleFlowView;

/**
 * 严重报警处置单 Service（四站状态机，唯一口子）。
 *
 * 四问只认这一处给答案：单子走到哪一站（系统按已过站口数推，报文步次只当来意）、
 * 下一站开不开（各站门槛同一把尺）、收过口再递接不接（封尾只读、拒推拒改拒撤）、
 * 同一站第二遍另不另起新行（同来路并头一行，退站重走才另攒）。
 * 控制器只递话，不算一个数、不定一站次。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITVehHandleFlowService {

    /** 按主键查单据 */
    TVehHandleFlow selectTVehHandleFlowById(Long id);

    /** 台账查询 */
    List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper);

    /**
     * 立单：定性四样齐才起得来（车辆营运代号、报警那一刻、当刻那个数、报警类别）；
     * 同一辆车同一类报警同时只容一张没收口的卷，头一张仍在办/半道止住，第二张起不来。
     * @param at 问话那一刻（墙钟，控制器解）
     * @return 起好的卷；四样缺/类别不认/已有在办卷，返回 null
     */
    TVehHandleFlow open(VehHandleFlowForm form, Date at);

    /**
     * 报文/值班岗敲当前站的门：递这一站的按语。
     * 同一来路第二遍报上来只改头一遍那行字，不另起新行；同秒两个来路各记各的。
     * 步次听系统数的，报文 claimedStage 与系统数的不一致只留来意、不挪步。
     * @param at 落按语那一刻
     * @return 卷的回读（同一份答案）；单不存在/已封尾返回 null
     */
    VehHandleFlowView knock(Long id, VehHandleFlowForm form, Date at);

    /**
     * 进一站：只许进紧邻的那一站，跳站不作数；门槛不齐/钟点没走满，下一站开着也当没开。
     * 挪步缘由由系统挂到该站事件段（system 来路，不与报文段并线、不计知会笔数）。
     * @param claimedStage 报文来意步次（与系统数的不一致按跳站拒），值班岗手记给 null
     * @return 回读；被拒返回 null
     */
    VehHandleFlowView advance(Long id, Integer claimedStage, String note, Date at);

    /**
     * 退一站：一次只退一站；被退那站先前的按语压到卷末，重走这一站时从这一站重新攒；
     * 已停在头一站的不再往外退。
     * @return 回读；被拒返回 null
     */
    VehHandleFlowView rollback(Long id, String note, Date at);

    /**
     * 特殊收口：不归运输机构督办、业户已销户（销户照业户总名录核验）这两说也算收口。
     * 收口即封尾：从此只读、修改位置锁死、整张撤不掉、挪一挪也不许。
     * @return 回读；名目不认/业户在册/已封尾返回 null
     */
    VehHandleFlowView closeSpecial(Long id, String closeKind, String note, Date at);

    /** 复查入档：入档站只看复查意见落没落；落了即收口封尾。 */
    VehHandleFlowView archive(Long id, String reviewOpinion, String note, Date at);

    /** 回读：册面与回读两边同一张单子同一份答案（四问只认这一处）。 */
    VehHandleFlowView getView(Long id, Date at);

    /** 屏上那个数与回库一站一站站点出来的数本是同一次算的：回库现点在办卷张数。 */
    int countOpen();

    /** 改内容：收过口的位置一律锁死，被拒返回 false */
    boolean updateContent(Long id, String remark);

    /** 撤单：收过口的整张撤不掉，被拒返回 false */
    boolean remove(Long id);
}
