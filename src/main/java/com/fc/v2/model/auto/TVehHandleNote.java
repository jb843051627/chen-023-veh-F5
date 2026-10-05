package com.fc.v2.model.auto;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 严重报警处置单按语卷对象 t_veh_handle_note
 *
 * 一段一行：哪一站、第几段、什么话、落在哪一刻。
 * 同一站第二遍报上来只改头一行（seg_ordinal 不动、不另起新行）；
 * 退站重写压到卷末另攒新段序；同秒两条来路各记各的，不并一条、不漏一条。
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_veh_handle_note")
@ApiModel(value = "TVehHandleNote", description = "严重报警处置单按语卷")
public class TVehHandleNote implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属处置单 */
    @TableField("flow_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属处置单")
    private Long flowId;

    /** 落在哪一站 0定性 1知会 2回告 3归档 */
    @TableField("stage")
    @ApiModelProperty(value = "落在哪一站")
    private Integer stage;

    /** 这一站的第几回（退站重走+1，同站第二遍不+1） */
    @TableField("visit_no")
    @ApiModelProperty(value = "这一站的第几回")
    private Integer visitNo;

    /** 来路号（同一来路第二遍并头一行；同秒两个来路各起一行） */
    @TableField("source_no")
    @ApiModelProperty(value = "来路号")
    private String sourceNo;

    /** 段序（从入档往回数当班簿几笔，对的就是它） */
    @TableField("seg_ordinal")
    @ApiModelProperty(value = "段序")
    private Integer segOrdinal;

    /** 这一站写下的话 */
    @TableField("note_text")
    @ApiModelProperty(value = "按语正文")
    private String noteText;

    /** 落在哪一刻 */
    @TableField("written_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落在哪一刻")
    private Date writtenAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

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

    public Integer getSegOrdinal() {
        return segOrdinal;
    }

    public void setSegOrdinal(Integer segOrdinal) {
        this.segOrdinal = segOrdinal;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Date getWrittenAt() {
        return writtenAt;
    }

    public void setWrittenAt(Date writtenAt) {
        this.writtenAt = writtenAt;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
