<template>
  <div class="register-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo">XuWenYeTech</div>
      <div class="nav-slogan">定义数字未来</div>
    </nav>

    <!-- 注册卡片 -->
    <div class="register-wrapper">
      <div class="register-card">
        <div class="register-header">
          <div class="register-icon">✦</div>
          <h2 class="register-title">创建账号</h2>
          <p class="register-desc">注册以加入企业管理系统</p>
        </div>

        <form @submit.prevent="handleRegister" class="register-form">
          <div class="form-group">
            <label>用户名</label>
            <div class="input-wrap">
              <span class="input-icon">👤</span>
              <input
                v-model="username"
                type="text"
                placeholder="2-20个字符"
                required
              />
            </div>
          </div>
          <div class="form-group">
            <label>昵称</label>
            <div class="input-wrap">
              <span class="input-icon">✨</span>
              <input
                v-model="nickname"
                type="text"
                placeholder="您的显示名称"
                required
              />
            </div>
          </div>

          <!-- 新增邮箱输入 -->
          <div class="form-group">
            <label>邮箱</label>
            <div class="input-wrap">
              <span class="input-icon">📧</span>
              <input
                  v-model="email"
                  type="email"
                  placeholder="请输入邮箱地址"
                  required
              />
            </div>
          </div>

          <!-- 新增邮箱验证码 -->
          <div class="form-group">
            <label>邮箱验证码</label>
            <div class="input-wrap verification-wrap">
              <span class="input-icon">✉️</span>
              <input
                  v-model="verificationCode"
                  type="text"
                  placeholder="请输入6位验证码"
                  maxlength="6"
                  required
              />
              <button
                  type="button"
                  class="send-code-btn"
                  @click="sendVerificationCode"
                  :disabled="codeCountdown > 0 || !isEmailValid"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码' }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>密码</label>
            <div class="input-wrap">
              <span class="input-icon">🔒</span>
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="至少6位密码"
                required
              />
              <span class="toggle-pwd" @click.stop="showPassword = !showPassword">
                {{ showPassword ? '🙈' : '👁️' }}
              </span>
            </div>
          <!-- 密码强度指示条 -->
          <div v-if="password.length > 0" class="strength-wrap">
            <div class="strength-bar">
              <div
                class="strength-fill"
                :class="'fill--' + passwordStrength.level"
                :style="{ width: passwordStrength.score === 0 ? '8%' : (passwordStrength.score / 9 * 100) + '%' }"
              ></div>
            </div>
            <span class="strength-text" :class="'text--' + passwordStrength.level">
              {{ passwordStrength.message }}
            </span>
          </div>
        </div>
        <div class="form-group">
          <label>确认密码</label>
            <div class="input-wrap">
              <span class="input-icon">🔐</span>
              <input
                v-model="passwordExam"
                :type="showPasswordExam ? 'text' : 'password'"
                placeholder="再次输入密码"
                required
              />
              <span class="toggle-pwd" @click.stop="showPasswordExam = !showPasswordExam">
                {{ showPasswordExam ? '🙈' : '👁️' }}
              </span>
            </div>
          <p v-if="passwordExam.length > 0" class="match-hint" :class="passwordMatch.valid ? 'hint--ok' : 'hint--err'">
            {{ passwordMatch.msg }}
          </p>
          </div>

          <button type="submit" class="register-btn" :disabled="!canSubmit">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>注册账号</span>
          </button>
        </form>

        <div class="login-link">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>

        <p v-if="message" :class="['msg', success ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="register-footer">
      <span>© 2026 XuWenYeTech. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'

const router = useRouter()
const username = ref('')
const nickname = ref('')
const email = ref('')
const verificationCode = ref('')
const password = ref('')
const passwordExam = ref('')
const loading = ref(false)
const message = ref('')
const success = ref(false)
const showPassword = ref(false)
const showPasswordExam = ref(false)
const codeCountdown = ref(0)
let countdownTimer = null

// ==================== 邮箱验证 ====================
const isEmailValid = computed(() => {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return emailRegex.test(email.value)
})

// ==================== 密码强度校验 ====================
const passwordStrength = computed(() => {
  const pwd = password.value
  if (!pwd) return { level: '', score: 0, valid: false, message: '' }

  const length = pwd.length
  const hasUpper = /[A-Z]/.test(pwd)
  const hasLower = /[a-z]/.test(pwd)
  const hasDigit = /[0-9]/.test(pwd)
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)

  let score = 0
  // 长度评分（与后端 PasswordStrengthUtils 一致）
  if (length >= 12) score += 3
  else if (length >= 10) score += 2
  else if (length >= 8) score += 1

  // 字符类型评分
  if (hasUpper) score += 1
  if (hasLower) score += 1
  if (hasDigit) score += 1
  if (hasSpecial) score += 2

  // 判定等级（与后端一致）
  if (length < 8) return { level: 'WEAK', score: 0, valid: false, message: '密码强度：弱（长度至少8位）' }
  if (score <= 4) return { level: 'WEAK', score, valid: false, message: '密码强度：弱（建议包含大小写字母、数字和特殊字符）' }
  if (score <= 6) return { level: 'MEDIUM', score, valid: true, message: '密码强度：中' }
  if (score <= 8) return { level: 'STRONG', score, valid: true, message: '密码强度：强' }
  return { level: 'VERY_STRONG', score: 9, valid: true, message: '密码强度：非常强' }
})

// 密码一致性
const passwordMatch = computed(() => {
  if (!passwordExam.value || !password.value) return { valid: false, msg: '' }
  const match = password.value === passwordExam.value
  return { valid: match, msg: match ? '✓ 密码一致' : '✗ 密码不一致' }
})

// 是否可以提交
const canSubmit = computed(() => {
  return passwordStrength.value.valid &&
      passwordMatch.value.valid &&
      verificationCode.value.length === 6 &&
      isEmailValid.value &&
      !loading.value
})

// ==================== 发送验证码 ====================
async function sendVerificationCode() {
  if (!isEmailValid.value) {
    message.value = '请输入正确的邮箱地址'
    success.value = false
    return
  }

  try {
    loading.value = true
    message.value = ''

    const params = new URLSearchParams()
    params.append('email', email.value.trim())

    const data = await request('/auth/send-rigistercode', {
      method: 'POST',
      body: params
    })

    if (data.code === 200) {
      success.value = true
      message.value = '验证码已发送到您的邮箱'
      // 开始倒计时
      codeCountdown.value = 60
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      success.value = false
      message.value = data.mes || '发送验证码失败'
    }
  } catch (err) {
    success.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}

// ==================== 注册 ====================
async function handleRegister() {
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('username', username.value.trim())
    params.append('password', password.value)
    params.append('password_exam', passwordExam.value)
    params.append('nickname', nickname.value.trim())
    params.append('email', email.value.trim())  // 新增
    params.append('code', verificationCode.value)  // 新增验证码

    const data = await request('/auth/register', {
      method: 'POST',
      body: params
    })
    if (data.code === 200) {
      success.value = true
      message.value = '注册成功，即将跳转登录...'
      setTimeout(() => { router.push('/login') }, 1500)
    } else {
      success.value = false
      message.value = data.mes || '注册失败'
    }
  } catch (err) {
    success.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.verification-wrap {
  padding-right: 4px;
}
.send-code-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #4a9eff, #7c3aed);
  color: white;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}
.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 158, 255, 0.3);
}
.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-pwd {
  cursor: pointer;
  font-size: 1rem;
  opacity: 0.5;
  transition: opacity 0.3s;
  user-select: none;
  padding: 4px;
}
.toggle-pwd:hover {
  opacity: 1;
}

.register-page {
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

.register-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 100px 24px 60px;
  position: relative;
  z-index: 1;
}
.register-card {
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

.register-header {
  text-align: center;
  margin-bottom: 36px;
}
.register-icon {
  font-size: 2rem;
  margin-bottom: 12px;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 1; }
}
.register-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}
.register-desc {
  font-size: 0.9rem;
  color: #868e96;
}

.register-form { margin-bottom: 16px; }
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

.register-btn {
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
.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(43,108,176,0.30);
}
.register-btn:disabled {
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
  margin-top: 20px;
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

.register-footer {
  position: relative;
  z-index: 1;
  padding: 24px 56px;
  text-align: center;
}
.register-footer span {
  font-size: 0.7rem;
  color: #adb5bd;
  letter-spacing: 0.02em;
}

/* ==================== 密码强度指示条 ==================== */
.strength-wrap {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.strength-bar {
  flex: 1;
  height: 4px;
  background: #e9ecef;
  border-radius: 2px;
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94), background 0.4s ease;
}
.fill--WEAK { background: #e03131; }
.fill--MEDIUM { background: #f08c00; }
.fill--STRONG { background: #2f9e44; }
.fill--VERY_STRONG { background: #0c8599; }
.strength-text {
  font-size: 0.75rem;
  white-space: nowrap;
  transition: color 0.3s ease;
}
.text--WEAK { color: #e03131; }
.text--MEDIUM { color: #f08c00; }
.text--STRONG { color: #2f9e44; }
.text--VERY_STRONG { color: #0c8599; }

/* ==================== 密码一致性提示 ==================== */
.match-hint {
  margin-top: 8px;
  font-size: 0.75rem;
  animation: fadeIn 0.3s ease;
}
.hint--ok { color: #2f9e44; }
.hint--err { color: #e03131; }

@media (max-width: 480px) {
  .nav { padding: 0 24px; }
  .nav-slogan { display: none; }
  .register-card { padding: 36px 24px; }
}
</style>