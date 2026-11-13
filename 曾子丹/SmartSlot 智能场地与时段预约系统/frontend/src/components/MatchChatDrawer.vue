<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="420px"
    direction="rtl"
    destroy-on-close
    class="match-chat-drawer"
    :before-close="handleClose"
  >
    <template #header>
      <div class="chat-drawer-header">
        <div class="header-main-line">
          <span class="activity-sport-badge">{{ activity?.categoryName || '拼场' }}</span>
          <h3 class="header-title" :title="activity?.title">{{ activity?.title || '搭子微室' }}</h3>
        </div>
        <div class="header-sub-meta">
          <div class="conn-status" :class="{ online: isConnected, reconnecting: !isConnected }">
            <span class="status-indicator-dot"></span>
            <span>{{ isConnected ? `微室在线 ${onlineCount} 人` : '连接中...' }}</span>
          </div>
          <span class="venue-brief" :title="activity?.venueName">
            <el-icon><Location /></el-icon> {{ activity?.venueName }}
          </span>
        </div>
      </div>
    </template>

    <div class="chat-main-container">
      <!-- 顶部活动快照与安全须知卡 -->
      <div class="match-info-ribbon">
        <div class="ribbon-time">
          <el-icon><Calendar /></el-icon>
          <span>{{ activity?.bookDate }} · {{ activity?.timeSlot }}</span>
        </div>
        <div class="ribbon-status-badge" :class="activity?.status === 1 ? 'status-locked' : 'status-recruiting'">
          {{ activity?.status === 1 ? '已成团满员' : `招募中 (${activity?.currentMembers}/${activity?.targetMembers}人)` }}
        </div>
      </div>

      <!-- 消息时间轴滚动区 -->
      <div ref="messageContainerRef" class="messages-scroll-area">
        <div v-if="loadingHistory" class="chat-loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在拉取微室记录...</span>
        </div>

        <div v-else-if="messages.length === 0" class="chat-empty-state">
          <div class="empty-bubble-icon">💬</div>
          <p>搭子微室已就绪，向球友发条打招呼消息吧！</p>
          <span class="empty-sub">支持点击下方快捷战术胶囊一键发送</span>
        </div>

        <div v-else class="message-stream-list">
          <template v-for="(msg, index) in messages" :key="msg.id || index">
            <!-- 系统通知类消息 -->
            <div v-if="msg.msgType === 'SYSTEM'" class="system-msg-row">
              <div class="system-bubble">
                <span class="sys-icon">📣</span>
                <span>{{ msg.content }}</span>
              </div>
            </div>

            <!-- 普通发言或快捷战术短语 -->
            <div 
              v-else 
              class="chat-msg-row" 
              :class="{ 'msg-self': isMyMessage(msg), 'msg-tactic': msg.msgType === 'TACTIC' }"
            >
              <div class="msg-avatar-col">
                <el-avatar :size="34" :src="msg.avatar || defaultAvatar" />
                <span v-if="msg.isCreator === 1" class="creator-crown" title="发起人">👑</span>
              </div>

              <div class="msg-body-col">
                <div class="msg-sender-meta">
                  <span class="sender-name">{{ msg.nickname || msg.username }}</span>
                  <span v-if="msg.isCreator === 1" class="sender-role-tag">发起人</span>
                  <span class="msg-time">{{ formatMsgTime(msg.createTime) }}</span>
                </div>

                <!-- 快捷战术消息特殊气泡 -->
                <div v-if="msg.msgType === 'TACTIC'" class="tactic-bubble">
                  <div class="tactic-badge-tag">战术与装备同步</div>
                  <div class="tactic-text">{{ msg.content }}</div>
                </div>

                <!-- 常规文本消息气泡 -->
                <div v-else class="text-bubble">
                  {{ msg.content }}
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 快捷战术/装备短语胶囊横滑栏 -->
      <div class="tactics-capsules-bar">
        <div class="tactic-label">快捷同步：</div>
        <div class="capsules-scroll">
          <button 
            v-for="(tactic, idx) in quickTactics" 
            :key="idx" 
            class="tactic-pill"
            :disabled="sending"
            @click="sendQuickTactic(tactic)"
          >
            {{ tactic }}
          </button>
        </div>
      </div>

      <!-- 底部输入与发送控制面板 -->
      <div class="chat-input-panel">
        <el-input
          v-model="inputContent"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="200"
          show-word-limit
          placeholder="和球友商定带球、站位或集合地点... (Enter 发送)"
          :disabled="activity?.status === 3"
          @keydown.enter.exact.prevent="handleSendText"
        />
        <div class="input-actions-bar">
          <span class="hotkey-tip">按 Enter 发送，Shift + Enter 换行</span>
          <el-button 
            type="primary" 
            size="small" 
            round
            :loading="sending"
            :disabled="!inputContent.trim() || activity?.status === 3"
            @click="handleSendText"
          >
            <el-icon><Position /></el-icon> 发送
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Location, Calendar, Position, Loading 
} from '@element-plus/icons-vue'
import { getChatHistory, sendChatMessage } from '@/api/matchChat'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const visible = ref(false)
const activity = ref(null)
const messages = ref([])
const onlineCount = ref(1)
const isConnected = ref(false)
const loadingHistory = ref(false)
const sending = ref(false)
const inputContent = ref('')
const messageContainerRef = ref(null)

let ws = null
let heartbeatTimer = null
let reconnectTimer = null

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 快捷战术与装备短语模板
const quickTactics = [
  '🏸 我带高品质比赛球',
  '📍 我已到达场馆前台',
  '⏱️ 路上稍堵，5分钟内赶到',
  '🔥 经典前后站位轮转',
  '🥤 备了冰镇电解质水',
  '💪 还有搭子在路上吗？',
  '👌 收到，球拍已就绪！'
]

const drawerTitle = computed(() => {
  return activity.value ? `搭子微聊 · ${activity.value.title}` : '拼场搭子微室'
})

// 判断是否是当前登录用户的消息
const isMyMessage = (msg) => {
  if (!userStore.userInfo) return false
  return msg.userId === userStore.userInfo.id || msg.username === userStore.userInfo.username
}

// 时间格式化
const formatMsgTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  } catch (e) {
    return timeStr
  }
}

// 自动滚动到消息区域底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

// 打开抽屉入口
const openDrawer = async (matchActivity) => {
  activity.value = matchActivity
  visible.value = true
  messages.value = []
  inputContent.value = ''
  
  await fetchHistory()
  connectWebSocket()
}

// 获取历史聊天记录
const fetchHistory = async () => {
  if (!activity.value?.id) return
  loadingHistory.value = true
  try {
    const res = await getChatHistory(activity.value.id)
    if (Array.isArray(res)) {
      messages.value = res
      scrollToBottom()
    } else if (res?.code === 200 && Array.isArray(res.data)) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch (err) {
    console.error('拉取微室记录失败:', err)
  } finally {
    loadingHistory.value = false
  }
}

// 建立 WebSocket 长连接
const connectWebSocket = () => {
  closeWebSocket()
  if (!activity.value?.id) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 后端端口通常与前端代理一致或直连后端
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/ws/match-chat?activityId=${activity.value.id}`

  try {
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      isConnected.value = true
      startHeartbeat()
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.eventType === 'ROOM_ONLINE_COUNT' || data.type === 'ONLINE_COUNT') {
          onlineCount.value = data.onlineCount || data.count || 1
        } else if (data.id && data.content) {
          // 后端直接广播的消息对象
          const exists = messages.value.some(m => m.id && m.id === data.id)
          if (!exists) {
            messages.value.push(data)
            scrollToBottom()
          }
        } else if (data.type === 'CHAT_MESSAGE' && data.payload) {
          const payload = data.payload
          const exists = messages.value.some(m => m.id && m.id === payload.id)
          if (!exists) {
            messages.value.push(payload)
            scrollToBottom()
          }
        }
      } catch (e) {
        console.warn('解析 WebSocket 消息异常:', e)
      }
    }

    ws.onerror = (e) => {
      console.warn('搭子微室 WebSocket 异常:', e)
      isConnected.value = false
    }

    ws.onclose = () => {
      isConnected.value = false
      stopHeartbeat()
    }
  } catch (err) {
    console.warn('创建 WebSocket 失败:', err)
  }
}

// 心跳保活
const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'PING' }))
    }
  }, 25000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// 断开 WebSocket
const closeWebSocket = () => {
  stopHeartbeat()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    try {
      ws.close()
    } catch (e) {}
    ws = null
  }
  isConnected.value = false
}

// 发送常规文本消息
const handleSendText = async () => {
  const text = inputContent.value.trim()
  if (!text) return
  await doSend(text, 'TEXT')
  inputContent.value = ''
}

// 发送快捷战术短语
const sendQuickTactic = async (tactic) => {
  await doSend(tactic, 'TACTIC')
}

// 统一发送函数 (REST API 落库并由后端推送到 WebSocket 广播)
const doSend = async (content, msgType) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再参与搭子微室交流')
    return
  }
  sending.value = true
  try {
    const res = await sendChatMessage({
      activityId: activity.value.id,
      content,
      msgType
    })
    const msgObj = res?.id ? res : res?.data
    if (msgObj && msgObj.id) {
      // 成功发送，检查是否已收到 WebSocket 回执，若未回执则手动追加
      const exists = messages.value.some(m => m.id === msgObj.id)
      if (!exists) {
        messages.value.push(msgObj)
        scrollToBottom()
      }
    }
  } catch (err) {
    console.error('发送消息异常:', err)
  } finally {
    sending.value = false
  }
}

// 关闭抽屉清理资源
const handleClose = (done) => {
  closeWebSocket()
  done()
}

onBeforeUnmount(() => {
  closeWebSocket()
})

defineExpose({
  openDrawer
})
</script>

<style scoped>
.match-chat-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: #f8fafc;
}

.match-chat-drawer :deep(.el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

.chat-drawer-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.header-main-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.activity-sport-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  padding: 2px 8px;
  border-radius: 9999px;
  box-shadow: 0 2px 4px rgba(79, 70, 229, 0.25);
  white-space: nowrap;
}

.header-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

.header-sub-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #64748b;
}

.conn-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.conn-status.online {
  color: #10b981;
}

.conn-status.reconnecting {
  color: #f59e0b;
}

.status-indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: currentColor;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  animation: pulse-dot 2s infinite ease-in-out;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

.venue-brief {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.chat-main-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f1f5f9;
}

/* 顶部活动快照横条 */
.match-info-ribbon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
}

.ribbon-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-weight: 500;
}

.ribbon-status-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 11px;
}

.status-locked {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-recruiting {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

/* 消息滚动区 */
.messages-scroll-area {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-loading-state,
.chat-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: #94a3b8;
  font-size: 13px;
  gap: 8px;
}

.empty-bubble-icon {
  font-size: 36px;
}

.empty-sub {
  font-size: 11px;
  color: #cbd5e1;
}

/* 消息项排版 */
.message-stream-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.system-msg-row {
  display: flex;
  justify-content: center;
  margin: 4px 0;
}

.system-bubble {
  background: rgba(241, 245, 249, 0.9);
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 90%;
  text-align: center;
}

.chat-msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  max-width: 85%;
}

.chat-msg-row.msg-self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar-col {
  position: relative;
  flex-shrink: 0;
}

.creator-crown {
  position: absolute;
  bottom: -4px;
  right: -4px;
  font-size: 12px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.msg-body-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.msg-self .msg-body-col {
  align-items: flex-end;
}

.msg-sender-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #94a3b8;
}

.sender-name {
  font-weight: 600;
  color: #475569;
}

.sender-role-tag {
  background: #fef3c7;
  color: #d97706;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 4px;
  font-weight: 700;
}

.text-bubble {
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  background: #ffffff;
  color: #1e293b;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-top-left-radius: 2px;
}

.msg-self .text-bubble {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  color: #ffffff;
  border-top-left-radius: 14px;
  border-top-right-radius: 2px;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.25);
}

/* 战术消息卡片气泡 */
.tactic-bubble {
  padding: 9px 13px;
  border-radius: 12px;
  background: #eff6ff;
  border-left: 4px solid #3b82f6;
  box-shadow: 0 2px 5px rgba(59, 130, 246, 0.1);
}

.msg-self .tactic-bubble {
  background: #eef2ff;
  border-left: none;
  border-right: 4px solid #4f46e5;
}

.tactic-badge-tag {
  font-size: 10px;
  font-weight: 700;
  color: #2563eb;
  margin-bottom: 2px;
  text-transform: uppercase;
}

.msg-self .tactic-badge-tag {
  color: #4f46e5;
  text-align: right;
}

.tactic-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

/* 快捷战术胶囊横滑栏 */
.tactics-capsules-bar {
  padding: 8px 12px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tactic-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  white-space: nowrap;
}

.capsules-scroll {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-bottom: 2px;
}

.capsules-scroll::-webkit-scrollbar {
  display: none;
}

.tactic-pill {
  flex-shrink: 0;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 9999px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tactic-pill:hover:not(:disabled) {
  background: #e0e7ff;
  border-color: #818cf8;
  color: #4338ca;
  transform: translateY(-1px);
}

/* 输入控制区 */
.chat-input-panel {
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hotkey-tip {
  font-size: 11px;
  color: #94a3b8;
}
</style>
