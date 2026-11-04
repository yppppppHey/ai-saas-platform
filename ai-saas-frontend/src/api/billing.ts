import { request } from './request'

// 配额信息
export function getQuota() {
  return request({ url: '/quota/me', method: 'get' })
}

// Token 用量统计
export function getTokenUsage(params?: Record<string, any>) {
  return request({ url: '/billing/token/statistics', method: 'get', params })
}

// 计费记录（Token 使用记录分页）
export function getBillingRecords(params?: Record<string, any>) {
  return request({ url: '/billing/token/list', method: 'get', params })
}

// VIP 信息
export function getVipInfo() {
  return request({ url: '/vip/me', method: 'get' })
}

// 每日 Token 用量统计（date: YYYY-MM-DD）
export function getDailyTokenStatistics(userId: number | string, startDate: string, endDate: string) {
  return request({ url: '/billing/token/statistics/daily', method: 'get', params: { userId, startDate, endDate } })
}

// 模型调用分布统计
export function getModelStatistics(userId: number | string, startDate: string, endDate: string) {
  return request({ url: '/billing/token/statistics/model', method: 'get', params: { userId, startDate, endDate } })
}
