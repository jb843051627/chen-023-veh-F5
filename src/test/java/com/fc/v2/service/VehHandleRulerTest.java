package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TVehHandleFlow;

/**
 * 处置单那把尺的纯算口径（不启 Spring、不要桩）。
 * 重点钉：知会几笔齐没齐、回告钟点压线（含那一刻算走满）、下一站开不开的各站说法。
 */
public class VehHandleRulerTest {

    private TVehHandleFlow flow(int stage, int status) {
        TVehHandleFlow r = new TVehHandleFlow();
        r.setStage(stage);
        r.setStatus(status);
        return r;
    }

    @Test
    public void stageDefaultsToDefineAndTerminalIsReadonly() {
        TVehHandleFlow r = flow(0, 0);
        r.setStage(null);
        assertEquals(0, VehHandleRuler.stageOf(r));
        assertFalse(VehHandleRuler.isTerminal(r));

        TVehHandleFlow closed = flow(3, 2);
        assertTrue(VehHandleRuler.isTerminal(closed));
        assertFalse(VehHandleRuler.nextOpen(closed, new Date()));
    }

    @Test
    public void notifyReadyCountsAllRequiredNotesWithOneAsDefault() {
        TVehHandleFlow r = flow(1, 1);
        r.setNotifyRequired(3);
        r.setNotifyCount(2);
        assertFalse(VehHandleRuler.notifyReady(r));
        assertEquals("知会那几笔还没全，还差1笔", VehHandleRuler.nextBlockReason(r, new Date()));

        r.setNotifyCount(3);
        assertTrue(VehHandleRuler.notifyReady(r));
        assertNull(VehHandleRuler.nextBlockReason(r, new Date()));

        // 应齐数缺省/小于一按一笔兜底
        TVehHandleFlow one = flow(1, 1);
        one.setNotifyRequired(null);
        one.setNotifyCount(1);
        assertTrue(VehHandleRuler.notifyReady(one));
    }

    @Test
    public void replyGateWaitsUntilDeadlineInclusive() {
        TVehHandleFlow r = flow(2, 1);
        r.setNotifyCount(1);
        r.setReplyCount(1);
        Date t0 = new Date(1_700_000_000_000L);
        r.setNotifyDoneAt(t0);
        r.setReplyWaitHours(2);
        long hour = 3600_000L;

        // 回话到了但钟点没走满：差一个钟头，下一站当没开
        assertFalse(VehHandleRuler.replyHoursElapsed(r, new Date(t0.getTime() + hour)));
        assertFalse(VehHandleRuler.replyReady(r, new Date(t0.getTime() + hour)));
        assertEquals("回告要守的钟点还没走满，回话递得再齐也进不了下一站",
                VehHandleRuler.nextBlockReason(r, new Date(t0.getTime() + hour)));

        // 压线：钟点正满那一刻算走满（含那一刻）
        assertTrue(VehHandleRuler.replyHoursElapsed(r, new Date(t0.getTime() + 2 * hour)));
        assertTrue(VehHandleRuler.replyReady(r, new Date(t0.getTime() + 2 * hour)));
        assertNull(VehHandleRuler.nextBlockReason(r, new Date(t0.getTime() + 2 * hour)));
    }

    @Test
    public void replyGateAlsoRequiresTheReplyItself() {
        TVehHandleFlow r = flow(2, 1);
        r.setNotifyCount(1);
        r.setReplyCount(0);
        r.setNotifyDoneAt(new Date());
        r.setReplyWaitHours(0);
        // 钟点早走满，回话没到照样不开
        assertEquals("企业回告还没递到", VehHandleRuler.nextBlockReason(r, new Date()));
    }

    @Test
    public void archiveStageWaitsOnlyForReviewOpinion() {
        TVehHandleFlow r = flow(3, 1);
        assertFalse(VehHandleRuler.nextOpen(r, new Date()));
        assertFalse(VehHandleRuler.reviewReady(r));
        r.setReviewOpinion("  ");
        assertFalse(VehHandleRuler.reviewReady(r));
        r.setReviewOpinion("复查无异常，入档");
        assertTrue(VehHandleRuler.reviewReady(r));
    }
}
