package com.aisaas.rag.mapper;

import com.aisaas.rag.entity.RagDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {

    @Select("SELECT * FROM rag_document WHERE doc_id = #{docId} AND is_deleted = 0")
    RagDocument selectByDocId(@Param("docId") String docId);

    @Select("SELECT * FROM rag_document WHERE doc_id = #{docId} AND is_deleted = 0 ORDER BY version DESC")
    List<RagDocument> selectVersionsByDocId(@Param("docId") String docId);

    @Select("SELECT * FROM rag_document WHERE kb_id = #{kbId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<RagDocument> selectByKbId(@Param("kbId") Long kbId);

    @Select("SELECT * FROM rag_document WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<RagDocument> selectByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM rag_document WHERE is_deleted = 0 " +
            "<if test='kbId != null'> AND kb_id = #{kbId} </if>" +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='docType != null and docType != \"\"'> AND doc_type = #{docType} </if>" +
            "<if test='processStatus != null'> AND process_status = #{processStatus} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND doc_name LIKE CONCAT('%',#{keyword},'%') </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<RagDocument> selectDocPage(Page<RagDocument> page, @Param("kbId") Long kbId,
                                     @Param("userId") Long userId, @Param("docType") String docType,
                                     @Param("processStatus") Integer processStatus, @Param("keyword") String keyword);

    @Update("UPDATE rag_document SET process_status = #{status}, updated_at = NOW() WHERE doc_id = #{docId}")
    int updateProcessStatus(@Param("docId") String docId, @Param("status") Integer status);

    @Update("UPDATE rag_document SET process_progress = #{progress}, process_status = #{status}, " +
            "updated_at = NOW() WHERE doc_id = #{docId}")
    int updateProgress(@Param("docId") String docId, @Param("progress") Integer progress, @Param("status") Integer status);

    @Update("UPDATE rag_document SET chunk_count = #{chunkCount}, process_status = #{status}, " +
            "process_completed_at = NOW(), updated_at = NOW() WHERE doc_id = #{docId}")
    int updateProcessCompleted(@Param("docId") String docId, @Param("chunkCount") Integer chunkCount, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM rag_document WHERE kb_id = #{kbId} AND process_status = #{status} AND is_deleted = 0")
    Long countByKbIdAndStatus(@Param("kbId") Long kbId, @Param("status") Integer processStatus);

    @Select("SELECT SUM(doc_size) FROM rag_document WHERE kb_id = #{kbId} AND is_deleted = 0")
    Long sumSizeByKbId(@Param("kbId") Long kbId);
}
