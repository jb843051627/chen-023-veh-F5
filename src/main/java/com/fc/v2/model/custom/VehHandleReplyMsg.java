package com.fc.v2.model.custom;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 企业回告报文（只当来意看待）。
 *
 * 报文里写着"已办到第几步"（claimStage），那是企业自己的说法：入库按系统数出来的站记，
 * 回读也照系统数出来的回；它跟系统站次不一致时，谁也不替谁改，报文步次只留在按语段里备查。
 * 这一笔真正干的事：在回告站（系统数出来的）落头一回告，同站第二遍不另起新行。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "VehHandleReplyMsg", description = "企业回告报文")
public class VehHandleReplyMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 处置单代号（卷上认的是系统排的 biz_no） */
    @ApiModelProperty(value = "处置单代号")
    private String bizNo;

    /** 报文里自报的已办到第几步（只当来意，驱动不了系统站次） */
    @ApiModelProperty(value = "报文自报步次（只当来意）")
    private Integer claimStage;

    /** 回告的话 */
    @ApiModelProperty(value = "回告的话")
    private String content;

    /** 报文到的那一刻（墙钟 yyyy-MM-dd HH:mm:ss；缺省按问话当下） */
    @ApiModelProperty(value = "报文到的那一刻 yyyy-MM-dd HH:mm:ss")
    private String at;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getClaimStage() {
        return claimStage;
    }

    public void setClaimStage(Integer claimStage) {
        this.claimStage = claimStage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }
}
