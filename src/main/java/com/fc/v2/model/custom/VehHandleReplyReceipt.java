package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 企业回告报文收口回执。
 *
 * 报的全是系统数出来的那一份：单子当站、下一站开不开（钟点走没走满）、这一笔有没有另起新行。
 * 报文自报步次只回挂"来意"，与系统站次不一致时入库与回读都不听它的。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "VehHandleReplyReceipt", description = "企业回告收口回执")
public class VehHandleReplyReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 处置单代号 */
    @ApiModelProperty(value = "处置单代号")
    private String bizNo;

    /** 接没接（收口后的单、不在回告站的一律不接） */
    @ApiModelProperty(value = "接没接")
    private Boolean accepted;

    /** 系统数出来的当站 0..3 */
    @ApiModelProperty(value = "系统数出来的当站")
    private Integer stage;

    /** 报文自报步次（只当来意） */
    @ApiModelProperty(value = "报文自报步次")
    private Integer claimStage;

    /** 来意与系统站次一不一致 */
    @ApiModelProperty(value = "来意与系统站次是否一致")
    private Boolean claimMatched;

    /** 这一笔落没落库（同站第二遍卷上还是头一行，不留新行） */
    @ApiModelProperty(value = "这一笔落没落库")
    private Boolean newLine;

    /** 下一站此刻开不开（钟点没走满就当没开） */
    @ApiModelProperty(value = "下一站此刻开不开")
    private Boolean nextOpen;

    /** 不开的缘由/收口批注 */
    @ApiModelProperty(value = "不开的缘由/收口批注")
    private String mark;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getClaimStage() {
        return claimStage;
    }

    public void setClaimStage(Integer claimStage) {
        this.claimStage = claimStage;
    }

    public Boolean getClaimMatched() {
        return claimMatched;
    }

    public void setClaimMatched(Boolean claimMatched) {
        this.claimMatched = claimMatched;
    }

    public Boolean getNewLine() {
        return newLine;
    }

    public void setNewLine(Boolean newLine) {
        this.newLine = newLine;
    }

    public Boolean getNextOpen() {
        return nextOpen;
    }

    public void setNextOpen(Boolean nextOpen) {
        this.nextOpen = nextOpen;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
