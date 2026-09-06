package com.aisaas.rag.constant;

import lombok.Getter;

@Getter
public enum KbStatusEnum {
    DISABLED(0, "禁用", "知识库已禁用"),
    ACTIVE(1, "正常", "知识库正常使用"),
    BUILDING(2, "构建中", "知识库正在构建索引"),
    BUILD_FAILED(3, "构建失败", "知识库构建失败"),
    SYNCING(4, "同步中", "知识库正在同步数据");

    private final int code;
    private final String name;
    private final String description;

    KbStatusEnum(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static KbStatusEnum fromCode(int code) {
        for (KbStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return ACTIVE;
    }

    public static boolean canBuildIndex(int code) {
        return code == ACTIVE.getCode() || code == BUILD_FAILED.getCode();
    }
}
