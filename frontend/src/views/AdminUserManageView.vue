<template>
  <div class="admin-page">
    <!-- 顶部导航 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-actions">
        <div class="avatar-wrapper" @click.stop>
          <span class="nav-balance" v-if="userBalance !== null">余额 ¥{{ Number(userBalance).toFixed(2) }}</span>
          <span class="nav-nickname">{{ nickname }}</span>
          <button class="avatar-btn" @click="profileMenuOpen = !profileMenuOpen" aria-label="用户菜单">
            <span class="avatar-circle">
              <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
            </span>
            <span class="avatar-caret" :class="{ open: profileMenuOpen }">▾</span>
          </button>
          <transition name="profile">
            <div class="profile-menu" v-if="profileMenuOpen">
              <div class="profile-menu-header">
                <span class="profile-menu-avatar">
                  <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
                </span>
                <div class="profile-menu-id">
                  <p class="profile-menu-name">{{ nickname }}</p>
                  <p class="profile-menu-role">{{ roleLabel }}</p>
                </div>
              </div>
              <button class="profile-menu-item" @click="profileMenuOpen = false; goHome()">返回商城</button>
              <button class="profile-menu-item" @click="profileMenuOpen = false; goOrders()">我的订单</button>
              <button class="profile-menu-item" @click="profileMenuOpen = false; goProfile()">个人中心</button>
              <button class="profile-menu-item" @click="profileMenuOpen = false; goCoupons()">我的优惠券</button>
              <button class="profile-menu-item" v-if="isAdminUser" @click="profileMenuOpen = false; goAdmin()">管理后台</button>
              <button class="profile-menu-item" v-if="isSellerUser" @click="profileMenuOpen = false; goSeller()">商家管理</button>
              <div class="profile-menu-divider"></div>
              <button class="profile-menu-item profile-menu-item--logout" @click="handleLogout">退出登录</button>
            </div>
          </transition>
        </div>
      </div>
    </nav>

    <!-- 主内容 -->
    <main class="admin-body">
      <!-- 页头 -->
      <header class="page-head">
        <div>
          <h1 class="page-title">用户管理</h1>
          <p class="page-sub">管理员用户管理与恢复操作 · 需权限码确认后执行</p>
        </div>
      </header>

      <!-- 管理员权限确认 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">🔑</span>
          <h3 class="panel-title">管理员权限确认</h3>
        </div>
        <div class="field-row">
          <div class="field">
            <label>管理员权限码</label>
            <input
              v-model="adminCode"
              type="password"
              placeholder="请输入管理员确认码"
              class="field-input"
            />
          </div>
          <p class="field-hint">执行删除 / 恢复 / 充值操作前需输入管理员权限码进行校验</p>
        </div>
      </section>

      <!-- 当前用户信息 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">👤</span>
          <h3 class="panel-title">当前用户信息</h3>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>昵称</th>
                <th>邮箱</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="cell-dim">{{ currentUser.id || '-' }}</td>
                <td>{{ currentUser.username || '-' }}</td>
                <td>{{ currentUser.nickname || '-' }}</td>
                <td class="cell-dim">{{ currentUser.email || '-' }}</td>
                <td>
                  <span class="status-badge" :class="currentUser.status === 1 ? 'status--ok' : 'status--off'">
                    {{ currentUser.status === 1 ? '正常' : '已删除' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 删除 / 恢复用户 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">🗑️</span>
          <h3 class="panel-title">删除 / 恢复用户</h3>
        </div>
        <div class="op-row">
          <div class="field op-field">
            <label>目标用户 ID</label>
            <input
              v-model="targetUserId"
              type="number"
              placeholder="请输入用户ID"
              class="field-input"
            />
          </div>
          <div class="op-btns">
            <button class="op-btn op-btn--delete" :disabled="loading" @click="handleDeleteUser">删除用户</button>
            <button class="op-btn op-btn--recover" :disabled="loading" @click="handleRecoverUser">恢复用户</button>
          </div>
        </div>
      </section>

      <!-- 用户余额充值 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-icon">💰</span>
          <h3 class="panel-title">用户余额充值</h3>
        </div>
        <div class="op-row">
          <div class="field op-field">
            <label>目标用户 ID</label>
            <input
              v-model="targetUserId"
              type="number"
              placeholder="请输入用户ID"
              class="field-input"
            />
          </div>
          <div class="field op-field">
            <label>充值金额</label>
            <input
              v-model="chargeAmount"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="请输入充值金额"
              class="field-input"
            />
          </div>
          <div class="op-btns">
            <button class="op-btn op-btn--charge" :disabled="loading" @click="handleCharge">确认充值</button>
          </div>
        </div>
      </section>

      <!-- 消息提示 -->
      <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">
        {{ message }}
      </p>
    </main>

    <!-- 底部版权 -->
    <footer class="page-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'
import { chargeUserBalance, getUserInfo } from '../api/index.js'
import { parseJWT } from '../utils/token'
import { getCachedAvatar } from '../utils/avatarCache.js'
import { getCachedRoles } from '../utils/auth.js'

const router = useRouter()

// ===== 用户头像下拉 =====
const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'
const avatarUrl = ref(DEFAULT_AVATAR)
const nickname = ref(localStorage.getItem('nickname') || '用户')
const userBalance = ref(null)
const profileMenuOpen = ref(false)

const roleLabel = computed(() => {
  const roles = ['管理员']
  return roles[0]
})

const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))

async function fetchUserProfile() {
  try {
    const res = await getUserInfo()
    if (res && res.code === 200 && res.data) {
      const u = res.data
      if (u.avatar) avatarUrl.value = u.avatar
      if (u.nickname) nickname.value = u.nickname
      if (u.balance !== undefined && u.balance !== null) {
        userBalance.value = Number(u.balance)
      }
    }
  } catch (e) {
    console.error('获取用户资料失败:', e)
  }
}

function closePopups() { profileMenuOpen.value = false }

const adminCode = ref('')
const targetUserId = ref('')
const chargeAmount = ref('')
const loading = ref(false)
const message = ref('')
const msgSuccess = ref(false)
const currentUser = ref({})

onMounted(() => {
  fetchUserProfile()
  document.addEventListener('click', closePopups)
  loadCurrentUser()
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})

function loadCurrentUser() {
  const token = localStorage.getItem('token')
  if (token) {
    const payload = parseJWT(token)
    if (payload) {
      currentUser.value = {
        id: payload.id || payload.userId || '',
        username: payload.sub || payload.username || '',
        nickname: localStorage.getItem('nickname') || '',
        email: payload.email || '',
        status: payload.status !== undefined ? payload.status : 1
      }
    }
  }
}

function goHome() {
  router.push('/home')
}

function goOrders() { router.push('/orders') }
function goProfile() { router.push('/profile') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

async function handleDeleteUser() {
  if (!validateInput()) return
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('id', Number(targetUserId.value))
    params.append('adminCode', adminCode.value)

    const data = await request('/user/delete_admin', {
      method: 'DELETE',
      body: params
    })
    if (data && data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '用户已删除'
    } else {
      msgSuccess.value = false
      message.value = data.mes || '删除失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '服务连接失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleRecoverUser() {
  if (!validateInput()) return
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('id', Number(targetUserId.value))
    params.append('adminCode', adminCode.value)

    const data = await request('/user/recover_admin', {
      method: 'PUT',
      body: params
    })
    if (data && data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '用户已恢复'
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

async function handleCharge() {
  if (!targetUserId.value) {
    message.value = '请先输入目标用户ID'
    msgSuccess.value = false
    return
  }
  const amount = Number(chargeAmount.value)
  if (!amount || amount <= 0) {
    message.value = '请输入有效的充值金额'
    msgSuccess.value = false
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await chargeUserBalance(targetUserId.value, amount)
    if (res && res.code === 200) {
      msgSuccess.value = true
      message.value = res.mes || '充值成功'
      chargeAmount.value = ''
    } else {
      msgSuccess.value = false
      message.value = (res && res.mes) || '充值失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '充值失败：' + ((err && err.message) || '网络错误')
  } finally {
    loading.value = false
  }
}
function validateInput() {
  if (!adminCode.value.trim()) {
    message.value = '请输入管理员权限码'
    msgSuccess.value = false
    return false
  }
  if (!targetUserId.value) {
    message.value = '请输入目标用户 ID'
    msgSuccess.value = false
    return false
  }
  return true
}
</script>

<style scoped>
/* ===== 页面骨架 ===== */
.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  font-family: var(--font-sans);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
}

/* ===== 顶部导航 ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0; height: 68px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 clamp(20px, 4vw, 48px);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border-bottom: 1px solid var(--border);
}
.nav-logo {
  font-family: var(--font-display); font-size: 1.35rem; font-weight: 700; letter-spacing: -0.02em;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.nav-actions { display: flex; align-items: center; gap: 12px; }

/* ===== 头像与下拉 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 8px; }
.nav-balance {
  padding: 5px 12px; border-radius: var(--radius-pill);
  background: var(--primary-soft); border: 1px solid rgba(47,84,235,0.15);
  color: var(--primary); font-size: 0.75rem; font-weight: 600; white-space: nowrap;
}
.nav-nickname { font-size: 0.85rem; font-weight: 600; color: var(--text-2); padding: 0 4px; }
.avatar-btn { display: flex; align-items: center; gap: 4px; background: none; border: none; cursor: pointer; padding: 2px; transition: transform 0.2s ease; }
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 38px; height: 38px; border-radius: 50%; overflow: hidden;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 1rem;
  box-shadow: var(--shadow-sm); border: 2px solid var(--surface);
}
.avatar-circle img { width: 100%; height: 100%; object-fit: cover; }
.avatar-caret { font-size: 0.7rem; color: var(--text-3); transition: transform 0.2s ease; }
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 14px); right: 0; min-width: 190px; padding: 8px;
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}
.profile-menu-header { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-bottom: 1px solid var(--border); margin-bottom: 6px; }
.profile-menu-avatar {
  width: 40px; height: 40px; border-radius: 50%; overflow: hidden; flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700;
}
.profile-menu-avatar img { width: 100%; height: 100%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name { font-size: 0.9rem; font-weight: 600; color: var(--text); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-menu-role { font-size: 0.72rem; color: var(--text-3); margin: 2px 0 0; }
.profile-menu-item {
  display: block; width: 100%; text-align: left; padding: 10px 14px; border: none; border-radius: var(--radius-sm);
  background: transparent; cursor: pointer; font-size: 0.85rem; color: var(--text-2); transition: all 0.2s ease;
}
.profile-menu-item:hover { background: var(--primary-softer); color: var(--primary); }
.profile-menu-item--logout:hover { background: var(--danger-soft); color: var(--danger); }
.profile-menu-divider { height: 1px; background: var(--border); margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主体布局 ===== */
.admin-body {
  flex: 1; width: 100%; max-width: 900px; margin: 0 auto;
  padding: 96px 24px 56px;
}
.page-head { margin-bottom: 24px; }
.page-title {
  font-family: var(--font-display); font-size: 1.7rem; font-weight: 700;
  color: var(--text); letter-spacing: -0.01em; line-height: 1.2;
}
.page-sub { margin-top: 6px; font-size: 0.9rem; color: var(--text-3); }

/* ===== 内容面板 ===== */
.panel {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
  padding: 24px; margin-bottom: 20px;
  animation: panelIn 0.35s ease both;
}
@keyframes panelIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.panel-head {
  display: flex; align-items: center; gap: 12px; margin-bottom: 18px;
}
.panel-icon {
  width: 38px; height: 38px; border-radius: var(--radius); flex-shrink: 0;
  background: var(--primary-soft);
  display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
}
.panel-title {
  font-family: var(--font-display); font-size: 1.1rem; font-weight: 700;
  color: var(--text); margin: 0;
}

/* ===== 表单字段 ===== */
.field-row { display: flex; flex-direction: column; gap: 10px; }
.field { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.field label { font-size: 0.78rem; font-weight: 600; color: var(--text-3); letter-spacing: 0.02em; }
.field-input {
  width: 100%; padding: 11px 14px;
  background: var(--surface-2); border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); color: var(--text); font-size: 0.9rem;
  outline: none; transition: all 0.2s ease; font-family: var(--font-sans);
}
.field-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); background: var(--surface); }
.field-input::placeholder { color: var(--text-4); }
.field-input::-webkit-inner-spin-button,
.field-input::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.field-input { -moz-appearance: textfield; }
.field-hint { font-size: 0.78rem; color: var(--text-4); }

/* ===== 操作行 ===== */
.op-row {
  display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap;
}
.op-field { min-width: 180px; }
.op-btns { display: flex; gap: 10px; padding-bottom: 2px; flex-wrap: wrap; }
.op-btn {
  padding: 11px 24px; border: none; border-radius: var(--radius-sm);
  font-size: 0.85rem; font-weight: 600; color: #fff; cursor: pointer;
  transition: all 0.2s ease; white-space: nowrap;
}
.op-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.op-btn--delete { background: linear-gradient(135deg, var(--danger), #f03e3e); }
.op-btn--delete:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 20px var(--danger-soft); }
.op-btn--recover { background: linear-gradient(135deg, var(--success), #37b24d); }
.op-btn--recover:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 20px var(--success-soft); }
.op-btn--charge { background: linear-gradient(135deg, var(--primary), var(--primary-2)); }
.op-btn--charge:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }

/* ===== 表格 ===== */
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius); }
.data-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; min-width: 560px; }
.data-table thead th {
  text-align: left; padding: 12px 16px; background: var(--surface-3);
  color: var(--text-2); font-weight: 600; font-size: 0.74rem;
  letter-spacing: 0.04em; white-space: nowrap; border-bottom: 1px solid var(--border);
}
.data-table tbody td {
  padding: 13px 16px; color: var(--text); border-bottom: 1px solid var(--border);
  vertical-align: middle;
}
.data-table tbody tr:last-child td { border-bottom: none; }
.cell-dim { color: var(--text-3); font-size: 0.8rem; }

/* ===== 状态标签 ===== */
.status-badge {
  display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px;
  border-radius: var(--radius-pill); font-size: 0.72rem; font-weight: 600; white-space: nowrap;
}
.status--ok { background: var(--success-soft); color: var(--success); }
.status--off { background: var(--danger-soft); color: var(--danger); }

/* ===== 消息提示 ===== */
.msg {
  margin-top: 4px; text-align: center; font-size: 0.85rem; padding: 12px;
  border-radius: var(--radius-sm); animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47, 158, 68, 0.18); }
.msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224, 49, 49, 0.18); }

/* ===== 底部 ===== */
.page-footer { padding: 8px 0 28px; text-align: center; }
.page-footer span { font-size: 0.72rem; color: var(--text-4); }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .admin-body { padding: 84px 16px 40px; }
  .nav-nickname { display: none; }
  .op-row { flex-direction: column; align-items: stretch; }
  .op-field { min-width: 100%; }
  .op-btns { width: 100%; }
  .op-btn { flex: 1; text-align: center; }
}
</style>
