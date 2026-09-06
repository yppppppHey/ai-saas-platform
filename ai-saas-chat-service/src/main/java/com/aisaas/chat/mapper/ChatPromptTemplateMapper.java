package com.aisaas.chat.mapper;

import com.aisaas.chat.entity.ChatPromptTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 提示词模板Mapper
 */
public interface ChatPromptTemplateMapper extends BaseMapper<ChatPromptTemplate> {

    /**
     * 分页查询提示词模板
     */
    Page<ChatPromptTemplate> selectTemplatePage(Page<ChatPromptTemplate> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("category") String category,
                                                 @Param("templateType") String templateType,
                                                 @Param("isBuiltin") Integer isBuiltin);

    /**
     * 查询内置模板列表
     */
    @Select("SELECT * FROM chat_prompt_template WHERE is_builtin = 1 AND status = 1 AND is_deleted = 0 ORDER BY usage_count DESC")
    List<ChatPromptTemplate> selectBuiltinTemplates();

    /**
     * 查询用户的自定义模板
     */
    @Select("SELECT * FROM chat_prompt_template WHERE creator_id = #{userId} AND is_builtin = 0 AND is_deleted = 0 ORDER BY created_at DESC")
    List<ChatPromptTemplate> selectUserTemplates(@Param("userId") Long userId);

    /**
     * 根据分类查询模板
     */
    @Select("SELECT * FROM chat_prompt_template WHERE category = #{category} AND status = 1 AND is_deleted = 0 ORDER BY usage_count DESC")
    List<ChatPromptTemplate> selectByCategory(@Param("category") String category);

    /**
     * 增加使用次数
     */
    @Update("UPDATE chat_prompt_template SET usage_count = usage_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 检查模板名称是否存在
     */
    @Select("SELECT COUNT(*) FROM chat_prompt_template WHERE name = #{name} AND is_deleted = 0")
    Integer countByName(@Param("name") String name);
}
