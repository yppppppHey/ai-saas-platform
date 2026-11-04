package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户登录日志Mapper
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {

    /**
     * 根据用户ID查询登录日志
     */
    @Select("SELECT * FROM user_login_log WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserLoginLog> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最近的登录日志
     */
    @Select("SELECT * FROM user_login_log WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserLoginLog> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查询用户最新的登录记录
     */
    @Select("SELECT * FROM user_login_log WHERE user_id = #{userId} AND login_status = 1 ORDER BY created_at DESC LIMIT 1")
    UserLoginLog selectLatestSuccessByUserId(@Param("userId") Long userId);
}
