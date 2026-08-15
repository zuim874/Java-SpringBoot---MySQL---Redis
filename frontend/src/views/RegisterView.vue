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
        <h1 class="aside-title">新人专享，<br />好礼拿不停</h1>
        <p class="aside-sub">注册即领新人礼券 · 会员专享价 · 每日秒杀<br />你的品质好物，从这里开始</p>

        <!-- 权益卡（纯装饰） -->
        <div class="aside-perks" aria-hidden="true">
          <div class="perk-card pk-1">
            <span class="pk-icon">🎁</span>
            <div class="pk-body">
              <p class="pk-title">新人礼券包</p>
              <p class="pk-sub">最高立省 ¥100</p>
            </div>
          </div>
          <div class="perk-card pk-2">
            <span class="pk-icon">⚡</span>
            <div class="pk-body">
              <p class="pk-title">每日秒杀</p>
              <p class="pk-sub">10:00 / 20:00 开抢</p>
            </div>
          </div>
          <div class="perk-card pk-3">
            <span class="pk-icon">👑</span>
            <div class="pk-body">
              <p class="pk-title">会员专享价</p>
              <p class="pk-sub">下单越多越省钱</p>
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

      <div class="auth-card auth-card--register">
        <div class="auth-header">
          <span class="auth-eyebrow">JOIN ZUIMSHOP</span>
          <h2 class="auth-title">创建账号</h2>
          <p class="auth-desc">注册即享新人礼券与会员专享价</p>
        </div>

        <form @submit.prevent="handleRegister" class="auth-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <div class="input-wrap">
              <span class="input-icon">👤</span>
              <input id="username" v-model="username" type="text" placeholder="2-20个字符，仅限字母和数字" autocomplete="username" required />
            </div>
          </div>

          <div class="form-group">
            <label for="nickname">昵称</label>
            <div class="input-wrap">
              <span class="input-icon">✨</span>
              <input id="nickname" v-model="nickname" type="text" placeholder="您的显示名称" required />
            </div>
          </div>

          <div class="form-group">
            <label for="email">邮箱</label>
            <div class="input-wrap">
              <span class="input-icon">📧</span>
              <input id="email" v-model="email" type="email" placeholder="请输入邮箱地址" autocomplete="email" required />
            </div>
          </div>

          <div class="form-group">
            <label for="verificationCode">邮箱验证码</label>
            <div class="input-wrap verification-wrap">
              <span class="input-icon">✉️</span>
              <input id="verificationCode" v-model="verificationCode" type="text" placeholder="请输入6位验证码" maxlength="6" required />
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
            <label for="password">密码</label>
            <div class="input-wrap">
              <span class="input-icon">🔒</span>
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="至少8位密码"
                autocomplete="new-password"
                required
              />
              <span class="toggle-pwd" role="button" tabindex="0" @click.stop="showPassword = !showPassword">
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
            <label for="passwordExam">确认密码</label>
            <div class="input-wrap">
              <span class="input-icon">🔐</span>
              <input
                id="passwordExam"
                v-model="passwordExam"
                :type="showPasswordExam ? 'text' : 'password'"
                placeholder="再次输入密码"
                autocomplete="new-password"
                required
              />
              <span class="toggle-pwd" role="button" tabindex="0" @click.stop="showPasswordExam = !showPasswordExam">
                {{ showPasswordExam ? '🙈' : '👁️' }}
              </span>
            </div>
            <p v-if="passwordExam.length > 0" class="match-hint" :class="passwordMatch.valid ? 'hint--ok' : 'hint--err'">
              {{ passwordMatch.msg }}
            </p>
          </div>

          <button type="submit" class="auth-btn" :disabled="!canSubmit">
            <span v-if="loading" class="btn-loading"></span>
            <template v-else>注 册</template>
          </button>
        </form>

        <p v-if="message" :class="['msg', success ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>

        <div class="auth-switch">
          <span>已有账号？</span>
          <router-link to="/login">立即登录 →</router-link>
        </div>

        <p class="auth-agreement">注册即代表你同意《用户协议》与《隐私政策》</p>
      </div>

      <p class="auth-mobile-copyright">© 2026 ZuiMShop 优选商城</p>
    </main>
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

    const data = await request('/auth/send-registercode', {
      method: 'POST',
      body: params
    })

    if (data && data.code === 200) {
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
    message.value = '服务连接失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ==================== 注册 ====================
async function handleRegister() {
  loading.value = true
  message.value = ''
  // 用户名格式校验：仅允许字母和数字，拒绝中文、下划线及其他特殊符号
  if (!/^[A-Za-z0-9]+$/.test(username.value.trim())) {
    message.value = '用户名只能包含字母和数字，不能包含中文、下划线及其他特殊符号'
    loading.value = false
    return
  }
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
    if (data && data.code === 200) {
      success.value = true
      message.value = '注册成功，即将跳转登录...'
      setTimeout(() => { router.push('/login') }, 1500)
    } else {
      success.value = false
      message.value = data.mes || '注册失败'
    }
  } catch (err) {
    success.value = false
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
  margin-bottom: 48px;
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
  margin-bottom: 36px;
}

/* 权益卡 */
.aside-perks { position: relative; height: 172px; margin-bottom: 32px; }
.perk-card {
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
.perk-card.pk-1 { left: 0; top: 4px; }
.perk-card.pk-2 { left: 216px; top: 66px; animation-delay: 1.2s; }
.perk-card.pk-3 { left: 44px; top: 128px; animation-delay: 2.4s; }
.pk-icon {
  display: grid;
  place-items: center;
  width: 44px; height: 44px;
  border-radius: 12px;
  font-size: 1.4rem;
  background: rgba(255, 255, 255, 0.16);
}
.pk-title { font-size: 0.84rem; color: rgba(255, 255, 255, 0.92); font-weight: 600; }
.pk-sub { font-size: 0.74rem; color: rgba(255, 255, 255, 0.62); margin-top: 2px; }
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
  padding: 40px 40px;
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
  margin-bottom: 24px;
}
.auth-mobile-logo span { color: var(--accent, #13c2c2); }

.auth-card {
  width: 400px;
  max-width: 100%;
  padding: 40px 40px 28px;
  background: var(--surface, #fff);
  border: 1px solid var(--border, #e4e9f0);
  border-radius: var(--radius-lg, 18px);
  box-shadow: var(--shadow-lg, 0 18px 50px rgba(23, 35, 61, 0.12));
}

/* 头部 */
.auth-header { text-align: center; margin-bottom: 26px; }
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
.form-group { margin-bottom: 16px; }
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
.verification-wrap { padding-right: 4px; }
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
}
.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-primary, 0 8px 20px rgba(47, 84, 235, 0.28));
}
.send-code-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* 密码可见切换 */
.toggle-pwd {
  cursor: pointer;
  font-size: 1rem;
  opacity: 0.5;
  transition: opacity 0.25s;
  user-select: none;
  padding: 4px;
}
.toggle-pwd:hover { opacity: 1; }

/* 密码强度指示条 */
.strength-wrap {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.strength-bar {
  flex: 1;
  height: 4px;
  background: var(--bg-deep, #e8edf4);
  border-radius: 2px;
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94), background 0.4s ease;
}
.fill--WEAK { background: var(--danger, #e03131); }
.fill--MEDIUM { background: var(--warning, #e67700); }
.fill--STRONG { background: var(--success, #2f9e44); }
.fill--VERY_STRONG { background: var(--accent, #13c2c2); }
.strength-text {
  font-size: 0.75rem;
  white-space: nowrap;
  transition: color 0.3s ease;
}
.text--WEAK { color: var(--danger, #e03131); }
.text--MEDIUM { color: var(--warning, #e67700); }
.text--STRONG { color: var(--success, #2f9e44); }
.text--VERY_STRONG { color: var(--accent, #13c2c2); }

/* 密码一致性提示 */
.match-hint {
  margin-top: 8px;
  font-size: 0.75rem;
  animation: fadeIn 0.3s ease;
}
.hint--ok { color: var(--success, #2f9e44); }
.hint--err { color: var(--danger, #e03131); }

/* 注册按钮 */
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
  letter-spacing: 0.28em;
  text-indent: 0.28em;
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
  margin-top: 14px;
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
  margin-top: 20px;
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

.auth-agreement {
  margin-top: 16px;
  text-align: center;
  font-size: 0.72rem;
  color: var(--text-4, #b3bac6);
}

.auth-mobile-copyright { display: none; }

/* ============ 响应式 ============ */
@media (max-width: 960px) {
  .auth-aside { display: none; }
  .auth-main { justify-content: flex-start; padding-top: 56px; }
  .auth-mobile-logo { display: block; }
  .auth-mobile-copyright { display: block; margin-top: 24px; font-size: 0.72rem; color: var(--text-4, #b3bac6); }
}
@media (max-width: 480px) {
  .auth-card { padding: 28px 20px 22px; }
}
</style>
