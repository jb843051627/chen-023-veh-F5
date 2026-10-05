package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 处置单册面与回读同一份的答案。
 *
 * 四问只认这一处作答：走到哪一站（系统按已过站口数推，不看来意）、下一站开不开
 * （各站门槛同一把尺）、收过口再递接不接（封尾只读）、同一站第二遍另不另起新行
 * （同来路并头一行，退站重走才另攒）。屏上与回读都从这一份取，不许第二处给说法。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "VehHandleFlowView", description = "严重报警处置单回读（册面与报文同一份）")
public class VehHandleFlowView implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "处置单代号")
    private String bizNo;

    @ApiModelProperty(value = "车辆营运代号")
    private String vehCode;

    @ApiModelProperty(value = "报警类别 speed/fatigue")
    private String alarmKind;

    @ApiModelProperty(value = "报警发生那一刻")
    private String alarmAt;

    @ApiModelProperty(value = "当刻那个数")
    private BigDecimal metricValue;

    @ApiModelProperty(value = "所对业户代号")
    private String siteNo;

    /** 当前站：系统数出来的，报文步次不作数 */
    @ApiModelProperty(value = "当前停在哪一站（系统数的）")
    private Integer stage;

    @ApiModelProperty(value = "当前站名")
    private String stageName;

    @ApiModelProperty(value = "情形 0未起 1在办 2已封尾")
    private Integer status;

    @ApiModelProperty(value = "情形名")
    private String statusName;

    @ApiModelProperty(value = "报文捎来的步次（只作来意留痕）")
    private Integer claimedStage;

    @ApiModelProperty(value = "来意与系统数出来的不是一回事")
    private Boolean claimedDiffers;

    @ApiModelProperty(value = "当前站第几回到")
    private Integer visitNo;

    @ApiModelProperty(value = "下一站（末站后为空）")
    private Integer nextStage;

    @ApiModelProperty(value = "下一站名")
    private String nextStageName;

    @ApiModelProperty(value = "下一站开不开（唯一答案）")
    private Boolean nextOpen;

    @ApiModelProperty(value = "不开的缘由（缺哪样/钟点没走满/已封尾…）")
    private String nextReason;

    @ApiModelProperty(value = "知会该齐的笔数")
    private Integer noticeCount;

    @ApiModelProperty(value = "知会已记的笔数")
    private Integer noticeDone;

    @ApiModelProperty(value = "回告钟点走满没有（问话那一刻量）")
    private Boolean replyHoursMet;

    @ApiModelProperty(value = "复查意见落没落")
    private Boolean reviewOpinionPresent;

    @ApiModelProperty(value = "收没收口")
    private Boolean closed;

    @ApiModelProperty(value = "收口名目")
    private String closeKind;

    @ApiModelProperty(value = "收口名目说法")
    private String closeKindName;

    @ApiModelProperty(value = "收口那一刻")
    private String closedAt;

    @ApiModelProperty(value = "卷上按语（按段序，从定性往后排）")
    private List<NoteView> notes = new ArrayList<NoteView>();

    /** 一段按语的回读 */
    @ApiModel(value = "VehHandleFlowView.NoteView")
    public static class NoteView implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "段序")
        private Integer segOrdinal;

        @ApiModelProperty(value = "落在哪一站")
        private Integer stage;

        @ApiModelProperty(value = "站名")
        private String stageName;

        @ApiModelProperty(value = "这一站的第几回")
        private Integer visitNo;

        @ApiModelProperty(value = "来路号")
        private String sourceNo;

        @ApiModelProperty(value = "写下的话")
        private String noteText;

        @ApiModelProperty(value = "落在哪一刻")
        private String writtenAt;

        public Integer getSegOrdinal() {
            return segOrdinal;
        }

        public void setSegOrdinal(Integer segOrdinal) {
            this.segOrdinal = segOrdinal;
        }

        public Integer getStage() {
            return stage;
        }

        public void setStage(Integer stage) {
            this.stage = stage;
        }

        public String getStageName() {
            return stageName;
        }

        public void setStageName(String stageName) {
            this.stageName = stageName;
        }

        public Integer getVisitNo() {
            return visitNo;
        }

        public void setVisitNo(Integer visitNo) {
            this.visitNo = visitNo;
        }

        public String getSourceNo() {
            return sourceNo;
        }

        public void setSourceNo(String sourceNo) {
            this.sourceNo = sourceNo;
        }

        public String getNoteText() {
            return noteText;
        }

        public void setNoteText(String noteText) {
            this.noteText = noteText;
        }

        public String getWrittenAt() {
            return writtenAt;
        }

        public void setWrittenAt(String writtenAt) {
            this.writtenAt = writtenAt;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
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

    public String getAlarmAt() {
        return alarmAt;
    }

    public void setAlarmAt(String alarmAt) {
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

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getClaimedStage() {
        return claimedStage;
    }

    public void setClaimedStage(Integer claimedStage) {
        this.claimedStage = claimedStage;
    }

    public Boolean getClaimedDiffers() {
        return claimedDiffers;
    }

    public void setClaimedDiffers(Boolean claimedDiffers) {
        this.claimedDiffers = claimedDiffers;
    }

    public Integer getVisitNo() {
        return visitNo;
    }

    public void setVisitNo(Integer visitNo) {
        this.visitNo = visitNo;
    }

    public Integer getNextStage() {
        return nextStage;
    }

    public void setNextStage(Integer nextStage) {
        this.nextStage = nextStage;
    }

    public String getNextStageName() {
        return nextStageName;
    }

    public void setNextStageName(String nextStageName) {
        this.nextStageName = nextStageName;
    }

    public Boolean getNextOpen() {
        return nextOpen;
    }

    public void setNextOpen(Boolean nextOpen) {
        this.nextOpen = nextOpen;
    }

    public String getNextReason() {
        return nextReason;
    }

    public void setNextReason(String nextReason) {
        this.nextReason = nextReason;
    }

    public Integer getNoticeCount() {
        return noticeCount;
    }

    public void setNoticeCount(Integer noticeCount) {
        this.noticeCount = noticeCount;
    }

    public Integer getNoticeDone() {
        return noticeDone;
    }

    public void setNoticeDone(Integer noticeDone) {
        this.noticeDone = noticeDone;
    }

    public Boolean getReplyHoursMet() {
        return replyHoursMet;
    }

    public void setReplyHoursMet(Boolean replyHoursMet) {
        this.replyHoursMet = replyHoursMet;
    }

    public Boolean getReviewOpinionPresent() {
        return reviewOpinionPresent;
    }

    public void setReviewOpinionPresent(Boolean reviewOpinionPresent) {
        this.reviewOpinionPresent = reviewOpinionPresent;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public String getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(String closeKind) {
        this.closeKind = closeKind;
    }

    public String getCloseKindName() {
        return closeKindName;
    }

    public void setCloseKindName(String closeKindName) {
        this.closeKindName = closeKindName;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public List<NoteView> getNotes() {
        return notes;
    }

    public void setNotes(List<NoteView> notes) {
        this.notes = notes;
    }
}
