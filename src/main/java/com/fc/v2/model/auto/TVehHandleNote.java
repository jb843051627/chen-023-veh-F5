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
import java.util.Date;

/**
 * 严重报警处置单按语段对象 t_veh_handle_note
 *
 * 卷上一段一段的按语：哪一站写了什么话、落在哪一刻。
 * 退回站先前写的段不抹（压到末位重攒），同一秒钟两条来路各记各的行，不许合成一条。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_veh_handle_note")
@ApiModel(value = "TVehHandleNote", description = "严重报警处置单按语段")
public class TVehHandleNote implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属处置单 id */
    @TableField("flow_id")
    @ApiModelProperty(value = "所属处置单 id")
    private Long flowId;

    /** 落段时系统数出来的当站（报文自报步次不进这格） */
    @TableField("stage")
    @ApiModelProperty(value = "落段当站（系统数出来的）")
    private Integer stage;

    /** 话种 OPEN定性/NOTIFY知会笔记/REPLY企业回告/REVIEW复查意见/CLOSE收口/MOVE挪步按语 */
    @TableField("note_kind")
    @ApiModelProperty(value = "话种 OPEN/NOTIFY/REPLY/REVIEW/CLOSE/MOVE")
    private String noteKind;

    /** 这一段写的话 */
    @TableField("note_text")
    @ApiModelProperty(value = "这一段写的话")
    private String noteText;

    /** 报文来意步次（仅 REPLY 段留痕，别处空） */
    @TableField("claim_stage")
    @ApiModelProperty(value = "报文来意步次（仅回告段）")
    private Integer claimStage;

    /** 落在哪一刻（墙钟，同秒两段也各是各的行） */
    @TableField("note_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落在哪一刻")
    private Date noteAt;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getNoteKind() {
        return noteKind;
    }

    public void setNoteKind(String noteKind) {
        this.noteKind = noteKind;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Integer getClaimStage() {
        return claimStage;
    }

    public void setClaimStage(Integer claimStage) {
        this.claimStage = claimStage;
    }

    public Date getNoteAt() {
        return noteAt;
    }

    public void setNoteAt(Date noteAt) {
        this.noteAt = noteAt;
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
}
