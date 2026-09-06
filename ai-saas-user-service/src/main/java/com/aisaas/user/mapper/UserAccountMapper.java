package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户账户Mapper
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user_account WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    UserAccount selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user_account WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    UserAccount selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user_account WHERE phone = #{phone} AND is_deleted = 0 LIMIT 1")
    UserAccount selectByPhone(@Param("phone") String phone);

    /**
     * 根据用户名、邮箱或手机号查询用户
     */
    @Select("SELECT * FROM user_account WHERE (username = #{account} OR email = #{account} OR phone = #{account}) AND is_deleted = 0 LIMIT 1")
    UserAccount selectByAccount(@Param("account") String account);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM user_account WHERE username = #{username} AND is_deleted = 0")
    Long countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM user_account WHERE email = #{email} AND is_deleted = 0")
    Long countByEmail(@Param("email") String email);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) FROM user_account WHERE phone = #{phone} AND is_deleted = 0")
    Long countByPhone(@Param("phone") String phone);

    /**
     * 根据用户ID列表批量查询
     */
    List<UserAccount> selectBatchByIds(@Param("ids") List<Long> ids);
}
