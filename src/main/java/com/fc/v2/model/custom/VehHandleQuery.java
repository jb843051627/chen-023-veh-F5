package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 严重报警处置单台账屏查条件。
 * 找卷给三个头：处置单代号、车辆营运代号、报警类别，都是前缀匹配；情形/收口凭据可整格对。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "VehHandleQuery", description = "处置单台账屏查条件")
public class VehHandleQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 处置单代号（前缀匹配） */
    @ApiModelProperty(value = "处置单代号")
    private String bizNo;

    /** 车辆营运代号（前缀匹配） */
    @ApiModelProperty(value = "车辆营运代号")
    private String vehCode;

    /** 报警类别（前缀匹配） */
    @ApiModelProperty(value = "报警类别")
    private String alarmKind;

    /** 情形 0未起 1在办 2已封尾（不给则全摆） */
    @ApiModelProperty(value = "情形 0未起 1在办 2已封尾")
    private Integer status;

    /** 收口凭据 REVIEW/OUTSIDE/DEREGISTERED */
    @ApiModelProperty(value = "收口凭据")
    private String closeKind;

    /** 页码（从 1 起） */
    @ApiModelProperty(value = "页码")
    private Integer page;

    /** 每页几条 */
    @ApiModelProperty(value = "每页几条")
    private Integer limit;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(String closeKind) {
        this.closeKind = closeKind;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
