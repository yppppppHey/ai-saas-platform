package com.aisaas.rag.constant;

import lombok.Getter;

@Getter
public enum KbAccessLevelEnum {
    PRIVATE(1, "私有", "仅创建者可访问"),
    TEAM(2, "团队", "团队成员可访问"),
    PUBLIC(3, "公开", "所有人可访问");

    private final int code;
    private final String name;
    private final String description;

    KbAccessLevelEnum(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static KbAccessLevelEnum fromCode(int code) {
        for (KbAccessLevelEnum level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return PRIVATE;
    }
}
