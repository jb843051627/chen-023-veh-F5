package com.fc.v2.mapper.custom;

import java.util.List;

import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.model.custom.VehHandleQuery;

/**
 * 严重报警处置单台账屏查数据层。
 * 屏上显的在办卷数与一页一页点出来的卷出自同一条 SELECT、同一回点算（窗口计数），不许两本账。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface TVehHandleFlowBoardMapper {

    /** 台账开屏：每页名单上自带本查询的全册卷数、在办卷数两个窗口计数。 */
    List<TVehHandleFlow> selectBoardPage(VehHandleQuery query);
}
