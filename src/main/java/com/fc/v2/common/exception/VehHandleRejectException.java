package com.fc.v2.common.exception;

/**
 * 严重报警处置单退回：必须点明卡在哪一站、哪一格。
 * 接口层捕到后照原话退回，不许只回一句"走不了"。
 * 挪步（推进/回退）被拒照旧返回 null/false 不抛；本异常只用于立单/笔记/回告/收口递交时的格错。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class VehHandleRejectException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 出毛病的那一格（字段名/站名） */
    private final String field;

    public VehHandleRejectException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
