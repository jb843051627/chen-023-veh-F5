package com.fc.v2.service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 严重报警处置单——走到哪一站、下一站开不开的唯一一把尺。
 *
 * 今年立住的三件事都归这一处说法：
 * 1. 步次只由系统按已经过的站口数推出（stage 本身就是那个数），报文里捎来的步次
 *    （claimedStage）只当一句来意：与系统数的不是一回事时，入库、回读都听系统数的；
 * 2. 进只许进紧邻一站（跳站那一笔不作数），退一次只退一站，头一站退无可退；
 * 3. 情形与步次同源：未起只留给建了单没动过的，挪过（含退回重办）即在办，收口即封尾。
 *
 * 各站开门的门槛（定性四样齐、知会笔数齐、回告钟点走满、复查意见落格）也只在此处定，
 * 服务层与回读视图共用同一套说法，不许第二处作答。
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class VehHandleRuler {

    /** 站：定性（头一站） */
    public static final int STAGE_NAMING = 0;
    /** 站：知会企业 */
    public static final int STAGE_NOTICE = 1;
    /** 站：等企业回告 */
    public static final int STAGE_REPLY = 2;
    /** 站：复查入档（末一站） */
    public static final int STAGE_ARCHIVE = 3;

    /** 情形：未起（建了单还没动过） */
    public static final int STATUS_NOT_STARTED = 0;
    /** 情形：在办（挪过就算，含退回头一站重办） */
    public static final int STATUS_ACTIVE = 1;
    /** 情形：已封尾（收过口即只读） */
    public static final int STATUS_TERMINAL = 2;

    /** 收口名目：复查入档 */
    public static final String CLOSE_ARCHIVE = "archive";
    /** 收口名目：不归运输机构督办 */
    public static final String CLOSE_NOT_TRANSPORT = "notTransport";
    /** 收口名目：业户已销户 */
    public static final String CLOSE_CANCELLED = "cancelled";

    /** 报警类别：超速（当刻那个数是车速） */
    public static final String ALARM_SPEED = "speed";
    /** 报警类别：疲劳驾驶（当刻那个数是连续驾驶时长） */
    public static final String ALARM_FATIGUE = "fatigue";

    /** 系统事件段来路号：进/退/收口由系统挂的段，不与报文段并线，也不计入知会笔数 */
    public static final String SOURCE_MOVE = "system";
    /** 报文/值班岗没给来路号时的默认来路 */
    public static final String SOURCE_MANUAL = "manual";

    private static final String FMT_TS = "yyyy-MM-dd HH:mm:ss";

    private VehHandleRuler() {
    }

    /** 墙钟时刻字符串：落按语、回读、钟点比较同走这一套，避开 JDBC serverTimezone 错位 */
    public static String ts(Date d) {
        return d == null ? null : new SimpleDateFormat(FMT_TS).format(d);
    }

    /** 站名只此一处给：0定性 1知会 2回告 3归档 */
    public static String stageName(Integer stage) {
        if (stage == null) {
            return "未定站";
        }
        switch (stage) {
            case STAGE_NAMING:
                return "定性";
            case STAGE_NOTICE:
                return "知会";
            case STAGE_REPLY:
                return "回告";
            case STAGE_ARCHIVE:
                return "归档";
            default:
                return "越界站(" + stage + ")";
        }
    }

    /** 情形名只此一处给 */
    public static String statusName(Integer status) {
        if (status == null) {
            return "未起";
        }
        switch (status) {
            case STATUS_NOT_STARTED:
                return "未起";
            case STATUS_ACTIVE:
                return "在办";
            case STATUS_TERMINAL:
                return "已封尾";
            default:
                return "不明(" + status + ")";
        }
    }

    /** 收口名目说法 */
    public static String closeKindName(String closeKind) {
        if (closeKind == null) {
            return null;
        }
        if (CLOSE_ARCHIVE.equals(closeKind)) {
            return "复查入档";
        }
        if (CLOSE_NOT_TRANSPORT.equals(closeKind)) {
            return "不归运输机构督办";
        }
        if (CLOSE_CANCELLED.equals(closeKind)) {
            return "业户已销户";
        }
        return closeKind;
    }

    /** 报警类别认不认（只认车速/连续驾驶时长这两类口径） */
    public static boolean validAlarmKind(String kind) {
        return ALARM_SPEED.equals(kind) || ALARM_FATIGUE.equals(kind);
    }

    /** 特殊收口名目认不认（非运输机构督办、业户销户这两说才算收口） */
    public static boolean validSpecialClose(String kind) {
        return CLOSE_NOT_TRANSPORT.equals(kind) || CLOSE_CANCELLED.equals(kind);
    }

    /**
     * 来意同系统数出来的是不是一站。报文说"已办到第 claimed 步"只当来意：
     * 进只认紧邻的下一站（claimed == 当前+1）；不是一回事，这笔跳站不作数。
     */
    public static boolean claimedMatchesNext(int currentStage, Integer claimedStage) {
        return claimedStage != null && claimedStage == currentStage + 1;
    }

    /** 挪到 newStage 后情形该怎么落：归档即封尾，其余在办（未起只留给从没挪过的） */
    public static int statusAfterMove(int newStage) {
        return newStage >= STAGE_ARCHIVE ? STATUS_TERMINAL : STATUS_ACTIVE;
    }

    /**
     * 回告那一站的钟点走满没有：知会出站那一刻起，守满当地定的那几个钟头。
     * 压线算走满（含满点那一刻）；钟点没定、知会没出站、问话时刻缺省，都算没走满——
     * 钟点没走满，企业回话递得再齐也进不去下一站。
     */
    public static boolean replyHoursSatisfied(Date noticeAt, Integer dueHours, Date at) {
        if (noticeAt == null || dueHours == null || dueHours < 0 || at == null) {
            return false;
        }
        long elapsed = at.getTime() - noticeAt.getTime();
        return elapsed >= dueHours * 3600_000L;
    }
}
