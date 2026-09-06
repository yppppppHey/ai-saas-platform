package com.aisaas.rag.constant;

import lombok.Getter;

@Getter
public enum DocProcessStatusEnum {
    PENDING(0, "待处理", "文档等待处理"),
    PROCESSING(1, "处理中", "文档正在处理"),
    SUCCESS(2, "成功", "文档处理成功"),
    FAILED(3, "失败", "文档处理失败"),
    PARTIAL_SUCCESS(4, "部分成功", "文档部分处理成功");

    private final int code;
    private final String name;
    private final String description;

    DocProcessStatusEnum(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static DocProcessStatusEnum fromCode(int code) {
        for (DocProcessStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }

    public static boolean isCompleted(int code) {
        return code == SUCCESS.getCode() || code == FAILED.getCode() || code == PARTIAL_SUCCESS.getCode();
    }
}
