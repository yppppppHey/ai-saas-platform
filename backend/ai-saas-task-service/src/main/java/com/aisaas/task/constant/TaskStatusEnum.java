package com.aisaas.task.constant;

import lombok.Getter;

@Getter
public enum TaskStatusEnum {
    PENDING(0, "待处理"),
    QUEUED(1, "已入队"),
    RUNNING(2, "执行中"),
    SUCCESS(3, "成功"),
    FAILED(4, "失败"),
    CANCELLED(5, "已取消"),
    TIMEOUT(6, "超时");

    private final int code;
    private final String desc;

    TaskStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatusEnum fromCode(int code) {
        for (TaskStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }

    public static boolean isFinalStatus(int code) {
        return code == SUCCESS.code || code == FAILED.code || code == CANCELLED.code || code == TIMEOUT.code;
    }

    public static boolean canCancel(int code) {
        return code == PENDING.code || code == QUEUED.code || code == RUNNING.code;
    }

    public static boolean canRetry(int code) {
        return code == FAILED.code || code == TIMEOUT.code;
    }
}
