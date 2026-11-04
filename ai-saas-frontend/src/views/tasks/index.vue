<template>
  <div class="tasks-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
          <el-button type="primary" size="small" :icon="Refresh" @click="loadTasks">刷新</el-button>
        </div>
      </template>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="id" label="任务 ID" width="200" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="12" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'RUNNING'" type="danger" size="small" link @click="cancelTask(row)">取消</el-button>
            <el-button type="primary" size="small" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="tasks.length === 0 && !loading" description="暂无任务，联调后展示" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getTasks, cancelTask as apiCancelTask } from '@/api/task'

const tasks = ref<any[]>([])
const loading = ref(false)

async function loadTasks() {
  loading.value = true
  try {
    const res: any = await getTasks()
    const list = res.data?.records || res.data || []
    tasks.value = list.map((t: any) => ({
      id: t.id,
      type: t.taskType || t.type,
      status: t.status,
      progress: t.progress || 0,
      createdAt: t.createdAt || '',
    }))
  } catch {
    // 错误已处理
  } finally {
    loading.value = false
  }
}

function statusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'info',
    RUNNING: 'primary',
    SUCCESS: 'success',
    FAILED: 'danger',
    CANCELLED: 'warning',
  }
  return map[status] || 'info'
}

async function cancelTask(row: any) {
  await ElMessageBox.confirm('确定取消该任务？', '提示', { type: 'warning' })
  try {
    await apiCancelTask(row.id)
    ElMessage.success('已取消')
    loadTasks()
  } catch {
    // 错误已处理
  }
}

function viewDetail(row: any) {
  ElMessage.info(`任务 ${row.id} 详情待联调`)
}

onMounted(loadTasks)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
