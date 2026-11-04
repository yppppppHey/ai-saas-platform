import { request } from './request'

// 任务列表
export function getTasks(params?: Record<string, any>) {
  return request({ url: '/v1/tasks', method: 'get', params })
}

// 任务详情
export function getTaskDetail(id: string) {
  return request({ url: `/v1/tasks/${id}`, method: 'get' })
}

// 创建任务
export function createTask(data: Record<string, any>) {
  return request({ url: '/v1/tasks', method: 'post', data })
}

// 取消任务
export function cancelTask(id: string) {
  return request({ url: `/v1/tasks/${id}/cancel`, method: 'post' })
}

// 任务类型列表
export function getTaskTypes() {
  return request({ url: '/v1/tasks/types', method: 'get' })
}
