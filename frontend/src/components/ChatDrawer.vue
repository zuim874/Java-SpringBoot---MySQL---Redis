<template>
  <Teleport to="body">
    <transition name="drawer-fade">
      <div v-if="show" class="chat-overlay" @click.self="$emit('close')">
        <transition name="drawer-slide">
          <div class="chat-drawer">
            <!-- 头部 -->
            <div class="chat-header">
              <div class="chat-header-info">
                <span class="chat-avatar" v-if="sellerAvatar">
                  <img :src="sellerAvatar" alt="卖家头像" @error="$event.target.style.display='none'">
                </span>
                <span class="chat-avatar chat-avatar--text" v-else>{{ sellerInitial }}</span>
                <div class="chat-header-text">
                  <h3 class="chat-title">{{ sellerName }}</h3>
                  <p class="chat-sub">在线客服 · 会话</p>
                </div>
              </div>
              <button class="chat-close" @click="$emit('close')">✕</button>
            </div>

            <!-- 消息列表 -->
            <div class="chat-body" ref="bodyRef">
              <div v-if="loading" class="chat-loading">
                <div class="loading-spinner"></div>
                <p>加载消息中...</p>
              </div>
              <div v-else-if="messages.length === 0" class="chat-empty">
                <span class="chat-empty-icon">💬</span>
                <p>还没有消息，打个招呼开始沟通吧</p>
              </div>
              <div v-else class="chat-msgs">
                <div v-for="m in messages" :key="m.id"
                     class="chat-msg" :class="{ 'chat-msg--mine': isMine(m) }">
                  <div class="chat-bubble">{{ m.content }}</div>
                  <span class="chat-time">{{ formatTime(m.createTime) }}</span>
                </div>
              </div>
            </div>

            <!-- 输入区 -->
            <div class="chat-input-bar">
              <input v-model="draft" type="text" maxlength="500"
                     placeholder="输入消息，Enter 发送"
                     @keyup.enter="send" />
              <button class="chat-send-btn" :disabled="sending || !draft.trim()" @click="send">
                <span v-if="sending" class="btn-loading"></span>
                <span v-else>发送</span>
              </button>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  userOpenChat,
  sendUserMessage as apiSend,
  getUserMessages as apiMessages,
  markUserConversationRead
} from '../api/index.js'
import { toastError } from '../utils/toast.js'

const props = defineProps({
  show: { type: Boolean, default: false },
  sellerId: { type: [Number, String], required: true },
  sellerName: { type: String, default: '' },
  sellerAvatar: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const conversationId = ref(null)
const lastMsgId = ref(0)
const bodyRef = ref(null)

const sellerInitial = computed(() =>
  props.sellerName ? props.sellerName.charAt(0).toUpperCase() : '?'
)

// 判断消息是否为本买家发送
function isMine(m) {
  return m.senderRole === 'USER'
}

function formatTime(t) {
  if (!t) return ''
  try {
    const d = new Date(t)
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

// 打开会话：获取/创建会话并加载历史消息
async function openConversation() {
  if (!props.sellerId) return
  loading.value = true
  conversationId.value = null
  lastMsgId.value = 0
  try {
    const res = await userOpenChat(props.sellerId)
    if (res && res.code === 200 && res.data) {
      conversationId.value = res.data.id
      const mres = await apiMessages(res.data.id)
      if (mres && mres.code === 200) {
        messages.value = mres.data || []
        if (messages.value.length > 0) {
          lastMsgId.value = messages.value[messages.value.length - 1].id
        }
      }
      // 标记已读
      await markUserConversationRead(res.data.id)
    }
  } catch (e) {
    toastError('会话加载失败，请稍后重试')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 增量轮询新消息
async function poll() {
  if (!conversationId.value) return
  try {
    const res = await apiMessages(conversationId.value, lastMsgId.value || undefined)
    if (res && res.code === 200 && res.data && res.data.length > 0) {
      messages.value.push(...res.data)
      lastMsgId.value = res.data[res.data.length - 1].id
      await markUserConversationRead(conversationId.value)
      scrollToBottom()
    }
  } catch {
    // 轮询失败静默，等待下一次
  }
}

// 发送消息
async function send() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    const res = await apiSend(props.sellerId, content)
    if (res && res.code === 200) {
      draft.value = ''
      // 若首次发送，此时已建立会话
      if (res.data && res.data.conversationId) {
        conversationId.value = res.data.conversationId
      }
      messages.value.push(res.data)
      lastMsgId.value = res.data.id
      scrollToBottom()
    } else {
      toastError((res && res.mes) || '发送失败，请稍后重试')
    }
  } catch {
    toastError('发送失败，网络异常')
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

let timer = null
watch(() => props.show, (v) => {
  if (v) {
    openConversation()
    timer = setInterval(poll, 3000)
  } else {
    if (timer) { clearInterval(timer); timer = null }
  }
})

onUnmounted(() => {
  if (timer) { clearInterval(timer); timer = null }
})
</script>

<style scoped>
/* ===== 遮罩 ===== */
.chat-overlay {
  position: fixed; inset: 0; z-index: 900;
  background: rgba(0, 0, 0, 0.25); backdrop-filter: blur(2px);
  display: flex; justify-content: flex-end;
}
.drawer-fade-enter-active, .drawer-fade-leave-active { transition: opacity 0.25s; }
.drawer-fade-enter-from, .drawer-fade-leave-to { opacity: 0; }

/* ===== 抽屉本体（毛玻璃 + 蓝紫渐变，NebulaTech 风格） ===== */
.chat-drawer {
  width: 400px; max-width: 92vw; height: 100%;
  display: flex; flex-direction: column;
  background: linear-gradient(160deg, rgba(248, 249, 255, 0.98), rgba(255, 255, 255, 0.97));
  backdrop-filter: blur(24px) saturate(1.4);
  box-shadow: -20px 0 60px rgba(0, 0, 0, 0.12);
  border-left: 1px solid rgba(255, 255, 255, 0.6);
  font-family: 'DM Sans', -apple-system, sans-serif;
}
.drawer-slide-enter-active, .drawer-slide-leave-active { transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94); }
.drawer-slide-enter-from, .drawer-slide-leave-to { transform: translateX(100%); }

/* ===== 头部 ===== */
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px;
  background: linear-gradient(135deg, rgba(43, 108, 176, 0.06), rgba(124, 58, 237, 0.06));
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}
.chat-header-info { display: flex; align-items: center; gap: 12px; }
.chat-avatar {
  width: 42px; height: 42px; border-radius: 50%;
  overflow: hidden; background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  display: flex; align-items: center; justify-content: center;
}
.chat-avatar img { width: 100%; height: 100%; object-fit: cover; }
.chat-avatar--text { color: #fff; font-weight: 700; font-size: 1.1rem; }
.chat-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.05rem; font-weight: 700; color: #212529; margin: 0;
}
.chat-sub { font-size: 0.75rem; color: #868e96; margin: 2px 0 0; }
.chat-close {
  border: none; background: transparent; font-size: 1.2rem;
  color: #adb5bd; cursor: pointer; padding: 6px; transition: color 0.2s;
}
.chat-close:hover { color: #212529; }

/* ===== 消息区 ===== */
.chat-body { flex: 1; overflow-y: auto; padding: 18px 20px; }
.chat-loading, .chat-empty {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #adb5bd; font-size: 0.85rem; gap: 12px;
}
.chat-empty-icon { font-size: 2.4rem; }
.loading-spinner {
  width: 30px; height: 30px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.chat-msgs { display: flex; flex-direction: column; gap: 14px; }
.chat-msg { display: flex; flex-direction: column; max-width: 82%; }
.chat-msg--mine { align-self: flex-end; align-items: flex-end; }
.chat-bubble {
  padding: 10px 14px; border-radius: 14px;
  font-size: 0.88rem; line-height: 1.5; color: #212529;
  background: #fff; border: 1px solid #e9ecef;
  border-top-left-radius: 4px; word-break: break-word;
}
.chat-msg--mine .chat-bubble {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  color: #fff; border: none; border-top-right-radius: 4px;
  border-top-left-radius: 14px;
}
.chat-time { font-size: 0.68rem; color: #adb5bd; margin-top: 4px; }

/* ===== 输入区 ===== */
.chat-input-bar {
  display: flex; gap: 10px; padding: 14px 16px;
  background: rgba(255, 255, 255, 0.85);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
.chat-input-bar input {
  flex: 1; padding: 11px 14px; border: 1px solid #dee2e6; border-radius: 10px;
  font-size: 0.88rem; font-family: 'DM Sans', sans-serif; outline: none;
  transition: border-color 0.2s;
}
.chat-input-bar input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.1); }
.chat-send-btn {
  padding: 11px 22px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: #fff; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s; display: flex; align-items: center; justify-content: center;
  min-width: 64px;
}
.chat-send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43, 108, 176, 0.3); }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-loading {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
