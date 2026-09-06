package com.aisaas.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 第三方账号绑定
 */
@Data
@TableName("user_oauth_binding")
public class UserOauthBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String platform;
    private String accountId;
    private String nickname;
    private LocalDateTime bindAt;
    private Integer isDeleted;
}
