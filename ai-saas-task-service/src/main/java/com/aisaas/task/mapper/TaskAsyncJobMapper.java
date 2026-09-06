package com.aisaas.task.mapper;

import com.aisaas.task.entity.TaskAsyncJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskAsyncJobMapper extends BaseMapper<TaskAsyncJob> {

    @Select("SELECT * FROM task_async_job WHERE task_id = #{taskId} AND is_deleted = 0")
    TaskAsyncJob selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM task_async_job WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<TaskAsyncJob> selectByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM task_async_job WHERE is_deleted = 0 " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='taskType != null'> AND task_type = #{taskType} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<TaskAsyncJob> selectTaskPage(Page<TaskAsyncJob> page, @Param("userId") Long userId,
                                       @Param("status") Integer status, @Param("taskType") String taskType);

    @Update("UPDATE task_async_job SET status = #{status}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updateStatus(@Param("taskId") String taskId, @Param("status") Integer status);

    @Update("UPDATE task_async_job SET progress = #{progress}, progress_detail = #{progressDetail}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updateProgress(@Param("taskId") String taskId, @Param("progress") Integer progress, @Param("progressDetail") String progressDetail);

    @Select("SELECT * FROM task_async_job WHERE status = #{status} AND is_deleted = 0 ORDER BY priority ASC, created_at ASC LIMIT #{limit}")
    List<TaskAsyncJob> selectByStatusWithLimit(@Param("status") Integer status, @Param("limit") Integer limit);

    @Select("SELECT * FROM task_async_job WHERE status IN (0, 1, 2) AND (timeout_at IS NULL OR timeout_at < NOW()) AND is_deleted = 0")
    List<TaskAsyncJob> selectTimeoutTasks();

    @Update("UPDATE task_async_job SET retry_count = retry_count + 1, updated_at = NOW() WHERE task_id = #{taskId}")
    int incrementRetryCount(@Param("taskId") String taskId);

    @Update("UPDATE task_async_job SET worker_node = #{workerNode}, started_at = NOW(), updated_at = NOW() WHERE task_id = #{taskId}")
    int assignWorker(@Param("taskId") String taskId, @Param("workerNode") String workerNode);

    @Select("SELECT COUNT(*) FROM task_async_job WHERE user_id = #{userId} AND status = #{status} AND is_deleted = 0")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
