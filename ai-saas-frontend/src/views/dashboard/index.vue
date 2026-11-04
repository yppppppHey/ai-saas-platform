<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-trend" :class="card.trend > 0 ? 'up' : 'down'">
                <el-icon><CaretTop v-if="card.trend > 0" /><CaretBottom v-else /></el-icon>
                {{ Math.abs(card.trend) }}% 较上周
              </div>
            </div>
            <div class="stat-icon" :style="{ background: card.bgColor }">
              <el-icon :size="28" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <!-- Token 用量趋势 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>Token 用量趋势</span>
              <el-radio-group v-model="trendRange" size="small">
                <el-radio-button label="7">近 7 天</el-radio-button>
                <el-radio-button label="30">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 任务状态分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>任务状态分布</template>
          <div ref="taskPieRef" class="chart-container" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 模型使用分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>模型调用分布</template>
          <div ref="modelBarRef" class="chart-container" style="height: 280px"></div>
        </el-card>
      </el-col>

      <!-- 最近活动 -->
      <el-col :span="8">
        <el-card>
          <template #header>最近活动</template>
          <el-timeline>
            <el-timeline-item
              v-for="activity in recentActivities"
              :key="activity.id"
              :timestamp="activity.time"
              :type="activity.type"
              :hollow="true"
            >
              <div class="activity-item">
                <span class="activity-title">{{ activity.title }}</span>
                <span class="activity-desc">{{ activity.desc }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <!-- 系统状态 -->
      <el-col :span="8">
        <el-card>
          <template #header>系统状态</template>
          <div class="service-status">
            <div v-for="svc in services" :key="svc.name" class="service-item">
              <div class="service-left">
                <el-icon :color="svc.online ? '#67c23a' : '#f56c6c'">
                  <CircleCheck v-if="svc.online" /><CircleClose v-else />
                </el-icon>
                <span class="service-name">{{ svc.name }}</span>
              </div>
              <el-tag :type="svc.online ? 'success' : 'danger'" size="small">
                {{ svc.online ? '运行中' : '离线' }}
              </el-tag>
            </div>
          </div>
          <el-divider />
          <div class="system-info">
            <div class="info-row"><span>版本</span><span>v1.0.0</span></div>
            <div class="info-row"><span>环境</span><span>开发环境</span></div>
            <div class="info-row"><span>运行时长</span><span>{{ uptime }}</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { CaretTop, CaretBottom, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getConversations } from '@/api/chat'
import { getTasks } from '@/api/task'
import { getKnowledgeBases } from '@/api/rag'
import { getQuota, getDailyTokenStatistics, getModelStatistics } from '@/api/billing'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 统计卡片（真实数据：会话/任务/知识库/配额）
const statCards = ref([
  { title: '会话总数', value: '--', trend: 0, icon: 'ChatDotRound', color: '#409eff', bgColor: '#ecf5ff' },
  { title: '任务总数', value: '--', trend: 0, icon: 'List', color: '#67c23a', bgColor: '#f0f9eb' },
  { title: '知识库', value: '--', trend: 0, icon: 'FolderOpened', color: '#e6a23c', bgColor: '#fdf6ec' },
  { title: '今日剩余配额', value: '--', trend: 0, icon: 'Wallet', color: '#f56c6c', bgColor: '#fef0f0' },
])

const trendRange = ref('7')
const trendChartRef = ref()
const taskPieRef = ref()
const modelBarRef = ref()
let trendChart: echarts.ECharts | null = null
let taskPie: echarts.ECharts | null = null
let modelBar: echarts.ECharts | null = null

// Token 用量趋势（真实数据）
interface TrendData { dates: string[]; tokens: number[]; calls: number[] }
const trendData = ref<Record<string, TrendData>>({
  '7': { dates: [], tokens: [], calls: [] },
  '30': { dates: [], tokens: [], calls: [] },
})

// 任务状态分布（真实聚合）
const taskStatusData = ref<{ value: number; name: string; itemStyle: { color: string } }[]>([])

// 模型调用分布（真实数据）
const modelData = ref<{ models: string[]; values: number[] }>({ models: [], values: [] })

// 最近活动（演示数据，待聚合多接口）
const recentActivities = ref([
  { id: 1, title: '新建对话', desc: '用户发起了一轮新的 AI 对话', time: '—', type: 'primary' },
  { id: 2, title: '任务完成', desc: '文档向量化任务执行成功', time: '—', type: 'success' },
])

// 服务状态（演示标注，联调期以进程在线为准）
const services = ref([
  { name: '网关服务', online: true },
  { name: '用户服务', online: true },
  { name: '对话服务', online: true },
  { name: '任务服务', online: true },
  { name: 'RAG 服务', online: true },
  { name: '计费服务', online: true },
])

const uptime = ref('开发环境')

function fmtDate(offset: number) {
  const d = new Date()
  d.setDate(d.getDate() - offset)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

async function loadStats() {
  try {
    const [convRes, taskRes, kbRes, quotaRes] = await Promise.all([
      getConversations({ page: 1, size: 1 }),
      getTasks({ page: 1, size: 1 }),
      getKnowledgeBases({ page: 1, size: 1 }),
      getQuota(),
    ])
    statCards.value[0].value = String(convRes.data?.total ?? 0)
    statCards.value[1].value = String(taskRes.data?.total ?? 0)
    statCards.value[2].value = String(kbRes.data?.total ?? 0)
    const quotaList = quotaRes.data || []
    const q = quotaList[0] || {}
    statCards.value[3].value = q.dailyRemaining != null ? String(q.dailyRemaining) : '--'
  } catch {
    // 统计卡片加载失败保持占位
  }
}

async function loadTrend(range: string) {
  const userId = userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return
  const days = range === '7' ? 7 : 30
  const startDate = fmtDate(days - 1)
  const endDate = fmtDate(0)
  try {
    const res: any = await getDailyTokenStatistics(userId, startDate, endDate)
    const items = res.data?.items || []
    const dates = items.map((i: any) => String(i.date).slice(5))
    const tokens = items.map((i: any) => i.tokens || 0)
    const calls = items.map((i: any) => i.count || 0)
    trendData.value[range] = { dates, tokens, calls }
  } catch {
    trendData.value[range] = { dates: [], tokens: [], calls: [] }
  }
  updateTrendChart()
}

async function loadTaskStatus() {
  try {
    const res: any = await getTasks({ page: 1, size: 100 })
    const list = res.data?.records || []
    const statusMap: Record<string, number> = {}
    list.forEach((t: any) => { statusMap[t.status] = (statusMap[t.status] || 0) + 1 })
    const colors: Record<string, string> = { RUNNING: '#409eff', SUCCESS: '#67c23a', FAILED: '#f56c6c', CANCELLED: '#909399', PENDING: '#e6a23c' }
    taskStatusData.value = Object.entries(statusMap).map(([name, value]) => ({
      value,
      name,
      itemStyle: { color: colors[name] || '#909399' },
    }))
  } catch {
    taskStatusData.value = []
  }
  updateTaskPie()
}

async function loadModelStats() {
  const userId = userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return
  try {
    const res: any = await getModelStatistics(userId, fmtDate(29), fmtDate(0))
    const items = res.data?.items || []
    modelData.value = {
      models: items.map((i: any) => i.modelId || i.provider || 'unknown'),
      values: items.map((i: any) => i.tokens || 0),
    }
  } catch {
    modelData.value = { models: [], values: [] }
  }
  updateModelBar()
}

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  updateTrendChart()
}

function updateTrendChart() {
  if (!trendChart) return
  const data = trendData.value[trendRange.value] || { dates: [], tokens: [], calls: [] }
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Token 消耗', '调用次数'], right: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.dates },
    yAxis: [
      { type: 'value', name: 'Token' },
      { type: 'value', name: '次数' },
    ],
    series: [
      {
        name: 'Token 消耗',
        type: 'line',
        smooth: true,
        data: data.tokens,
        areaStyle: { opacity: 0.1 },
        itemStyle: { color: '#409eff' },
      },
      {
        name: '调用次数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: data.calls,
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
}

function initTaskPie() {
  if (!taskPieRef.value) return
  taskPie = echarts.init(taskPieRef.value)
  updateTaskPie()
}

function updateTaskPie() {
  if (!taskPie) return
  const data = taskStatusData.value.length > 0 ? taskStatusData.value : [{ value: 1, name: '暂无数据', itemStyle: { color: '#e4e7ed' } }]
  taskPie.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data,
    }],
  })
}

function initModelBar() {
  if (!modelBarRef.value) return
  modelBar = echarts.init(modelBarRef.value)
  updateModelBar()
}

function updateModelBar() {
  if (!modelBar) return
  const models = modelData.value.models.length > 0 ? modelData.value.models : ['暂无数据']
  const values = modelData.value.models.length > 0 ? modelData.value.values : [0]
  modelBar.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: models, axisLabel: { rotate: 20, fontSize: 11 } },
    yAxis: { type: 'value', name: 'Token' },
    series: [{
      type: 'bar',
      data: values,
      barWidth: '50%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 1, color: '#188df0' },
        ]),
      },
    }],
  })
}

function handleResize() {
  trendChart?.resize()
  taskPie?.resize()
  modelBar?.resize()
}

watch(trendRange, () => nextTick(() => loadTrend(trendRange.value)))

onMounted(() => {
  nextTick(() => {
    initTrendChart()
    initTaskPie()
    initModelBar()
  })
  window.addEventListener('resize', handleResize)
  loadStats()
  loadTrend(trendRange.value)
  loadTaskStatus()
  loadModelStats()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  taskPie?.dispose()
  modelBar?.dispose()
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
}
.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}
.stat-title {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.stat-trend {
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 2px;
}
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-container {
  width: 100%;
}
.activity-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.activity-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.activity-desc {
  font-size: 12px;
  color: #909399;
}
.service-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.service-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.service-name {
  font-size: 14px;
  color: #303133;
}
.system-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}
</style>
