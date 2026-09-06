package com.aisaas.common.ai.agent.state;

/**
 * 工作流状态枚举
 * 定义工作流执行过程中的所有可能状态
 */
public enum WorkflowState {
    /**
     * 待执行
     */
    PENDING("PENDING", "待执行"),

    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 已暂停
     */
    PAUSED("PAUSED", "已暂停"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 执行失败
     */
    FAILED("FAILED", "执行失败"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消"),

    /**
     * 执行超时
     */
    TIMEOUT("TIMEOUT", "执行超时"),

    /**
     * 等待用户输入
     */
    WAITING_FOR_INPUT("WAITING_FOR_INPUT", "等待用户输入"),

    /**
     * 等待子流程
     */
    WAITING_FOR_SUBFLOW("WAITING_FOR_SUBFLOW", "等待子流程"),

    /**
     * 等待外部事件
     */
    WAITING_FOR_EVENT("WAITING_FOR_EVENT", "等待外部事件");

    private final String code;
    private final String description;

    WorkflowState(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取状态枚举
     */
    public static WorkflowState fromCode(String code) {
        for (WorkflowState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        return null;
    }

    /**
     * 是否为活跃状态（可以执行）
     */
    public boolean isActive() {
        return this == PENDING || this == RUNNING || this == PAUSED
                || this == WAITING_FOR_INPUT || this == WAITING_FOR_SUBFLOW
                || this == WAITING_FOR_EVENT;
    }

    /**
     * 是否为终态（不可再执行）
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }

    /**
     * 是否可以暂停
     */
    public boolean canPause() {
        return this == RUNNING;
    }

    /**
     * 是否可以恢复
     */
    public boolean canResume() {
        return this == PAUSED;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == RUNNING || this == PAUSED
                || this == WAITING_FOR_INPUT || this == WAITING_FOR_SUBFLOW
                || this == WAITING_FOR_EVENT;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this == FAILED || this == TIMEOUT;
    }
}
