<template>
  <div class="auth-page">
    <!-- ===== 左侧品牌展示区（电商氛围） ===== -->
    <aside class="auth-aside">
      <div class="aside-decor" aria-hidden="true">
        <span class="decor-circle c1"></span>
        <span class="decor-circle c2"></span>
        <span class="decor-circle c3"></span>
        <span class="decor-ring"></span>
      </div>

      <div class="aside-content">
        <div class="aside-logo">ZuiM<span>Shop</span></div>
        <h1 class="aside-title">账号安全，<br />由我们守护</h1>
        <p class="aside-sub">通过注册邮箱验证身份，找回你的购物账号<br />正品严选 · 极速发货 · 七天无理由退换</p>

        <!-- 安全提示卡（纯装饰） -->
        <div class="aside-tips" aria-hidden="true">
          <div class="tip-card tc-1">
            <span class="tc-icon">🔑</span>
            <div class="tc-body">
              <p class="tc-title">邮箱验证</p>
              <p class="tc-sub">仅需 3 步即可完成</p>
            </div>
          </div>
          <div class="tip-card tc-2">
            <span class="tc-icon">🔒</span>
            <div class="tc-body">
              <p class="tc-title">信息加密</p>
              <p class="tc-sub">全程加密传输</p>
            </div>
          </div>
          <div class="tip-card tc-3">
            <span class="tc-icon">🛡️</span>
            <div class="tc-body">
              <p class="tc-title">安全护航</p>
              <p class="tc-sub">保护你的购物资产</p>
            </div>
          </div>
        </div>

        <!-- 服务保障 -->
        <ul class="aside-benefits">
          <li><span class="benefit-dot">🛡️</span>正品保障 · 假一赔十</li>
          <li><span class="benefit-dot">🚚</span>满 ¥99 免运费 · 极速发货</li>
          <li><span class="benefit-dot">↩️</span>七天无理由退换货</li>
        </ul>
      </div>

      <p class="aside-copyright">© 2026 ZuiMShop 优选商城</p>
    </aside>

    <!-- ===== 右侧表单区 ===== -->
    <main class="auth-main">
      <div class="auth-mobile-logo">ZuiM<span>Shop</span></div>

      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-eyebrow">ACCOUNT RECOVERY</span>
          <h2 class="auth-title">找回账号</h2>
          <p class="auth-desc">通过注册邮箱验证身份，安全找回你的账号</p>
        </div>

        <form @submit.prevent="handleRecover" class="auth-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <div class="input-wrap">
              <span class="input-icon">👤</span>
              <input id="username" v-model="username" type="text" placeholder="请输入用户名" autocomplete="username" required />
            </div>
          </div>

          <div class="form-group">
            <label for="email">注册邮箱</label>
            <div class="input-wrap">
              <span class="input-icon">📧</span>
              <input id="email" v-model="email" type="email" placeholder="请输入注册邮箱" autocomplete="email" required />
            </div>
          </div>

          <div class="form-group">
            <label for="code">验证码</label>
            <div class="input-wrap code-wrap">
              <span class="input-icon">🔑</span>
              <input id="code" v-model="code" type="text" placeholder="请输入验证码" maxlength="6" required />
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

          <button type="submit" class="auth-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading"></span>
            <template v-else>找回账号</template>
          </button>
        </form>

        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>

        <div class="auth-switch">
          <span>已想起密码？</span>
          <router-link to="/login">立即登录 →</router-link>
        </div>
      </div>

      <p class="auth-mobile-copyright">© 2026 ZuiMShop 优选商城</p>
    </main>
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
    params.append('username', username.value)
    const data = await request('/user/send-recovercode', {
      method: 'POST',
      body: params
    })
    if (data && data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '验证码已发送'
      startCountdown()
    } else {
      msgSuccess.value = false
      message.value = data.mes || '发送验证码失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '服务连接失败，请稍后重试'
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
    if (data && data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '账号恢复成功'
      setTimeout(() => { router.push('/login') }, 1500)
    } else {
      msgSuccess.value = false
      message.value = data.mes || '恢复失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '服务连接失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ============ 整体布局 ============ */
.auth-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg, #f2f5f9);
  font-family: var(--font-sans, 'DM Sans', sans-serif);
  -webkit-font-smoothing: antialiased;
}

/* ============ 左侧品牌区 ============ */
.auth-aside {
  position: relative;
  flex: 0 0 46%;
  max-width: 46%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  color: #fff;
  overflow: hidden;
  background:
    radial-gradient(1100px 520px at -10% -20%, rgba(19, 194, 194, 0.38), transparent 62%),
    radial-gradient(820px 640px at 112% 18%, rgba(47, 84, 235, 0.55), transparent 56%),
    linear-gradient(160deg, #0e1b3f 0%, #12295f 46%, #0e7490 100%);
}
.aside-decor { position: absolute; inset: 0; pointer-events: none; }
.decor-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.10);
}
.decor-circle.c1 { width: 420px; height: 420px; top: -140px; right: -120px; }
.decor-circle.c2 { width: 280px; height: 280px; bottom: 60px; left: -120px; border-color: rgba(19, 194, 194, 0.25); }
.decor-circle.c3 { width: 160px; height: 160px; top: 42%; right: 12%; border-color: rgba(255, 255, 255, 0.14); }
.decor-ring {
  position: absolute;
  top: 20%; right: 6%;
  width: 240px; height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.02) 70%);
  filter: blur(2px);
}

.aside-content { position: relative; z-index: 1; }

.aside-logo {
  font-family: var(--font-display, 'Playfair Display', Georgia, serif);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  margin-bottom: 52px;
}
.aside-logo span { color: #7ef0ef; }

.aside-title {
  font-family: var(--font-display, 'Playfair Display', Georgia, serif);
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.01em;
  margin-bottom: 20px;
}
.aside-sub {
  font-size: 0.95rem;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
  margin-bottom: 40px;
}

/* 安全提示卡 */
.aside-tips { position: relative; height: 168px; margin-bottom: 32px; }
.tip-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: var(--radius-lg, 18px);
  background: rgba(255, 255, 255, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);
  animation: floatY 6s ease-in-out infinite;
}
.tip-card.tc-1 { left: 0; top: 4px; }
.tip-card.tc-2 { left: 218px; top: 64px; animation-delay: 1.2s; }
.tip-card.tc-3 { left: 46px; top: 124px; animation-delay: 2.4s; }
.tc-icon {
  display: grid;
  place-items: center;
  width: 44px; height: 44px;
  border-radius: 12px;
  font-size: 1.4rem;
  background: rgba(255, 255, 255, 0.16);
}
.tc-title { font-size: 0.84rem; color: rgba(255, 255, 255, 0.92); font-weight: 600; }
.tc-sub { font-size: 0.74rem; color: rgba(255, 255, 255, 0.62); margin-top: 2px; }
@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 服务保障 */
.aside-benefits {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 14px 28px;
}
.aside-benefits li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.86);
}
.benefit-dot { font-size: 1rem; }

.aside-copyright {
  position: relative;
  z-index: 1;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.45);
}

/* ============ 右侧表单区 ============ */
.auth-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background:
    radial-gradient(600px 400px at 100% 0%, rgba(47, 84, 235, 0.05), transparent 60%),
    var(--bg, #f2f5f9);
}
.auth-mobile-logo {
  display: none;
  font-family: var(--font-display, 'Playfair Display', Georgia, serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary, #2f54eb);
  margin-bottom: 28px;
}
.auth-mobile-logo span { color: var(--accent, #13c2c2); }

.auth-card {
  width: 400px;
  max-width: 100%;
  padding: 44px 40px 32px;
  background: var(--surface, #fff);
  border: 1px solid var(--border, #e4e9f0);
  border-radius: var(--radius-lg, 18px);
  box-shadow: var(--shadow-lg, 0 18px 50px rgba(23, 35, 61, 0.12));
}

/* 头部 */
.auth-header { text-align: center; margin-bottom: 30px; }
.auth-eyebrow {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: var(--primary, #2f54eb);
  margin-bottom: 10px;
}
.auth-title {
  font-family: var(--font-display, 'Playfair Display', Georgia, serif);
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text, #17233d);
  margin-bottom: 8px;
}
.auth-desc { font-size: 0.88rem; color: var(--text-3, #8a94a6); }

/* 表单 */
.auth-form { margin-bottom: 6px; }
.form-group { margin-bottom: 18px; }
.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-2, #4e5a70);
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}
.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid var(--border-strong, #cdd6e1);
  border-radius: var(--radius, 12px);
  background: var(--surface-2, #f8fafc);
  transition: all 0.25s ease;
}
.input-wrap:focus-within {
  border-color: var(--primary, #2f54eb);
  box-shadow: 0 0 0 3px var(--primary-soft, rgba(47, 84, 235, 0.08));
  background: var(--surface, #fff);
}
.input-icon { font-size: 1rem; opacity: 0.55; }
.input-wrap input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 13px 0;
  font-size: 0.9rem;
  font-family: var(--font-sans, 'DM Sans', sans-serif);
  color: var(--text, #17233d);
  outline: none;
}
.input-wrap input::placeholder { color: var(--text-4, #b3bac6); }

/* 验证码按钮 */
.code-wrap { padding-right: 4px; }
.send-code-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: var(--radius-sm, 8px);
  background: linear-gradient(135deg, var(--primary, #2f54eb), var(--accent, #13c2c2));
  color: #fff;
  font-family: var(--font-sans, 'DM Sans', sans-serif);
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
}
.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-primary, 0 8px 20px rgba(47, 84, 235, 0.28));
}
.send-code-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-loading-small {
  display: inline-block;
  width: 14px; height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.30);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* 提交按钮 */
.auth-btn {
  width: 100%;
  margin-top: 8px;
  padding: 15px;
  border: none;
  border-radius: var(--radius, 12px);
  background: linear-gradient(135deg, var(--primary, #2f54eb) 0%, var(--accent, #13c2c2) 130%);
  color: #fff;
  font-family: var(--font-sans, 'DM Sans', sans-serif);
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-indent: 0.14em;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}
.auth-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(255, 255, 255, 0.28) 50%, transparent 60%);
  transform: translateX(-120%);
  transition: transform 0.6s ease;
}
.auth-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-primary, 0 8px 20px rgba(47, 84, 235, 0.28));
}
.auth-btn:hover:not(:disabled)::after { transform: translateX(120%); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-loading {
  display: inline-block;
  width: 20px; height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.30);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 消息提示 */
.msg {
  margin-top: 16px;
  text-align: center;
  font-size: 0.85rem;
  padding: 10px 16px;
  border-radius: var(--radius, 12px);
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success {
  color: var(--success, #2f9e44);
  background: var(--success-soft, rgba(47, 158, 68, 0.10));
  border: 1px solid rgba(47, 158, 68, 0.14);
}
.msg--error {
  color: var(--danger, #e03131);
  background: var(--danger-soft, rgba(224, 49, 49, 0.08));
  border: 1px solid rgba(224, 49, 49, 0.14);
}

/* 切换链接 */
.auth-switch {
  margin-top: 22px;
  text-align: center;
  font-size: 0.86rem;
  color: var(--text-3, #8a94a6);
}
.auth-switch a {
  color: var(--primary, #2f54eb);
  font-weight: 600;
  margin-left: 4px;
  transition: opacity 0.2s;
}
.auth-switch a:hover { opacity: 0.8; }

.auth-mobile-copyright { display: none; }

/* ============ 响应式 ============ */
@media (max-width: 960px) {
  .auth-aside { display: none; }
  .auth-main { justify-content: flex-start; padding-top: 64px; }
  .auth-mobile-logo { display: block; }
  .auth-mobile-copyright { display: block; margin-top: 24px; font-size: 0.72rem; color: var(--text-4, #b3bac6); }
}
@media (max-width: 480px) {
  .auth-card { padding: 32px 22px 24px; }
}
</style>
