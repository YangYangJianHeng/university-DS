<template>
  <div class="ai-chat-container">
    <!-- Sidebar: 会话列表 -->
    <aside class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h3 v-show="!sidebarCollapsed">AI 助手</h3>
        <el-button
          type="primary"
          :icon="Plus"
          circle
          size="small"
          @click="handleNewSession"
          title="新建对话"
        />
        <el-button
          :icon="sidebarCollapsed ? Expand : Fold"
          circle
          size="small"
          @click="sidebarCollapsed = !sidebarCollapsed"
          style="margin-left: 4px"
        />
      </div>
      <div class="session-list" v-show="!sidebarCollapsed">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: currentSessionId === s.id }"
          @click="switchSession(s)"
        >
          <div class="session-title">{{ s.title || '新的对话' }}</div>
          <div class="session-meta">
            <span>{{ s.messageCount || 0 }} 条消息</span>
            <el-popconfirm title="确定删除此对话？" @confirm="handleDeleteSession(s.id)">
              <template #reference>
                <el-icon class="delete-icon" :size="14"><Delete /></el-icon>
              </template>
            </el-popconfirm>
          </div>
        </div>
        <el-empty v-if="sessions.length === 0" description="暂无对话" :image-size="60" />
      </div>
    </aside>

    <!-- Main: 聊天区域 -->
    <main class="chat-main">
      <!-- 欢迎页 -->
      <div class="welcome-area" v-if="!currentSessionId">
        <div class="welcome-icon">🤖</div>
        <h1>你好，我是小星</h1>
        <p>你的专属校园竞赛与项目管理助手</p>
        <div class="quick-cards">
          <div class="quick-card" v-for="q in quickQuestions" :key="q" @click="startQuickQuestion(q)">
            {{ q }}
          </div>
        </div>
      </div>

      <!-- 聊天消息区 -->
      <div class="messages-area" v-else ref="messagesRef">
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>加载消息中...</span>
        </div>

        <template v-else>
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="message-row"
            :class="[msg.role, { 'animate-in': animateMessages }]"
            :style="{ animationDelay: animateMessages ? `${i * 0.08}s` : '0s' }"
          >
            <!-- AI 头像 -->
            <div v-if="msg.role === 'assistant'" class="message-avatar">🤖</div>
            <div class="message-bubble" :class="msg.role">
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
            <!-- 用户消息靠右，不需要头像 -->
          </div>

          <!-- 流式输出中的临时消息 -->
          <div class="message-row assistant" v-if="streaming">
            <div class="message-avatar">🤖</div>
            <div class="message-bubble assistant">
              <div class="message-text" v-html="renderMarkdown(streamContent)"></div>
              <span class="streaming-cursor"></span>
            </div>
          </div>

          <el-empty v-if="!loading && messages.length === 0 && !streaming"
            description="开始聊聊吧~" :image-size="80" />
        </template>
      </div>

      <!-- 输入区域 -->
      <div class="input-area" v-if="currentSessionId">
        <div class="input-wrapper">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入你的问题... (Enter 发送，Shift+Enter 换行)"
            @keydown.enter.exact="handleSend"
            :disabled="sending"
            resize="none"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            circle
            :loading="sending"
            :disabled="!inputText.trim()"
            @click="handleSend"
          />
        </div>
        <div class="input-tips">
          💡 试试问我：项目进度、竞赛推荐、团队协作建议
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { Plus, Fold, Expand, Delete, Promotion, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createSession, getSessions, deleteSession,
  getMessages, sendMessageStream
} from '@/api/ai'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked
marked.setOptions({ breaks: true, gfm: true })

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    const html = marked.parse(text) as string
    return DOMPurify.sanitize(html)
  } catch {
    return text.replace(/\n/g, '<br>')
  }
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

// ---- 状态 ----
const sidebarCollapsed = ref(false)
const sessions = ref<any[]>([])
const currentSessionId = ref<number | null>(null)
const messages = ref<any[]>([])
const inputText = ref('')
const sending = ref(false)
const streaming = ref(false)
const streamContent = ref('')
const loading = ref(false)
const animateMessages = ref(false)
const messagesRef = ref<HTMLElement | null>(null)

const quickQuestions = [
  '📊 我的项目进展怎么样？',
  '🏆 最近有什么比赛可以报名？',
  '👥 怎么优化团队协作？',
  '💡 可以帮我推荐适合的竞赛吗？',
  '⚠️ 我的项目有哪些风险？',
]

// ---- 会话管理 ----
async function loadSessions() {
  try {
    const res = await getSessions()
    sessions.value = res.data || []
  } catch { /* ignore */ }
}

async function handleNewSession() {
  try {
    const res = await createSession()
    const session = res.data
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
    animateMessages.value = false
  } catch {
    ElMessage.error('创建会话失败')
  }
}

function switchSession(session: any) {
  currentSessionId.value = session.id
  loadMessages()
}

async function loadMessages() {
  if (!currentSessionId.value) return
  loading.value = true
  animateMessages.value = false
  try {
    const res = await getMessages(currentSessionId.value)
    messages.value = res.data || []
    // 触发渐入动画
    await nextTick()
    animateMessages.value = true
    scrollToBottom()
  } catch {
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

async function handleDeleteSession(sessionId: number) {
  try {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

// ---- 发送消息 ----
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  // 追加用户消息到界面（禁用动画，直接显示）
  animateMessages.value = false
  messages.value.push({
    role: 'user',
    content: text,
    createdAt: new Date().toISOString()
  })
  inputText.value = ''
  await nextTick()
  scrollToBottom()

  // 流式调用
  sending.value = true
  streaming.value = true
  streamContent.value = ''

  sendMessageStream(
    currentSessionId.value!,
    text,
    (chunk: string) => {
      streamContent.value += chunk
      scrollToBottom()
    },
    () => {
      // 完成，将流式内容保存为正式消息
      messages.value.push({
        role: 'assistant',
        content: streamContent.value,
        createdAt: new Date().toISOString()
      })
      streamContent.value = ''
      streaming.value = false
      sending.value = false
      scrollToBottom()
      // 刷新会话列表更新标题
      loadSessions()
    },
    (err: any) => {
      ElMessage.error('回复失败: ' + (err?.message || '未知错误'))
      streaming.value = false
      sending.value = false
    }
  )
}

function startQuickQuestion(q: string) {
  // 先用快捷问题创建会话
  handleNewSession().then(() => {
    setTimeout(() => {
      inputText.value = q.replace(/^[^\s]+\s/, '') // 去掉 emoji
      handleSend()
    }, 300)
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 初始化
loadSessions()
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

/* ---- Sidebar ---- */
.chat-sidebar {
  width: 280px;
  min-width: 280px;
  background: var(--color-card);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
}
.chat-sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 12px;
  border-bottom: 1px solid #e4e7ed;
}
.sidebar-header h3 {
  flex: 1;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.session-item:hover {
  background: var(--color-bg);
}
.session-item.active {
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary-light);
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}
.delete-icon {
  cursor: pointer;
  color: #c0c4cc;
  transition: color 0.2s;
}
.delete-icon:hover {
  color: #f56c6c;
}

/* ---- Main Chat ---- */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* ---- Welcome ---- */
.welcome-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.welcome-icon {
  font-size: 64px;
}
.welcome-area h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}
.welcome-area p {
  font-size: 16px;
  color: var(--color-text-muted);
  margin: 0;
}

.quick-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
  max-width: 520px;
}
.quick-card {
  padding: 10px 18px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.quick-card:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64,158,255,0.15);
}

/* ---- Messages ---- */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: var(--color-text-muted);
}

.message-row {
  display: flex;
  gap: 10px;
  max-width: 80%;
}
.message-row.user {
  align-self: flex-end;
}
.message-row.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}
.message-bubble.user {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-bubble.user .message-time {
  color: rgba(255,255,255,0.7);
}
.message-bubble.assistant {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.message-text {
  font-size: 14px;
  word-break: break-word;
}
.message-text :deep(p) { margin: 0 0 8px 0; }
.message-text :deep(p:last-child) { margin: 0; }
.message-text :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.message-text :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}
.message-text :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}
.message-text :deep(ul), .message-text :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

.message-time {
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-top: 4px;
}

/* Typing / streaming cursor */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--color-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursorBlink 0.6s infinite;
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Message fade-in animation */
.message-row.animate-in {
  animation: fadeInUp 0.35s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- Input ---- */
.input-area {
  border-top: 1px solid var(--color-border-light);
  padding: 12px 16px;
  background: var(--color-card);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 14px;
  line-height: 1.5;
  border-color: var(--color-border);
}
.input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
}

.input-tips {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 6px;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .chat-sidebar {
    position: absolute;
    z-index: 10;
    height: 100%;
    box-shadow: 2px 0 8px rgba(0,0,0,0.1);
  }
  .chat-sidebar.collapsed {
    display: none;
  }
  .message-row {
    max-width: 95%;
  }
}
</style>
