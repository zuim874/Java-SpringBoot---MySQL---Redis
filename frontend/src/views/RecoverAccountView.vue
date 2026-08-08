<template>
  <div class="recover-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-slogan">恢复账号</div>
    </nav>

    <!-- 恢复卡片 -->
    <div class="recover-wrapper">
      <div class="recover-card">
        <div class="recover-header">
          <div class="recover-icon">🔄</div>
          <h2 class="recover-title">恢复账号</h2>
          <p class="recover-desc">验证身份后即可恢复您的账号</p>
        </div>

        <form @submit.prevent="handleRecover" class="recover-form">
          <div class="form-group">
            <label>用户名</label>
            <div class="input-wrap">
              <span class="input-icon">👤</span>
              <input
                v-model="username"
                type="text"
                placeholder="请输入用户名"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label>邮箱</label>
            <div class="input-wrap">
              <span class="input-icon">📧</span>
              <input
                v-model="email"
                type="email"
                placeholder="请输入注册邮箱"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label>验证码</label>
            <div class="input-wrap code-wrap">
              <span class="input-icon">🔑</span>
              <input
                v-model="code"
                type="text"
                placeholder="请输入验证码"
                required
              />
              <button
                type="button"
                class="send-code-btn"
                :disabled="codeSending || codeCountdown > 0 || !isEmailValid"
                @click="sendCode"
              >
                <span v-if="codeSending" class="btn-loading-small"></span>
                <span v-else-if="codeCountdown > 0">{{ codeCountdown }}s</span>
                <span v-else>发送验证码</span>
              </button>
            </div>
          </div>

          <button type="submit" class="recover-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>恢复账号</span>
          </button>
        </form>

        <div class="login-link">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>

        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="recover-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'

const username = ref('')
const email = ref('')
const code = ref('')
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
const message = ref('')
const msgSuccess = ref(false)
const router = useRouter()

let countdownTimer = null

const isEmailValid = computed(() => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(email.value)
})

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendCode() {
  if (!isEmailValid.value) return
  codeSending.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', email.value)
    const data = await request('/user/send-recovercode', {
      method: 'POST',
      body: params
    })
    if (data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '验证码已发送'
      startCountdown()
    } else {
      msgSuccess.value = false
      message.value = data.mes || '发送验证码失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    codeSending.value = false
  }
}

async function handleRecover() {
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('username', username.value)
    params.append('email', email.value)
    params.append('code', code.value)
    const data = await request('/user/recover_user', {
      method: 'PUT',
      body: params
    })
    if (data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '账号恢复成功'
      setTimeout(() => { router.push('/login') }, 1500)
    } else {
      msgSuccess.value = false
      message.value = data.mes || '恢复失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 全局 ===== */
.recover-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif;
  position: relative;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
}

/* ===== 背景装饰 ===== */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}
.bg-circle--1 {
  width: 600px; height: 600px;
  top: -200px; right: -200px;
  background: radial-gradient(circle, #4a9eff, #2b6cb0);
}
.bg-circle--2 {
  width: 500px; height: 500px;
  bottom: -150px; left: -150px;
  background: radial-gradient(circle, #7c3aed, #5b21b6);
}
.bg-circle--3 {
  width: 300px; height: 300px;
  top: 40%; left: 10%;
  background: radial-gradient(circle, #4a9eff, transparent);
}

/* ===== 导航 ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
  z-index: 100;
}
.nav-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.4rem;
  font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
}
.nav-slogan {
  font-size: 0.75rem;
  color: #adb5bd;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* ===== 恢复卡片 ===== */
.recover-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 100px 24px 60px;
  position: relative;
  z-index: 1;
}
.recover-card {
  width: 420px;
  max-width: 100%;
  padding: 48px 40px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.50);
  box-shadow:
    0 4px 24px rgba(0,0,0,0.04),
    0 20px 60px rgba(0,0,0,0.06),
    inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 卡片头部 ===== */
.recover-header {
  text-align: center;
  margin-bottom: 36px;
}
.recover-icon {
  font-size: 2rem;
  margin-bottom: 12px;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 1; }
}
.recover-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}
.recover-desc {
  font-size: 0.9rem;
  color: #868e96;
}

/* ===== 表单 ===== */
.recover-form { margin-bottom: 16px; }
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}
.input-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border: 1px solid #dee2e6;
  border-radius: 12px;
  background: rgba(255,255,255,0.60);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.input-wrap:focus-within {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.12);
  background: white;
}
.input-icon {
  font-size: 1rem;
  opacity: 0.5;
}
.input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px 0;
  font-size: 0.9rem;
  font-family: 'DM Sans', sans-serif;
  color: #212529;
  outline: none;
}
.input-wrap input::placeholder {
  color: #adb5bd;
  font-weight: 400;
}

/* ===== 验证码输入框 ===== */
.code-wrap {
  padding-right: 4px;
}
.send-code-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 90px;
}
.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(43,108,176,0.30);
}
.send-code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-loading-small {
  display: inline-block;
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.30);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ===== 按钮 ===== */
.recover-btn {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.9rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}
.recover-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(43,108,176,0.30);
}
.recover-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-loading {
  display: inline-block;
  width: 20px; height: 20px;
  border: 2px solid rgba(255,255,255,0.30);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.login-link {
  text-align: center;
  margin-top: 16px;
  font-size: 0.85rem;
  color: #868e96;
}
.login-link a {
  color: #2b6cb0;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.3s;
}
.login-link a:hover { color: #4a9eff; }

/* ===== 消息提示 ===== */
.msg {
  margin-top: 16px;
  text-align: center;
  font-size: 0.85rem;
  padding: 10px 16px;
  border-radius: 10px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success {
  color: #2b8a3e;
  background: rgba(43,138,62,0.06);
  border: 1px solid rgba(43,138,62,0.12);
}
.msg--error {
  color: #c92a2a;
  background: rgba(201,42,42,0.06);
  border: 1px solid rgba(201,42,42,0.12);
}

/* ===== 底部 ===== */
.recover-footer {
  position: relative;
  z-index: 1;
  padding: 24px 56px;
  text-align: center;
}
.recover-footer span {
  font-size: 0.7rem;
  color: #adb5bd;
  letter-spacing: 0.02em;
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .nav { padding: 0 24px; }
  .nav-slogan { display: none; }
  .recover-card { padding: 36px 24px; }
}
</style>