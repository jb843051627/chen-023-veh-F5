package com.fc.v2.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.fc.v2.model.auto.TVehHandleFlow;

/**
 * 严重报警处置单——"走到哪一站、下一站开不开"唯一的一把尺。
 *
 * 步次只认一个来处：系统按单子已经过的站数数（stage 一栏），报文里捎来的步次进不了这把尺，
 * 只配在按语段里留个"来意"。册面回读、推进守门、屏上开不开，全来这里问，别处不许另答。
 *
 * 四站：0定级 1知会 2回告 3归档。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class VehHandleRuler {

    /** 环节：定级（头一档） */
    public static final int STAGE_DEFINE = 0;
    /** 环节：知会 */
    public static final int STAGE_NOTIFY = 1;
    /** 环节：回告（守当地定的钟点） */
    public static final int STAGE_REPLY = 2;
    /** 环节：归档（末一档），复查意见落下即封尾 */
    public static final int STAGE_ARCHIVE = 3;

    /** 情形：未起（建了单还没挪过步的存量旧账才许是这一档；新单建单即在办） */
    public static final int STATUS_NOT_STARTED = 0;
    /** 情形：在办（含半道止住的，照样占着同车同类的名额） */
    public static final int STATUS_ACTIVE = 1;
    /** 情形：已封尾（收口即只读，推不动、改不动、撤不掉） */
    public static final int STATUS_TERMINAL = 2;

    /** 收口凭据：复查入档 */
    public static final String CLOSE_REVIEW = "REVIEW";
    /** 收口凭据：不归运输机构督办 */
    public static final String CLOSE_OUTSIDE = "OUTSIDE";
    /** 收口凭据：业户已销户 */
    public static final String CLOSE_DEREGISTERED = "DEREGISTERED";

    /** 量别：车速（当刻那个数按 km/h） */
    public static final String MEASURE_SPEED = "speed";
    /** 量别：连续驾驶时长（当刻那个数按小时） */
    public static final String MEASURE_DURATION = "duration";

    /** 按语话种：定性 */
    public static final String NOTE_OPEN = "OPEN";
    /** 按语话种：知会笔记 */
    public static final String NOTE_NOTIFY = "NOTIFY";
    /** 按语话种：企业回告 */
    public static final String NOTE_REPLY = "REPLY";
    /** 按语话种：复查意见 */
    public static final String NOTE_REVIEW = "REVIEW";
    /** 按语话种：收口 */
    public static final String NOTE_CLOSE = "CLOSE";
    /** 按语话种：挪步按语（进/退） */
    public static final String NOTE_MOVE = "MOVE";

    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";
    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;

    private VehHandleRuler() {
    }

    /** 墙钟时刻字符串：与全册别处同一套时刻读法 */
    public static String ts(Date d) {
        return d == null ? null : new SimpleDateFormat(FMT_TS).format(d);
    }

    /** 系统数出来的当站：空着照头一档认 */
    public static int stageOf(TVehHandleFlow r) {
        return r.getStage() == null ? STAGE_DEFINE : r.getStage();
    }

    /** 收没收口只看情形这一栏 */
    public static boolean isTerminal(TVehHandleFlow r) {
        return r.getStatus() != null && r.getStatus() == STATUS_TERMINAL;
    }

    /** 还在不在办（含停在归档站等复查意见的） */
    public static boolean isActive(TVehHandleFlow r) {
        return !isTerminal(r);
    }

    /** 知会站应齐几笔：当地定的数缺省/小于一按一笔兜底 */
    public static int notifyRequired(TVehHandleFlow r) {
        return r.getNotifyRequired() == null || r.getNotifyRequired() < 1 ? 1 : r.getNotifyRequired();
    }

    /** 知会那几笔全了没有 */
    public static boolean notifyReady(TVehHandleFlow r) {
        int have = r.getNotifyCount() == null ? 0 : r.getNotifyCount();
        return have >= notifyRequired(r);
    }

    /** 回告站要守的钟点走满没有：从知会齐、进回告站那一刻起数当地定的几个钟点 */
    public static boolean replyHoursElapsed(TVehHandleFlow r, Date at) {
        if (at == null || r.getNotifyDoneAt() == null) {
            return false;
        }
        int hours = r.getReplyWaitHours() == null || r.getReplyWaitHours() < 0 ? 0 : r.getReplyWaitHours();
        String deadline = ts(new Date(r.getNotifyDoneAt().getTime() + hours * ONE_HOUR_MS));
        return ts(at).compareTo(deadline) >= 0;
    }

    /** 企业回话递到没有（本趟头一笔才算数，退回知会要清零重攒） */
    public static boolean replyArrived(TVehHandleFlow r) {
        return r.getReplyCount() != null && r.getReplyCount() > 0;
    }

    /** 回告站放不放行：回话得到、钟点也得走满，两样缺一样下一站都当没开 */
    public static boolean replyReady(TVehHandleFlow r, Date at) {
        return replyArrived(r) && replyHoursElapsed(r, at);
    }

    /** 复查意见落没落（入档那一站只看这一格） */
    public static boolean reviewReady(TVehHandleFlow r) {
        return r.getReviewOpinion() != null && !r.getReviewOpinion().trim().isEmpty();
    }

    /**
     * 下一站此刻开不开：唯一的问法。返回 null 为开；不开返回一句缘由，册面回读照原话挂。
     * 已收口的永远不开；定级站只认起单那道门槛（不在这儿拦）；
     * 知会站看笔记齐不齐；回告站回话齐了钟点没走满也不开。
     */
    public static String nextBlockReason(TVehHandleFlow r, Date at) {
        if (isTerminal(r)) {
            return "这张单已经收口，封尾只读";
        }
        int stage = stageOf(r);
        if (stage == STAGE_DEFINE) {
            return null;
        }
        if (stage == STAGE_NOTIFY) {
            if (!notifyReady(r)) {
                return "知会那几笔还没全，还差" + (notifyRequired(r)
                        - (r.getNotifyCount() == null ? 0 : r.getNotifyCount())) + "笔";
            }
            return null;
        }
        if (stage == STAGE_REPLY) {
            if (!replyArrived(r)) {
                return "企业回告还没递到";
            }
            if (!replyHoursElapsed(r, at)) {
                return "回告要守的钟点还没走满，回话递得再齐也进不了下一站";
            }
            return null;
        }
        // 已在归档站：前面没有下一站，落复查意见才收得口
        return "已到归档站，只等复查意见落下收口";
    }

    /** 下一站开不开的 boolean 说法，与 {@link #nextBlockReason} 同一个判法 */
    public static boolean nextOpen(TVehHandleFlow r, Date at) {
        return nextBlockReason(r, at) == null;
    }
}
