package com.fc.v2.model.auto;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 严重报警处置单对象 t_veh_handle_flow
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_veh_handle_flow")
@ApiModel(value = "TVehHandleFlow", description = "严重报警处置单")
public class TVehHandleFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 严重报警处置单代号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "严重报警处置单代号")
    private String bizNo;

    /** 车辆营运代号（定性四样之一） */
    @TableField("veh_code")
    @ApiModelProperty(value = "车辆营运代号")
    private String vehCode;

    /** 报警属哪一类 speed超速 fatigue疲劳驾驶 */
    @TableField("alarm_kind")
    @ApiModelProperty(value = "报警类别 speed/fatigue")
    private String alarmKind;

    /** 报警发生那一刻（定性四样之一） */
    @TableField("alarm_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "报警发生那一刻")
    private Date alarmAt;

    /** 当刻那个数（车速km/h 或 连续驾驶时长小时，定性四样之一） */
    @TableField("metric_value")
    @ApiModelProperty(value = "当刻那个数")
    private BigDecimal metricValue;

    /** 所对运输业户代号 */
    @TableField("site_no")
    @ApiModelProperty(value = "所对业户代号")
    private String siteNo;

    /** 当前停在哪一站 0定性 1知会 2回告 3归档（系统按已过站口数推） */
    @TableField("stage")
    @ApiModelProperty(value = "当前停在哪一站 0定性 1知会 2回告 3归档")
    private Integer stage;

    /** 处置单情形 0未起 1在办 2已封尾 */
    @TableField("status")
    @ApiModelProperty(value = "处置单情形 0未起 1在办 2已封尾")
    private Integer status;

    /** 知会那一站该齐的笔数 */
    @TableField("notice_count")
    @ApiModelProperty(value = "知会该齐的笔数")
    private Integer noticeCount;

    /** 知会已记上的笔数 */
    @TableField("notice_done")
    @ApiModelProperty(value = "知会已记的笔数")
    private Integer noticeDone;

    /** 回告要守满的钟点（小时） */
    @TableField("reply_due_hours")
    @ApiModelProperty(value = "回告要守满的钟点")
    private Integer replyDueHours;

    /** 知会出站那一刻（回告钟点自这儿起算） */
    @TableField("notice_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "知会出站那一刻")
    private Date noticeAt;

    /** 复查意见（入档站只看这一格落没落） */
    @TableField("review_opinion")
    @ApiModelProperty(value = "复查意见")
    private String reviewOpinion;

    /** 收口名目 archive复查入档 notTransport不归运输机构督办 cancelled业户已销户 */
    @TableField("close_kind")
    @ApiModelProperty(value = "收口名目")
    private String closeKind;

    /** 收口那一刻 */
    @TableField("closed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "收口那一刻")
    private Date closedAt;

    /** 在办口 1没收口；收口置 NULL（同车同类唯一索引只认 1） */
    @TableField("active_slot")
    @ApiModelProperty(value = "在办口 1没收口 收口置NULL")
    private Integer activeSlot;

    /** 当前停着的这一站是第几回到（同站第二遍照旧段，退站重走另攒新段） */
    @TableField("visit_no")
    @ApiModelProperty(value = "当前站第几回到")
    private Integer visitNo;

    /** 报文捎来的步次（只当来意留痕，步次仍照系统数的） */
    @TableField("claimed_stage")
    @ApiModelProperty(value = "报文捎来的步次（只作来意）")
    private Integer claimedStage;

    /** 一段一记（旧式样保留，按语新卷挂 t_veh_handle_note） */
    @TableField("content")
    @ApiModelProperty(value = "一段一记")
    private String content;

    /** 最近一次挪步动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次挪步动作")
    private String lastAction;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

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

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Integer getReplyDueHours() {
        return replyDueHours;
    }

    public void setReplyDueHours(Integer replyDueHours) {
        this.replyDueHours = replyDueHours;
    }

    public Date getNoticeAt() {
        return noticeAt;
    }

    public void setNoticeAt(Date noticeAt) {
        this.noticeAt = noticeAt;
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

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Integer getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(Integer activeSlot) {
        this.activeSlot = activeSlot;
    }

    public Integer getVisitNo() {
        return visitNo;
    }

    public void setVisitNo(Integer visitNo) {
        this.visitNo = visitNo;
    }

    public Integer getClaimedStage() {
        return claimedStage;
    }

    public void setClaimedStage(Integer claimedStage) {
        this.claimedStage = claimedStage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
