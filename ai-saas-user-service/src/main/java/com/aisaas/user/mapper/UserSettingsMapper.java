package com.aisaas.user.mapper;

import com.aisaas.user.entity.UserSettings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户设置Mapper
 */
@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {

    /**
     * 根据用户ID查询设置
     */
    @Select("SELECT * FROM user_settings WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    UserSettings selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除设置
     */
    int deleteByUserId(@Param("userId") Long userId);
}
