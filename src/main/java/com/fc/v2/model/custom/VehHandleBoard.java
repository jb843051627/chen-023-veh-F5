package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.fc.v2.model.auto.TVehHandleFlow;

/**
 * 值班屏一板：屏头"没收口几张"与名下各卷是同一条 SQL、同一回点算的产物。
 * 在办数是全册的数（起一张加一、收一张减一），不随筛选借行撑数。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class VehHandleBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本屏卷数（筛选后，一张一张点出来的） */
    private long totalCount;

    /** 全册没收口的卷数（屏上显的那个数，与回库点的同一次算） */
    private int activeCount;

    /** 名下各卷（当站、下一站开不开都照唯一一把尺回挂） */
    private List<TVehHandleFlow> flows = Collections.emptyList();

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public List<TVehHandleFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<TVehHandleFlow> flows) {
        this.flows = flows;
    }
}
