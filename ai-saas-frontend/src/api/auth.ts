import { request } from './request'

// 发送验证码
export function sendCode(params: { target: string; type: string }) {
  return request({ url: '/auth/send-code', method: 'post', params })
}

// 注册
export function register(data: Record<string, any>) {
  return request({ url: '/auth/register', method: 'post', data })
}

// 登录
export function login(data: { account: string; password: string }) {
  return request({ url: '/auth/login', method: 'post', data })
}

// 退出登录
export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

// 获取当前用户信息
export function getCurrentUser() {
  return request({ url: '/user/me', method: 'get' })
}
