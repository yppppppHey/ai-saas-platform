<template>
  <div class="billing-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="quota-card">
          <template #header>配额概览</template>
          <div class="quota-item">
            <div class="quota-label">剩余 Token</div>
            <div class="quota-value">{{ quota.remaining || '--' }}</div>
          </div>
          <el-progress :percentage="quota.percentage || 0" :stroke-width="14" />
          <el-button type="primary" plain class="upgrade-btn" :icon="Star">升级 VIP</el-button>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>计费记录</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="today">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="records" v-loading="loading" stripe>
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="model" label="模型" width="150" />
            <el-table-column prop="tokens" label="Token 消耗" width="120" />
            <el-table-column prop="cost" label="费用" width="100" />
          </el-table>
          <el-empty v-if="records.length === 0 && !loading" description="暂无记录，联调后展示" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { Star } from '@element-plus/icons-vue'
import { getQuota, getBillingRecords } from '@/api/billing'

const quota = reactive({ remaining: 0, total: 0, percentage: 0 })
const records = ref<any[]>([])
const loading = ref(false)
const timeRange = ref('today')

async function loadQuota() {
  try {
    const res: any = await getQuota()
    const list = res.data || []
    // 取第一个配额（对话 Token）
    const q = list[0] || {}
    quota.remaining = q.dailyRemaining ?? '--'
    const dailyLimit = q.dailyLimit || 0
    const dailyUsed = q.dailyUsed || 0
    quota.total = dailyLimit
    quota.percentage = dailyLimit ? Math.round((dailyUsed / dailyLimit) * 100) : 0
  } catch {
    // 错误已处理
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const res: any = await getBillingRecords()
    const list = res.data?.records || res.data || []
    records.value = list.map((r: any) => ({
      time: r.createdAt || '',
      type: r.operationType || 'chat',
      model: r.modelId || '-',
      tokens: r.totalTokens ?? 0,
      cost: r.costCny != null ? '¥' + Number(r.costCny).toFixed(4) : (r.totalCost ?? 0),
    }))
  } catch {
    // 错误已处理
  } finally {
    loading.value = false
  }
}

watch(timeRange, loadRecords)
onMounted(() => { loadQuota(); loadRecords() })
</script>

<style scoped>
.quota-card {
  text-align: center;
}
.quota-item {
  margin-bottom: 16px;
}
.quota-label {
  font-size: 14px;
  color: #909399;
}
.quota-value {
  font-size: 32px;
  font-weight: 600;
  color: #409eff;
  margin-top: 4px;
}
.upgrade-btn {
  margin-top: 20px;
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
