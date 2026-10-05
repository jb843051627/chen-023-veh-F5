package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 严重报警处置单立单递来的话。
 *
 * 定性这一站要四样齐：车辆营运代号、报警发生那一刻、当刻那个数（车速还是连续驾驶时长）、
 * 报警属哪一类；缺一样起不来。处置单代号由系统排，卷上自带号递来不认。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "VehHandleOpenForm", description = "严重报警处置单立单")
public class VehHandleOpenForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 车辆营运代号（必给） */
    @ApiModelProperty(value = "车辆营运代号")
    private String vehCode;

    /** 报警发生那一刻（必给，墙钟 yyyy-MM-dd HH:mm:ss） */
    @ApiModelProperty(value = "报警发生那一刻 yyyy-MM-dd HH:mm:ss")
    private String alarmAt;

    /** 当刻那个数量的是车速(speed)还是连续驾驶时长(duration)（必给） */
    @ApiModelProperty(value = "量别 speed车速 duration连续驾驶时长")
    private String measureKind;

    /** 当刻那个数：车速 km/h、连续驾驶时长小时（必给） */
    @ApiModelProperty(value = "当刻那个数（车速 km/h、连续驾驶时长小时）")
    private BigDecimal alarmValue;

    /** 报警属哪一类（必给；同车同类只容一张在办卷） */
    @ApiModelProperty(value = "报警类别")
    private String alarmKind;

    /** 所知会业户代号（可在知会站之前先落；销户收口照它查名录） */
    @ApiModelProperty(value = "所知会业户代号")
    private String siteNo;

    /** 知会站应齐几笔（当地定，缺省一笔） */
    @ApiModelProperty(value = "知会站应齐几笔，缺省一笔")
    private Integer notifyRequired;

    /** 回告等待钟点：知会齐后至少几个钟点（当地定，缺省不候） */
    @ApiModelProperty(value = "回告等待钟点（小时）")
    private Integer replyWaitHours;

    /** 定性按语（可空） */
    @ApiModelProperty(value = "定性按语")
    private String content;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    public String getVehCode() {
        return vehCode;
    }

    public void setVehCode(String vehCode) {
        this.vehCode = vehCode;
    }

    public String getAlarmAt() {
        return alarmAt;
    }

    public void setAlarmAt(String alarmAt) {
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

    public Integer getReplyWaitHours() {
        return replyWaitHours;
    }

    public void setReplyWaitHours(Integer replyWaitHours) {
        this.replyWaitHours = replyWaitHours;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
