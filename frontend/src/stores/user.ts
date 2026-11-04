import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, getCurrentUser as apiGetCurrentUser, logout as apiLogout } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<Record<string, any> | null>(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  async function login(account: string, password: string) {
    const res: any = await apiLogin({ account, password })
    token.value = res.data?.accessToken || res.data?.token || res.token || ''
    userInfo.value = res.data?.user || res.user || null
    // 登录成功后拉取用户信息（后端登录响应不含 user 字段）
    if (token.value) {
      try {
        const me: any = await apiGetCurrentUser()
        userInfo.value = me.data || me
      } catch {
        // 用户信息拉取失败不阻断登录
      }
    }
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return { data: { token: token.value, user: userInfo.value } }
  }

  async function fetchUserInfo() {
    const res: any = await apiGetCurrentUser()
    userInfo.value = res.data || res
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // 忽略退出接口错误
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, login, fetchUserInfo, logout }
})
