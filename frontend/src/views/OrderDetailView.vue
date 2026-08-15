<template>
  <div class="order-detail-page">
    <!-- 顶部导航 -->
    <nav class="nav">
      <div class="nav-logo" @click="goHome">ZuiMShop</div>
      <div class="avatar-wrapper" @click.stop>
        <span class="nav-balance" v-if="userBalance !== null">余额 ¥{{ Number(userBalance).toFixed(2) }}</span>
        <span class="nav-nickname">{{ nickname }}</span>
        <button class="avatar-btn" @click="profileMenuOpen = !profileMenuOpen" aria-label="用户菜单">
          <span class="avatar-circle">
            <img :src="avatarUrl" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
          </span>
          <span class="avatar-caret" :class="{ open: profileMenuOpen }">▾</span>
        </button>
        <transition name="profile">
          <div class="profile-menu" v-if="profileMenuOpen">
            <div class="profile-menu-header">
              <span class="profile-menu-avatar">
                <img :src="avatarUrl" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
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
    </nav>

    <!-- 主内容 -->
    <main class="detail-main">
      <!-- 面包屑 / 返回 -->
      <div class="breadcrumb">
        <button class="crumb-btn" @click="goBack">‹ 返回订单列表</button>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">订单详情</span>
      </div>

      <div class="detail-panel">
        <!-- 加载中 -->
        <div class="state-box" v-if="loading">
          <div class="spinner"></div>
          <p class="state-hint">加载订单详情...</p>
        </div>

        <!-- 订单不存在 -->
        <div class="state-box" v-else-if="!order">
          <div class="state-icon">🔍</div>
          <p class="state-title">订单不存在</p>
          <p class="state-hint">抱歉，未找到该订单信息</p>
          <button class="btn btn--primary" @click="goBack">返回订单列表</button>
        </div>

        <!-- 订单详情 -->
        <template v-else>
          <!-- 状态栏 -->
          <div class="status-banner">
            <div class="status-symbol" :class="getOrderStatusClass(order.status)"></div>
            <div class="status-info">
              <h2 class="status-title">{{ getOrderStatusText(order.status) }}</h2>
              <p class="status-order-no">订单号：{{ order.orderNo || order.id }}</p>
            </div>
            <span class="status-badge" :class="getOrderStatusClass(order.status)">
              {{ getOrderStatusText(order.status) }}
            </span>
          </div>

          <!-- 收货信息 -->
          <div class="section" v-if="order.receiverAddress || order.receiverName">
            <h3 class="section-title">收货信息</h3>
            <div class="address-card">
              <div class="address-line">
                <span class="address-name">{{ order.receiverName }}</span>
                <span class="address-phone">{{ order.receiverPhone }}</span>
              </div>
              <p class="address-detail">📍 {{ order.receiverAddress }}</p>
            </div>
          </div>

          <!-- 商品清单 -->
          <div class="section">
            <h3 class="section-title">商品清单</h3>
            <div class="items-list">
              <div v-for="item in (order.items || order.orderItems || [])" :key="item.id" class="item-row">
                <img
                  :src="item.mainImageUrl || item.image || '/uploads/hero/hero-1.jpg'"
                  :alt="item.productName"
                  class="item-thumb"
                >
                <div class="item-info">
                  <h4 class="item-name">{{ item.productName }}</h4>
                  <span class="item-price">¥{{ (item.price || 0).toFixed(2) }}</span>
                  <span class="item-qty">×{{ item.quantity || item.qty || 1 }}</span>
                </div>
                <span class="item-subtotal">¥{{ ((item.price || 0) * (item.quantity || item.qty || 1)).toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 金额明细 -->
          <div class="section">
            <h3 class="section-title">金额明细</h3>
            <div class="amount-card">
              <div class="amount-row">
                <span class="amount-label">商品总额</span>
                <span class="amount-value">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              </div>
              <div class="amount-row amount-row--total">
                <span class="amount-label">实付金额</span>
                <span class="amount-value amount-value--real">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 订单信息 -->
          <div class="section">
            <h3 class="section-title">订单信息</h3>
            <div class="info-list">
              <div class="info-row">
                <span class="info-label">订单号</span>
                <span class="info-value">{{ order.orderNo || order.id }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">订单状态</span>
                <span class="info-value info-value--status" :class="getOrderStatusClass(order.status)">
                  {{ getOrderStatusText(order.status) }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">下单时间</span>
                <span class="info-value">{{ formatDate(order.createTime) }}</span>
              </div>
              <div class="info-row" v-if="order.payTime">
                <span class="info-label">支付时间</span>
                <span class="info-value">{{ formatDate(order.payTime) }}</span>
              </div>
              <div class="info-row" v-if="order.shipTime">
                <span class="info-label">发货时间</span>
                <span class="info-value">{{ formatDate(order.shipTime) }}</span>
              </div>
              <div class="info-row" v-if="order.completeTime">
                <span class="info-label">完成时间</span>
                <span class="info-value">{{ formatDate(order.completeTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <button class="btn btn--primary btn--lg" @click="handlePay"
                    v-if="order.status === 0" :disabled="actionLoading">
              立即支付
            </button>
            <button class="btn btn--ghost btn--lg btn--danger-text" @click="handleCancel"
                    v-if="order.status === 0" :disabled="actionLoading">
              取消订单
            </button>
            <button class="btn btn--ghost btn--lg btn--warn-text" @click="handleRefund"
                    v-if="order.status === 1 || order.status === 2" :disabled="actionLoading">
              申请退款
            </button>
            <button class="btn btn--ghost btn--lg" @click="goBack">
              返回列表
            </button>
          </div>

          <!-- 消息提示 -->
          <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
        </template>
      </div>
    </main>

    <!-- 底部 -->
    <div class="detail-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserInfo, getOrderDetail, payOrder, cancelOrder, refundOrder } from '../api/index.js'
import { getCachedAvatar } from '../utils/avatarCache.js'

const route = useRoute()
const router = useRouter()

const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'

const nickname = ref(localStorage.getItem('nickname') || '')
const profileMenuOpen = ref(false)
const avatarUrl = ref(DEFAULT_AVATAR)
const userBalance = ref(null)

function getCachedRoles() {
  const roles = localStorage.getItem('roles')
  return roles ? JSON.parse(roles) : []
}
const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))
const roleLabel = computed(() => {
  if (isAdminUser.value) return '管理员'
  if (isSellerUser.value) return '商家'
  return '普通用户'
})

async function fetchUserProfile() {
  try {
    const res = await getUserInfo()
    if (res && res.code === 200 && res.data) {
      const u = res.data
      if (u.avatar) avatarUrl.value = getCachedAvatar(u.avatar)
      if (u.nickname) nickname.value = u.nickname
      if (u.balance !== undefined && u.balance !== null) {
        userBalance.value = Number(u.balance)
      }
    }
  } catch (e) {
    console.error('获取用户资料失败:', e)
  }
}

function closePopups() {
  profileMenuOpen.value = false
}

function goProfile() {
  router.push('/profile')
}

const orderId = ref(route.params.id)
const order = ref(null)
const loading = ref(true)
const actionLoading = ref(false)
const message = ref('')
const msgSuccess = ref(false)

/**
 * 获取订单详情
 */
async function fetchOrderDetail() {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    if (res && res.code === 200 && res.data) {
      // 后端返回 { order: {...}, items: [...] } 嵌套结构，需展平
      const o = res.data.order || res.data
      if (o) {
        o.items = res.data.items || []
        order.value = o
      } else {
        order.value = null
      }
    } else {
      order.value = null
    }
  } catch (e) {
    console.error('获取订单详情失败:', e)
    order.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 支付订单
 */
async function handlePay() {
  actionLoading.value = true
  try {
    const res = await payOrder(order.value.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，支付按钮即刻消失
      order.value.status = 1
      showMessage(res.mes || '支付成功', true)
    } else {
      showMessage(res?.mes || '支付失败', false)
    }
  } catch {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

/**
 * 取消订单
 */
async function handleCancel() {
  if (!confirm('确定要取消该订单吗？')) return
  actionLoading.value = true
  try {
    const res = await cancelOrder(order.value.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，操作按钮即刻消失
      order.value.status = 4
      showMessage('订单已取消', true)
    } else {
      showMessage(res?.mes || '取消失败', false)
    }
  } catch {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

/**
 * 申请退款
 */
async function handleRefund() {
  if (!confirm('确定要申请退款吗？')) return
  actionLoading.value = true
  try {
    const res = await refundOrder(order.value.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，操作按钮即刻消失
      order.value.status = 5
      showMessage('退款申请已提交', true)
    } else {
      showMessage(res?.mes || '申请失败', false)
    }
  } catch {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

/**
 * 获取订单状态文本
 */
function getOrderStatusText(status) {
  // 兼容后端可能返回字符串的情况
  const s = Number(status)
  const map = {
    0: '待支付', 1: '已支付', 2: '已发货',
    3: '已完成', 4: '已取消', 5: '已退款'
  }
  return map[s] !== undefined ? map[s] : '未知'
}

function getOrderStatusClass(status) {
  const s = Number(status)
  const map = {
    0: 'status--pending', 1: 'status--paid', 2: 'status--shipped',
    3: 'status--completed', 4: 'status--cancelled', 5: 'status--refunded'
  }
  return map[s] || 'status--unknown'
}

/**
 * 格式化日期
 */
function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return dateStr
  }
}

function showMessage(msg, success) {
  message.value = msg
  msgSuccess.value = success
  setTimeout(() => { message.value = '' }, 3000)
}

function goHome() { router.push('/home') }
function goOrders() { router.push('/orders') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }
function goBack() {
  // 保留当前路由 query（如选中的状态标签），返回列表页时保持一致
  router.push({ path: '/orders', query: route.query.status ? { status: route.query.status } : {} })
}
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  document.addEventListener('click', closePopups)
  fetchOrderDetail()
  fetchUserProfile()
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})
</script>

<style scoped>
/* ===== 页面骨架 ===== */
.order-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  font-family: var(--font-sans);
  color: var(--text);
}

/* ===== 顶部导航 ===== */
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border-bottom: 1px solid var(--border);
}
.nav-logo {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
}

/* ===== 用户头像与下拉菜单 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 6px; }
.nav-balance {
  padding: 5px 12px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  border: 1px solid var(--border);
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
.nav-nickname { font-size: 0.85rem; font-weight: 600; color: var(--text-2); padding: 0 4px; }
.avatar-btn {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none; cursor: pointer; padding: 2px;
  transition: transform 0.3s;
}
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 38px; height: 38px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff; font-family: var(--font-sans);
  font-size: 1rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.avatar-circle img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-caret { font-size: 0.7rem; color: var(--text-3); transition: transform 0.3s; }
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 14px); right: 0;
  min-width: 180px; padding: 8px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
}
.profile-menu-header {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-bottom: 1px solid var(--border); margin-bottom: 6px;
}
.profile-menu-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff; font-family: var(--font-sans);
  font-size: 1.05rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; overflow: hidden;
}
.profile-menu-avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name {
  font-size: 0.9rem; font-weight: 600; color: var(--text); margin: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.profile-menu-role { font-size: 0.72rem; color: var(--text-3); margin: 2px 0 0; }
.profile-menu-item {
  display: block; width: 100%; text-align: left;
  padding: 10px 14px; border: none; border-radius: var(--radius-sm);
  background: transparent; cursor: pointer;
  font-family: var(--font-sans); font-size: 0.85rem;
  color: var(--text-2); transition: all 0.2s;
}
.profile-menu-item:hover { background: var(--primary-soft); color: var(--primary); }
.profile-menu-item--logout:hover { background: var(--danger-soft); color: var(--danger); }
.profile-menu-divider { height: 1px; background: var(--border); margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主体布局 ===== */
.detail-main {
  flex: 1;
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

/* 面包屑 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}
.crumb-btn {
  background: none;
  border: none;
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  color: var(--text-2);
  font-family: var(--font-sans);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.crumb-btn:hover { background: var(--primary-soft); color: var(--primary); }
.crumb-sep { color: var(--text-4); font-size: 0.85rem; }
.crumb-current { font-size: 0.85rem; font-weight: 600; color: var(--text-3); }

/* 详情面板 */
.detail-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 32px;
}

/* ===== 加载 & 空态 ===== */
.state-box { text-align: center; padding: 80px 16px; color: var(--text-3); }
.state-icon { font-size: 3rem; margin-bottom: 12px; }
.state-title { font-size: 1.1rem; font-weight: 700; color: var(--text-2); margin-bottom: 6px; }
.state-hint { font-size: 0.85rem; color: var(--text-3); margin-bottom: 20px; }

/* ===== 状态栏 ===== */
.status-banner {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px;
  margin-bottom: 28px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.status-symbol {
  width: 56px; height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  background: var(--surface-3);
  flex-shrink: 0;
}
.status-symbol.status--pending { background: var(--warning-soft); }
.status-symbol.status--paid { background: var(--primary-soft); }
.status-symbol.status--shipped { background: var(--accent-soft); }
.status-symbol.status--completed { background: var(--success-soft); }
.status-symbol.status--cancelled { background: var(--surface-3); }
.status-symbol.status--refunded { background: var(--danger-soft); }
.status-symbol.status--unknown { background: var(--surface-3); }
.status-symbol::before { line-height: 1; }
.status-symbol.status--pending::before { content: '⏳'; }
.status-symbol.status--paid::before { content: '📦'; }
.status-symbol.status--shipped::before { content: '🚚'; }
.status-symbol.status--completed::before { content: '✅'; }
.status-symbol.status--cancelled::before { content: '🚫'; }
.status-symbol.status--refunded::before { content: '↩️'; }
.status-symbol.status--unknown::before { content: '❔'; }
.status-info { flex: 1; min-width: 0; }
.status-title { font-size: 1.3rem; font-weight: 700; color: var(--text); margin-bottom: 4px; }
.status-order-no { font-size: 0.82rem; color: var(--text-3); }
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: var(--radius-pill);
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}
.status-badge.status--pending { color: var(--warning); background: var(--warning-soft); }
.status-badge.status--paid { color: var(--primary); background: var(--primary-soft); }
.status-badge.status--shipped { color: var(--accent); background: var(--accent-soft); }
.status-badge.status--completed { color: var(--success); background: var(--success-soft); }
.status-badge.status--cancelled { color: var(--text-3); background: var(--surface-3); }
.status-badge.status--refunded { color: var(--danger); background: var(--danger-soft); }
.status-badge.status--unknown { color: var(--text-4); background: var(--surface-3); }

/* ===== 分区 ===== */
.section { margin-bottom: 28px; }
.section-title {
  font-family: var(--font-sans);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--primary), var(--accent));
}

/* ===== 收货信息 ===== */
.address-card {
  padding: 18px 20px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.address-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.address-name { font-size: 1rem; font-weight: 700; color: var(--text); }
.address-phone {
  font-size: 0.85rem;
  color: var(--text-2);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  background: var(--surface-3);
}
.address-detail { font-size: 0.88rem; color: var(--text-3); }

/* ===== 商品清单 ===== */
.items-list { display: flex; flex-direction: column; gap: 10px; }
.item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.item-thumb {
  width: 64px; height: 64px;
  border-radius: var(--radius);
  object-fit: cover;
  background: var(--surface-3);
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.item-info { flex: 1; min-width: 0; }
.item-name {
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-price { font-size: 0.85rem; font-weight: 700; color: var(--price); }
.item-qty { font-size: 0.85rem; color: var(--text-3); margin-left: 10px; }
.item-subtotal {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--price);
  flex-shrink: 0;
  font-family: var(--font-sans);
}

/* ===== 金额明细 ===== */
.amount-card {
  padding: 6px 20px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed var(--border);
}
.amount-row:last-child { border-bottom: none; }
.amount-label { font-size: 0.88rem; color: var(--text-3); }
.amount-value { font-size: 0.95rem; font-weight: 600; color: var(--text-2); }
.amount-row--total .amount-label { font-size: 0.95rem; font-weight: 700; color: var(--text); }
.amount-value--real { font-size: 1.25rem; font-weight: 800; color: var(--price); font-family: var(--font-sans); }

/* ===== 订单信息 ===== */
.info-list {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 6px 20px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 13px 0;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 0.85rem; color: var(--text-3); flex-shrink: 0; }
.info-value { font-size: 0.88rem; font-weight: 600; color: var(--text-2); word-break: break-all; text-align: right; }
.info-value--status {
  padding: 3px 12px;
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
}
.info-value--status.status--pending { color: var(--warning); background: var(--warning-soft); }
.info-value--status.status--paid { color: var(--primary); background: var(--primary-soft); }
.info-value--status.status--shipped { color: var(--accent); background: var(--accent-soft); }
.info-value--status.status--completed { color: var(--success); background: var(--success-soft); }
.info-value--status.status--cancelled { color: var(--text-3); background: var(--surface-3); }
.info-value--status.status--refunded { color: var(--danger); background: var(--danger-soft); }
.info-value--status.status--unknown { color: var(--text-4); background: var(--surface-3); }

/* ===== 操作按钮 ===== */
.action-section {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}
.btn--primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  border: none;
  color: #fff;
  box-shadow: var(--shadow-primary);
}
.btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(47, 84, 235, 0.35);
}
.btn--danger-text { color: var(--danger); }
.btn--danger-text:hover:not(:disabled) { border-color: var(--danger); color: var(--danger); }
.btn--warn-text { color: var(--warning); }
.btn--warn-text:hover:not(:disabled) { border-color: var(--warning); color: var(--warning); }

/* ===== 消息提示 ===== */
.msg {
  margin-top: 20px;
  text-align: center;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 12px;
  border-radius: var(--radius);
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47, 158, 68, 0.2); }
.msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224, 49, 49, 0.2); }

/* ===== 底部 ===== */
.detail-footer { padding: 24px 32px; text-align: center; }
.detail-footer span { font-size: 0.72rem; color: var(--text-4); }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 20px; }
  .nav-balance { display: none; }
  .detail-main { padding: 24px 16px 48px; }
  .detail-panel { padding: 20px 16px; }
  .status-banner { flex-wrap: wrap; }
  .status-symbol { width: 48px; height: 48px; }
  .status-badge { margin-left: auto; }
  .item-row { flex-wrap: wrap; }
  .item-subtotal { margin-left: auto; }
  .action-section { flex-direction: column; }
  .action-section .btn { width: 100%; text-align: center; }
}
</style>
