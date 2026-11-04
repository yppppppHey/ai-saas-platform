package com.aisaas.user.messaging;

/**
 * 验证码发送器
 *
 * 默认实现为日志输出（本地/自测环境）；接入真实短信或邮件服务时
 * 只需新增一个实现（如 AliyunSmsNotifier / MailNotifier）并替换 Bean，
 * 业务层 sendVerifyCode 不需要改动。
 */
public interface VerifyCodeNotifier {

    /**
     * @param target 手机号或邮箱
     * @param type   sms / email
     * @param code   验证码
     */
    void send(String target, String type, String code);
}
