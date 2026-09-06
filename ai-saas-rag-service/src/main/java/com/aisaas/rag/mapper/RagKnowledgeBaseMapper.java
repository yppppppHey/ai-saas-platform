package com.aisaas.rag.mapper;

import com.aisaas.rag.entity.RagKnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RagKnowledgeBaseMapper extends BaseMapper<RagKnowledgeBase> {

    @Select("SELECT * FROM rag_knowledge_base WHERE kb_id = #{kbId} AND is_deleted = 0")
    RagKnowledgeBase selectByKbId(@Param("kbId") String kbId);

    @Select("SELECT * FROM rag_knowledge_base WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<RagKnowledgeBase> selectByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM rag_knowledge_base WHERE is_deleted = 0 " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (kb_name LIKE CONCAT('%',#{keyword},'%') OR description LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<RagKnowledgeBase> selectKbPage(Page<RagKnowledgeBase> page, @Param("userId") Long userId,
                                         @Param("keyword") String keyword, @Param("status") Integer status);

    @Update("UPDATE rag_knowledge_base SET status = #{status}, updated_at = NOW() WHERE kb_id = #{kbId}")
    int updateStatus(@Param("kbId") String kbId, @Param("status") Integer status);

    @Update("UPDATE rag_knowledge_base SET total_documents = #{totalDocuments}, total_chunks = #{totalChunks}, " +
            "total_size = #{totalSize}, updated_at = NOW() WHERE kb_id = #{kbId}")
    int updateStatistics(@Param("kbId") String kbId, @Param("totalDocuments") Integer totalDocuments,
                       @Param("totalChunks") Integer totalChunks, @Param("totalSize") Long totalSize);

    @Select("SELECT COUNT(*) FROM rag_knowledge_base WHERE user_id = #{userId} AND status = #{status} AND is_deleted = 0")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
