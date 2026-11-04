<template>
  <div class="settings-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>个人资料</template>
          <el-form :model="profileForm" label-width="80px">
            <el-form-item label="头像">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="handleAvatarChange">
                <el-avatar :size="64" :icon="UserFilled" :src="profileForm.avatar" />
              </el-upload>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>修改密码</template>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 20px">
          <template #header>账号信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户 ID">{{ userStore.userInfo?.userId ?? userStore.userInfo?.id ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ userStore.userInfo?.createdAt || '--' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag type="success">正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile, updateUserProfile, changePassword as apiChangePassword, uploadAvatar } from '@/api/user'

const userStore = useUserStore()
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  username: '',
  email: '',
  phone: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function loadProfile() {
  try {
    const res: any = await getUserProfile()
    const data = res.data || res
    profileForm.username = data.username || ''
    profileForm.email = data.email || ''
    profileForm.phone = data.phone || ''
    profileForm.avatar = data.avatarUrl || ''
    // 回写 store，刷新账号信息区
    userStore.userInfo = { ...(userStore.userInfo || {}), ...data }
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
  } catch {
    // 错误已处理
  }
}

async function saveProfile() {
  try {
    await updateUserProfile({ username: profileForm.username, email: profileForm.email, phone: profileForm.phone })
    ElMessage.success('保存成功')
    if (userStore.userInfo) {
      userStore.userInfo.email = profileForm.email
      userStore.userInfo.phone = profileForm.phone
    }
  } catch {
    // 错误已处理
  }
}

async function handleAvatarChange(file: any) {
  try {
    await uploadAvatar(file.raw)
    ElMessage.success('头像上传成功')
    loadProfile()
  } catch {
    // 错误已处理
  }
}

async function changePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate()
  try {
    await apiChangePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword, confirmPassword: passwordForm.confirmPassword })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    // 错误已处理
  }
}

onMounted(loadProfile)
</script>
