package com.fc.v2.model.auto;

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
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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

    /** 严重报警处置单代号（系统排号，卷上自带的不认） */
    @TableField("biz_no")
    @ApiModelProperty(value = "严重报警处置单代号")
    private String bizNo;

    /** 当前步次 0..3（定级/知会/回告/归档），只由系统数已经过的站数得出 */
    @TableField("stage")
    @ApiModelProperty(value = "当前步次 0..3（定级/知会/回告/归档）")
    private Integer stage;

    /** 处置单情形 0未起 1在办 2已封尾（与步次同源） */
    @TableField("status")
    @ApiModelProperty(value = "处置单情形 0未起 1在办 2已封尾")
    private Integer status;

    /** 车辆营运代号（定性四样之一） */
    @TableField("veh_code")
    @ApiModelProperty(value = "车辆营运代号（定性四样之一）")
    private String vehCode;

    /** 报警发生那一刻（定性四样之一） */
    @TableField("alarm_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "报警发生那一刻")
    private Date alarmAt;

    /** 当刻那个数量的是车速(speed)还是连续驾驶时长(duration) */
    @TableField("measure_kind")
    @ApiModelProperty(value = "量别 speed车速 duration连续驾驶时长")
    private String measureKind;

    /** 报警当刻那个数（车速 km/h、连续驾驶时长小时，定性四样之一） */
    @TableField("alarm_value")
    @ApiModelProperty(value = "当刻那个数（车速 km/h、连续驾驶时长小时）")
    private BigDecimal alarmValue;

    /** 报警属哪一类（定性四样之一；同车同类只容一张在办卷） */
    @TableField("alarm_kind")
    @ApiModelProperty(value = "报警类别")
    private String alarmKind;

    /** 所知会业户代号（对 t_veh_ent.site_no） */
    @TableField("site_no")
    @ApiModelProperty(value = "所知会业户代号")
    private String siteNo;

    /** 知会站应齐几笔知会笔记（当地定，缺省一笔） */
    @TableField("notify_required")
    @ApiModelProperty(value = "知会站应齐几笔")
    private Integer notifyRequired;

    /** 知会站已落几笔知会笔记 */
    @TableField("notify_count")
    @ApiModelProperty(value = "知会站已落几笔")
    private Integer notifyCount;

    /** 本趟回告站已落几笔回告（同站第二遍不另起行；退回知会清零，重走重攒） */
    @TableField("reply_count")
    @ApiModelProperty(value = "本趟回告站已落几笔回告")
    private Integer replyCount;

    /** 回告站要守的钟点：知会齐后至少等几个钟点（当地定） */
    @TableField("reply_wait_hours")
    @ApiModelProperty(value = "回告等待钟点（小时）")
    private Integer replyWaitHours;

    /** 知会笔记齐、进回告站那一刻（钟点起算点） */
    @TableField("notify_done_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "知会齐那一刻（钟点起算点）")
    private Date notifyDoneAt;

    /** 最后一笔企业回告报文里自报的步次（只当来意留痕，绝不驱动站次） */
    @TableField("reply_claim_stage")
    @ApiModelProperty(value = "回告报文来意步次（驱动不了站次）")
    private Integer replyClaimStage;

    /** 复查意见（入档那一站只看这格落没落） */
    @TableField("review_opinion")
    @ApiModelProperty(value = "复查意见")
    private String reviewOpinion;

    /** 凭什么收的口 REVIEW复查入档/OUTSIDE非运输机构督办/DEREGISTERED业户销户 */
    @TableField("close_kind")
    @ApiModelProperty(value = "收口凭据 REVIEW/OUTSIDE/DEREGISTERED")
    private String closeKind;

    /** 一段一记（旧格留着，式样不动） */
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

    /** 卷上按语段（回读时按时刻序挂出，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "卷上按语段（按时刻序）")
    private List<TVehHandleNote> notes;

    /** 业户全称（join 名录回显，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "业户全称")
    private String siteName;

    /** 下一站此刻开不开（唯一一处数出来，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "下一站此刻开不开")
    private Boolean nextOpen;

    /** 下一站不开的缘由（不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "下一站不开的缘由")
    private String nextBlockReason;

    /** 全册卷数（开屏同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全册卷数")
    private Integer boardTotal;

    /** 全册在办卷数（开屏同回点算，不落本单表） */
    @TableField(exist = false)
    @ApiModelProperty(value = "全册在办卷数")
    private Integer boardActiveCount;

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

    public String getVehCode() {
        return vehCode;
    }

    public void setVehCode(String vehCode) {
        this.vehCode = vehCode;
    }

    public Date getAlarmAt() {
        return alarmAt;
    }

    public void setAlarmAt(Date alarmAt) {
        this.alarmAt = alarmAt;
    }

    public String getMeasureKind() {
        return measureKind;
    }

    public void setMeasureKind(String measureKind) {
        this.measureKind = measureKind;
    }

    public BigDecimal getAlarmValue() {
        return alarmValue;
    }

    public void setAlarmValue(BigDecimal alarmValue) {
        this.alarmValue = alarmValue;
    }

    public String getAlarmKind() {
        return alarmKind;
    }

    public void setAlarmKind(String alarmKind) {
        this.alarmKind = alarmKind;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getNotifyRequired() {
        return notifyRequired;
    }

    public void setNotifyRequired(Integer notifyRequired) {
        this.notifyRequired = notifyRequired;
    }

    public Integer getNotifyCount() {
        return notifyCount;
    }

    public void setNotifyCount(Integer notifyCount) {
        this.notifyCount = notifyCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Integer getReplyWaitHours() {
        return replyWaitHours;
    }

    public void setReplyWaitHours(Integer replyWaitHours) {
        this.replyWaitHours = replyWaitHours;
    }

    public Date getNotifyDoneAt() {
        return notifyDoneAt;
    }

    public void setNotifyDoneAt(Date notifyDoneAt) {
        this.notifyDoneAt = notifyDoneAt;
    }

    public Integer getReplyClaimStage() {
        return replyClaimStage;
    }

    public void setReplyClaimStage(Integer replyClaimStage) {
        this.replyClaimStage = replyClaimStage;
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

    public List<TVehHandleNote> getNotes() {
        return notes;
    }

    public void setNotes(List<TVehHandleNote> notes) {
        this.notes = notes;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Boolean getNextOpen() {
        return nextOpen;
    }

    public void setNextOpen(Boolean nextOpen) {
        this.nextOpen = nextOpen;
    }

    public String getNextBlockReason() {
        return nextBlockReason;
    }

    public void setNextBlockReason(String nextBlockReason) {
        this.nextBlockReason = nextBlockReason;
    }

    public Integer getBoardTotal() {
        return boardTotal;
    }

    public void setBoardTotal(Integer boardTotal) {
        this.boardTotal = boardTotal;
    }

    public Integer getBoardActiveCount() {
        return boardActiveCount;
    }

    public void setBoardActiveCount(Integer boardActiveCount) {
        this.boardActiveCount = boardActiveCount;
    }
}
