package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserOauthBinding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserOauthBindingMapper extends BaseMapper<UserOauthBinding> {

    @Select("SELECT * FROM user_oauth_binding WHERE user_id = #{userId} AND platform = #{platform} AND is_deleted = 0 LIMIT 1")
    UserOauthBinding selectByUserAndPlatform(@Param("userId") Long userId, @Param("platform") String platform);

    @Select("SELECT * FROM user_oauth_binding WHERE user_id = #{userId} AND is_deleted = 0")
    List<UserOauthBinding> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_oauth_binding WHERE user_id = #{userId} AND platform = #{platform} AND is_deleted = 0")
    long countBinding(@Param("userId") Long userId, @Param("platform") String platform);

    @Update("UPDATE user_oauth_binding SET is_deleted = 1 WHERE user_id = #{userId} AND platform = #{platform} AND is_deleted = 0")
    int softDelete(@Param("userId") Long userId, @Param("platform") String platform);
}
