package com.aisaas.task.service;

import com.aisaas.common.util.JsonUtils;
import com.aisaas.common.util.RedisUtils;
import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.dto.TaskProgressDTO;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TaskProgressService {

    @Autowired
    private RedisUtils redisUtils;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    @org.springframework.beans.factory.annotation.Qualifier("taskScheduler")
    @org.springframework.beans.factory.annotation.Autowired
    private ScheduledExecutorService heartbeatExecutor;

    public TaskProgressService() {
        // 心跳调度移至 @PostConstruct：构造阶段 @Autowired 字段尚未注入（heartbeatExecutor 为 null），
        // 在构造器里调用会必然 NPE
    }

    @jakarta.annotation.PostConstruct
    public void initHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    public void updateProgress(String taskId, int progress, String detail) {
        try {
            TaskProgressDTO progressDTO = new TaskProgressDTO();
            progressDTO.setTaskId(taskId);
            progressDTO.setProgress(progress);
            progressDTO.setProgressDetail(detail);
            progressDTO.setUpdatedAt(LocalDateTime.now());

            String key = getProgressKey(taskId);
            redisUtils.set(key, JsonUtils.toJson(progressDTO), 3600, TimeUnit.SECONDS);

            notifyProgressUpdate(taskId, progressDTO);

            log.debug("任务进度更新: taskId={}, progress={}, detail={}", taskId, progress, detail);
        } catch (Exception e) {
            log.error("更新任务进度失败: taskId={}, error={}", taskId, e.getMessage());
        }
    }

    public TaskProgressDTO getProgress(String taskId) {
        try {
            String key = getProgressKey(taskId);
            String json = (String) redisUtils.get(key);
            if (json != null) {
                return JsonUtils.parseObject(json, TaskProgressDTO.class);
            }
        } catch (Exception e) {
            log.error("获取任务进度失败: taskId={}, error={}", taskId, e.getMessage());
        }
        return null;
    }

    public SseEmitter subscribeProgressStream(String taskId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(taskId);
            log.debug("SSE连接完成: taskId={}", taskId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(taskId);
            log.warn("SSE连接超时: taskId={}", taskId);
        });

        emitter.onError((e) -> {
            emitters.remove(taskId);
            log.error("SSE连接错误: taskId={}, error={}", taskId, e.getMessage());
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("taskId", taskId, "status", "connected", "timestamp", System.currentTimeMillis())));
        } catch (IOException e) {
            log.error("发送连接确认失败: taskId={}", taskId, e);
        }

        log.info("SSE进度流订阅: taskId={}", taskId);
        return emitter;
    }

    public void notifyTaskComplete(TaskAsyncJob task) {
        try {
            String taskId = task.getTaskId();
            SseEmitter emitter = emitters.get(taskId);

            if (emitter != null) {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("taskId", taskId);
                data.put("status", TaskStatusEnum.SUCCESS.getCode());
                data.put("statusDesc", TaskStatusEnum.SUCCESS.getDesc());
                data.put("progress", 100);
                data.put("message", "任务执行完成");
                data.put("completedAt", LocalDateTime.now().toString());

                emitter.send(SseEmitter.event()
                        .name("completed")
                        .data(data));

                emitter.complete();
                emitters.remove(taskId);
            }

            updateProgress(taskId, 100, "任务执行完成");
            log.info("通知任务完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("通知任务完成失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
        }
    }

    public void notifyTaskFail(TaskAsyncJob task, String errorMsg) {
        try {
            String taskId = task.getTaskId();
            SseEmitter emitter = emitters.get(taskId);

            if (emitter != null) {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("taskId", taskId);
                data.put("status", TaskStatusEnum.FAILED.getCode());
                data.put("statusDesc", TaskStatusEnum.FAILED.getDesc());
                data.put("error", errorMsg);
                data.put("retryCount", task.getRetryCount());
                data.put("maxRetry", task.getMaxRetry());
                data.put("failedAt", LocalDateTime.now().toString());

                emitter.send(SseEmitter.event()
                        .name("failed")
                        .data(data));

                emitter.complete();
                emitters.remove(taskId);
            }

            log.info("通知任务失败: taskId={}, error={}", taskId, errorMsg);
        } catch (Exception e) {
            log.error("通知任务失败异常: taskId={}, error={}", task.getTaskId(), e.getMessage());
        }
    }

    private void notifyProgressUpdate(String taskId, TaskProgressDTO progress) {
        try {
            SseEmitter emitter = emitters.get(taskId);
            if (emitter != null) {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("taskId", taskId);
                data.put("progress", progress.getProgress());
                data.put("progressDetail", progress.getProgressDetail());
                data.put("status", progress.getStatus());
                data.put("timestamp", System.currentTimeMillis());

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(data));
            }
        } catch (Exception e) {
            log.error("发送进度更新失败: taskId={}, error={}", taskId, e.getMessage());
        }
    }

    private void sendHeartbeats() {
        emitters.forEach((taskId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("taskId", taskId, "timestamp", System.currentTimeMillis())));
            } catch (Exception e) {
                log.debug("心跳发送失败，移除连接: taskId={}", taskId);
                emitters.remove(taskId);
            }
        });
    }

    private String getProgressKey(String taskId) {
        return "ai:platform:task:progress:" + taskId;
    }
}
