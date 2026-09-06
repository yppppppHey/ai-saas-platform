package com.aisaas.rag.service;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.maintenance.*;
import java.util.List;

public interface KbMaintenanceService {
    Result<Boolean> rebuildIndex(String kbId, Long userId);
    Result<RebuildProgressDTO> getRebuildProgress(String kbId, Long userId);
    Result<Boolean> syncDocuments(String kbId, List<String> docIds, Long userId);
    Result<SyncStatusDTO> getSyncStatus(String kbId, Long userId);
    Result<StorageUsageDTO> getStorageUsage(String kbId, Long userId);
    Result<List<StorageUsageDTO>> getStorageTrend(String kbId, Integer days, Long userId);
    Result<Boolean> cleanupOrphanChunks(String kbId, Long userId);
    Result<Boolean> optimizeIndex(String kbId, Long userId);
    Result<BackupDTO> createBackup(String kbId, Long userId);
    Result<List<BackupDTO>> listBackups(String kbId, Long userId);
    Result<Boolean> restoreFromBackup(String kbId, Long backupId, Long userId);
    Result<Boolean> deleteBackup(String kbId, Long backupId, Long userId);
}
