<template>
  <div class="chat-page">
    <el-container class="chat-container">
      <!-- 会话列表 -->
      <el-aside width="260px" class="conversation-list">
        <div class="list-header">
          <el-button type="primary" size="small" :icon="Plus" @click="createConversation">新建对话</el-button>
        </div>
        <el-scrollbar>
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conversation-item"
            :class="{ active: currentConversationId === conv.id }"
            @click="selectConversation(conv)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="conv-title">{{ conv.title || '新对话' }}</span>
          </div>
          <el-empty v-if="conversations.length === 0" description="暂无对话" :image-size="60" />
        </el-scrollbar>
      </el-aside>

      <!-- 对话主区域 -->
      <el-main class="chat-main">
        <el-scrollbar ref="messageScrollRef" class="message-list">
          <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
            <div class="message-avatar">
              <el-avatar :size="36" :icon="msg.role === 'user' ? User : Service" />
            </div>
            <div class="message-content">
              <div class="message-bubble">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="messages.length === 0" class="empty-chat">
            <el-icon :size="64" color="#c0c4cc"><ChatDotRound /></el-icon>
            <p>开始与 AI 对话吧</p>
          </div>
        </el-scrollbar>

        <!-- 输入区 -->
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <el-button :icon="CircleClose" :disabled="!isStreaming" @click="abortGeneration">停止</el-button>
            <el-button type="primary" :icon="Promotion" :loading="isStreaming" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ChatDotRound, User, Service, CircleClose, Promotion } from '@element-plus/icons-vue'
import { getConversations, getMessages, streamChat, createConversation as apiCreateConversation, abortChat } from '@/api/chat'

const conversations = ref<any[]>([])
const currentConversationId = ref<string>('')
const messages = ref<any[]>([])
const inputText = ref('')
const isStreaming = ref(false)
const messageScrollRef = ref()
const currentMessageId = ref<string>('')

async function loadConversations() {
  try {
    const res: any = await getConversations()
    conversations.value = res.data?.records || res.data || []
    if (conversations.value.length > 0) {
      selectConversation(conversations.value[0])
    }
  } catch {
    ElMessage.error('会话列表加载失败')
  }
}

async function createConversation() {
  try {
    const res: any = await apiCreateConversation({ title: '新对话', model: 'deepseek-chat' })
    const id = res.data
    ElMessage.success('新建对话成功')
    await loadConversations()
    const created = conversations.value.find((c: any) => c.id === id)
    if (created) selectConversation(created)
  } catch {
    ElMessage.error('新建对话失败')
  }
}

async function selectConversation(conv: any) {
  currentConversationId.value = String(conv.id)
  messages.value = []
  try {
    const res: any = await getMessages(String(conv.id))
    const list = res.data?.records || res.data || []
    messages.value = list.map((m: any) => ({
      id: String(m.id),
      role: m.role === 'user' ? 'user' : 'assistant',
      content: m.content || '',
    }))
    scrollToBottom()
  } catch {
    ElMessage.error('消息加载失败')
  }
}

async function sendMessage() {
  if (!inputText.value.trim()) return
  if (isStreaming.value) return

  // 无会话时先自动创建
  if (!currentConversationId.value) {
    try {
      const res: any = await apiCreateConversation({ title: '新对话', model: 'deepseek-chat' })
      currentConversationId.value = String(res.data)
      await loadConversations()
    } catch {
      ElMessage.error('创建会话失败，请稍后再试')
      return
    }
  }

  const userMsg = { id: Date.now().toString(), role: 'user', content: inputText.value }
  messages.value.push(userMsg)
  const question = inputText.value
  inputText.value = ''
  isStreaming.value = true
  currentMessageId.value = ''

  const aiMsg = reactive({ id: (Date.now() + 1).toString(), role: 'assistant', content: '' })
  messages.value.push(aiMsg)

  streamChat(
    { conversationId: Number(currentConversationId.value), content: question },
    (chunk) => {
      if (chunk && chunk !== '[DONE]') {
        aiMsg.content += chunk
        scrollToBottom()
      }
    },
    () => {
      isStreaming.value = false
      if (!aiMsg.content) aiMsg.content = '未能生成回复（模型服务未配置或调用失败）'
      scrollToBottom()
    },
    (error) => {
      isStreaming.value = false
      if (!aiMsg.content) {
        aiMsg.content = '生成失败：' + (error?.message || '未知错误')
      }
      ElMessage.error('生成失败')
    },
  )

  scrollToBottom()
}

async function abortGeneration() {
  isStreaming.value = false
  if (currentMessageId.value) {
    try {
      await abortChat(currentMessageId.value)
      ElMessage.info('已停止生成')
    } catch {
      ElMessage.info('已停止生成')
    }
  } else {
    ElMessage.info('已停止生成')
  }
}

function scrollToBottom() {
  nextTick(() => {
    const scrollbar = messageScrollRef.value
    if (scrollbar) {
      scrollbar.scrollTop = scrollbar.scrollHeight
    }
  })
}

onMounted(loadConversations)
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 100px);
}
.chat-container {
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.conversation-list {
  border-right: 1px solid #e4e7ed;
  background: #fafafa;
}
.list-header {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
}
.conversation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin: 4px 8px;
}
.conversation-item:hover {
  background: #f0f2f5;
}
.conversation-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.chat-main {
  display: flex;
  flex-direction: column;
  padding: 0;
}
.message-list {
  flex: 1;
  padding: 20px;
}
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-item.user {
  flex-direction: row-reverse;
}
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-item.user .message-bubble {
  background: #409eff;
  color: #fff;
}
.empty-chat {
  text-align: center;
  padding-top: 120px;
  color: #c0c4cc;
}
.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 16px 20px;
}
.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
