package com.aisaas.common.constant;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "禁止访问，权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    BUSINESS_ERROR(1000, "业务处理错误"),
    QUOTA_EXCEEDED(1001, "配额不足"),
    MODEL_CALL_FAILED(1002, "模型调用失败"),
    TASK_TIMEOUT(1003, "任务执行超时"),
    INVALID_CREDENTIALS(1004, "凭据无效"),
    ACCOUNT_LOCKED(1005, "账户已锁定"),
    PASSWORD_EXPIRED(1006, "密码已过期"),
    TOKEN_EXPIRED(1007, "令牌已过期"),
    TOKEN_INVALID(1008, "令牌无效"),
    SESSION_EXPIRED(1009, "会话已过期"),
    DATA_INTEGRITY_ERROR(1010, "数据完整性错误"),
    DUPLICATE_KEY(1011, "数据重复"),
    OPTIMISTIC_LOCK_FAILED(1012, "乐观锁失败"),
    EXTERNAL_SERVICE_ERROR(1013, "外部服务错误"),
    FILE_UPLOAD_FAILED(1014, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(1015, "文件下载失败"),
    INVALID_FILE_TYPE(1016, "不支持的文件类型"),
    FILE_TOO_LARGE(1017, "文件过大"),
    DATABASE_ERROR(1018, "数据库操作错误"),
    CACHE_ERROR(1019, "缓存操作错误"),
    NETWORK_ERROR(1020, "网络通信错误"),
    CONFIGURATION_ERROR(1021, "配置错误"),
    FEATURE_NOT_IMPLEMENTED(1022, "功能未实现"),
    FEATURE_DISABLED(1023, "功能已禁用"),
    MAINTENANCE_MODE(1024, "系统维护中"),
    RATE_LIMIT_EXCEEDED(1025, "速率限制 exceeded");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ResultCode getByCode(int code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode() == code) {
                return resultCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
