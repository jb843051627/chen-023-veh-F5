package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * 严重报警处置单来客表单：立单与报文敲门同走这一个门。
 *
 * 报文捎来的步次（claimedStage）只当一句来意，不决定单子走到哪——
 * 步次永远由系统按已经过的站口数落；与系统数的不一致，入库、回读都听系统的。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class VehHandleFlowForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 敲门时给（已有单的主键）；立单不给 */
    private Long id;

    /** 车辆营运代号（定性四样之一） */
    private String vehCode;

    /** 报警属哪一类 speed超速 fatigue疲劳驾驶（定性四样之一，兼卷的去重键） */
    private String alarmKind;

    /** 报警发生那一刻（定性四样之一，墙钟 yyyy-MM-dd HH:mm:ss） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmAt;

    /** 当刻那个数：车速 km/h 或连续驾驶时长 小时（定性四样之一） */
    private BigDecimal metricValue;

    /** 所对运输业户代号（销户那两说照名录核验） */
    private String siteNo;

    /** 知会那一站该齐的笔数（立单时定门槛） */
    private Integer noticeCount;

    /** 回告要守满的钟点（当地定的几个钟头，立单时定） */
    private Integer replyDueHours;

    /** 报文捎来的步次（只当来意：进只认紧邻的下一站） */
    private Integer claimedStage;

    /** 这一站写下的话（卷上一段） */
    private String note;

    /** 来路号：同一来路第二遍并头一行，同秒两个来路各起一行；空着按值班岗手记 */
    private String sourceNo;

    /** 复查意见（入档站只看这一格落没落） */
    private String reviewOpinion;

    /** 特殊收口名目 notTransport/cancelled（走 closeSpecial 那道门时给） */
    private String closeKind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehCode() {
        return vehCode;
    }

    public void setVehCode(String vehCode) {
        this.vehCode = vehCode;
    }

    public String getAlarmKind() {
        return alarmKind;
    }

    public void setAlarmKind(String alarmKind) {
        this.alarmKind = alarmKind;
    }

    public Date getAlarmAt() {
        return alarmAt;
    }

    public void setAlarmAt(Date alarmAt) {
        this.alarmAt = alarmAt;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getNoticeCount() {
        return noticeCount;
    }

    public void setNoticeCount(Integer noticeCount) {
        this.noticeCount = noticeCount;
    }

    public Integer getReplyDueHours() {
        return replyDueHours;
    }

    public void setReplyDueHours(Integer replyDueHours) {
        this.replyDueHours = replyDueHours;
    }

    public Integer getClaimedStage() {
        return claimedStage;
    }

    public void setClaimedStage(Integer claimedStage) {
        this.claimedStage = claimedStage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSourceNo() {
        return sourceNo;
    }

    public void setSourceNo(String sourceNo) {
        this.sourceNo = sourceNo;
    }

    /** 来路号空着按值班岗手记（manual） */
    public String sourceNoOrManual() {
        return sourceNo == null || sourceNo.trim().isEmpty() ? "manual" : sourceNo.trim();
    }

    public String getReviewOpinion() {
        return reviewOpinion;
    }

    public void setReviewOpinion(String reviewOpinion) {
        this.reviewOpinion = reviewOpinion;
    }

    public String getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(String closeKind) {
        this.closeKind = closeKind;
    }
}
