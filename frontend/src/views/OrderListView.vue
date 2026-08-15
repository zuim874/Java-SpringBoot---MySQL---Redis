<template>
  <div class="orders-page">
    <!-- 顶部导航 -->
    <nav class="nav">
      <div class="nav-logo" @click="goHome">ZuiMShop</div>
      <div class="nav-actions">
        <!-- 用户头像与菜单 -->
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
      </div>
    </nav>

    <!-- 订单中心主体 -->
    <main class="orders-main">
      <!-- 订单页头 -->
      <div class="orders-head">
        <div class="head-left">
          <h1 class="head-title">我的订单</h1>
          <p class="head-sub">{{ nickname }}，这里记录着您的每一笔消费</p>
        </div>
        <button class="head-btn" @click="goHome">返回商城</button>
      </div>

      <!-- 状态筛选 Tabs -->
      <div class="status-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="switchStatus(tab.value)"
        >
          <span class="status-tab-dot" :class="'dot--' + getStatusDotClass(tab.value)"></span>
          <span class="status-tab-label">{{ tab.label }}</span>
        </button>
      </div>

      <!-- 订单内容卡片 -->
      <div class="orders-panel">
        <!-- 加载中 -->
        <div class="state-box" v-if="loading">
          <div class="spinner"></div>
          <p class="state-hint">加载订单中...</p>
        </div>

        <!-- 错误状态 -->
        <div class="state-box" v-else-if="error">
          <div class="state-icon">⚠️</div>
          <p class="state-title">订单加载失败</p>
          <p class="state-hint">网络似乎开小差了，请稍后重试</p>
          <button class="btn btn--primary" @click="fetchOrders">重新加载</button>
        </div>

        <!-- 空订单 -->
        <div class="state-box" v-else-if="orders.length === 0">
          <div class="state-icon">🛒</div>
          <p class="state-title">暂无订单</p>
          <p class="state-hint">快去商城挑选心仪的商品吧</p>
          <button class="btn btn--primary" @click="goHome">去逛逛</button>
        </div>

        <!-- 订单列表 -->
        <div class="order-list" v-else>
          <div v-for="order in orders" :key="order.id" class="order-card" @click="goToOrderDetail(order.id)">
            <!-- 订单头部 -->
            <div class="order-head">
              <div class="order-meta">
                <span class="order-no">订单号：{{ order.orderNo || order.id }}</span>
                <span class="order-time">{{ formatDate(order.createTime) }}</span>
              </div>
              <span class="status-badge" :class="getOrderStatusClass(order.status)">
                {{ getOrderStatusText(order.status) }}
              </span>
            </div>

            <!-- 订单商品行 -->
            <div class="order-items">
              <div v-for="item in (order.items || order.orderItems || [])" :key="item.id" class="order-item">
                <img
                  :src="item.mainImageUrl || item.image || '/uploads/hero/hero-1.jpg'"
                  :alt="item.productName"
                  class="item-thumb"
                >
                <div class="item-info">
                  <h4 class="item-name">{{ item.productName }}</h4>
                  <span class="item-price">¥{{ (item.price || 0).toFixed(2) }}</span>
                </div>
                <span class="item-qty">×{{ item.quantity || item.qty || 1 }}</span>
              </div>
              <!-- 如果 items 为空，显示商品名 -->
              <div v-if="!order.items && !order.orderItems && order.productName" class="order-item">
                <div class="item-info">
                  <h4 class="item-name">{{ order.productName }}</h4>
                </div>
              </div>
            </div>

            <!-- 订单底部 -->
            <div class="order-foot">
              <div class="order-total">
                <span class="total-count">共 {{ getItemCount(order) }} 件商品</span>
                <span class="total-label">合计：</span>
                <span class="total-price">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              </div>
              <div class="order-actions" @click.stop>
                <button class="btn btn--primary" @click="handlePay(order)"
                        v-if="order.status === 0"
                        :disabled="actionLoading">立即支付</button>
                <button class="btn btn--ghost btn--danger-text" @click="handleCancel(order)"
                        v-if="order.status === 0"
                        :disabled="actionLoading">取消订单</button>
                <button class="btn btn--ghost btn--warn-text" @click="handleRefund(order)"
                        v-if="order.status === 1 || order.status === 2"
                        :disabled="actionLoading">申请退款</button>
                <button class="btn btn--ghost" @click="goToOrderDetail(order.id)">查看详情</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹ 上一页</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页 ›</button>
        </div>

        <!-- 消息提示 -->
        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
      </div>
    </main>

    <!-- 底部 -->
    <div class="orders-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserOrders, payOrder, cancelOrder, refundOrder, getUserInfo } from '../api/index.js'
import { getCachedAvatar } from '../utils/avatarCache.js'
import { getCachedRoles } from '../utils/auth.js'

const router = useRouter()
const route = useRoute()

// ===== 状态变量 =====
const orders = ref([])
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 10
const activeStatus = ref('')
const actionLoading = ref(false)
const message = ref('')
const msgSuccess = ref(false)

const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'
const avatarUrl = ref(DEFAULT_AVATAR)
const userBalance = ref(null)
const profileMenuOpen = ref(false)

const nickname = ref(localStorage.getItem('nickname') || '用户')

const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))
const roleLabel = computed(() => {
  if (isAdminUser.value) return '管理员'
  if (isSellerUser.value) return '商家'
  return '普通用户'
})

// 状态标签选项（后端使用数字状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款）
const statusTabs = [
  { value: '', label: '全部' },
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付' },
  { value: 2, label: '已发货' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已退款' }
]

/**
 * 切换状态标签
 * 同时将选中状态写入 URL query，返回本页时保持所选标签
 */
function switchStatus(status) {
  activeStatus.value = status
  currentPage.value = 1
  router.replace({
    path: '/orders',
    query: status === '' ? {} : { status: String(status) }
  })
  fetchOrders()
}

/**
 * 获取订单列表
 */
async function fetchOrders() {
  loading.value = true
  error.value = false
  try {
    const res = await getUserOrders(currentPage.value, pageSize, activeStatus.value)
    if (res && res.code === 200 && res.data) {
      orders.value = res.data.records || []
      totalPages.value = res.data.pages || 1
    } else {
      orders.value = []
      totalPages.value = 1
    }
  } catch (e) {
    console.error('获取订单列表失败:', e)
    error.value = true
    orders.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 分页跳转
 */
function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchOrders()
}

/**
 * 支付订单
 */
async function handlePay(order) {
  actionLoading.value = true
  try {
    const res = await payOrder(order.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，支付按钮即刻消失
      order.status = 1
      showMessage(res.mes || '支付成功', true)
      // 稍后重新拉取（等待 MQ 异步清理订单列表缓存）
      setTimeout(() => fetchOrders(), 800)
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
async function handleCancel(order) {
  if (!confirm('确定要取消该订单吗？')) return
  actionLoading.value = true
  try {
    const res = await cancelOrder(order.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，操作按钮即刻消失
      order.status = 4
      showMessage('订单已取消', true)
      setTimeout(() => fetchOrders(), 800)
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
async function handleRefund(order) {
  if (!confirm('确定要申请退款吗？')) return
  actionLoading.value = true
  try {
    const res = await refundOrder(order.id)
    if (res && res.code === 200) {
      // 本地立即更新订单状态，操作按钮即刻消失
      order.status = 5
      showMessage('退款申请已提交', true)
      setTimeout(() => fetchOrders(), 800)
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
 * 跳转订单详情
 */
function goToOrderDetail(id) {
  router.push(`/order/${id}`)
}

/**
 * 获取订单商品数量
 */
function getItemCount(order) {
  const items = order.items || order.orderItems || []
  if (items.length > 0) {
    return items.reduce((sum, item) => sum + (item.quantity || item.qty || 1), 0)
  }
  return 1
}

/**
 * 获取订单状态文本
 */
function getOrderStatusText(status) {
  const map = {
    0: '待支付', 1: '已支付', 2: '已发货',
    3: '已完成', 4: '已取消', 5: '已退款'
  }
  return map[status] !== undefined ? map[status] : '未知'
}

/**
 * 获取订单状态 CSS 类名
 */
function getOrderStatusClass(status) {
  const map = {
    0: 'status--pending', 1: 'status--paid', 2: 'status--shipped',
    3: 'status--completed', 4: 'status--cancelled', 5: 'status--refunded'
  }
  return map[status] || 'status--unknown'
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
function goProfile() { router.push('/profile') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

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

function getStatusDotClass(tabValue) {
  if (tabValue === '') return 'all'
  const map = {
    0: 'pending', 1: 'paid', 2: 'shipped',
    3: 'completed', 4: 'cancelled', 5: 'refunded'
  }
  return map[tabValue] || 'unknown'
}

function closePopups() { profileMenuOpen.value = false }

onMounted(() => {
  // 从 URL query 恢复上次选中的状态标签（从详情页返回时保持一致）
  const q = route.query.status
  const matched = statusTabs.find(t => t.value !== '' && String(t.value) === q)
  activeStatus.value = matched ? matched.value : ''
  fetchUserProfile()
  fetchOrders()
  document.addEventListener('click', closePopups)
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})
</script>

<style scoped>
/* ===== 页面骨架 ===== */
.orders-page {
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
.nav-actions { display: flex; align-items: center; gap: 12px; }

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
.orders-main {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 40px 24px 60px;
}

/* 订单页头 */
.orders-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.head-title {
  font-family: var(--font-display);
  font-size: 1.9rem;
  font-weight: 700;
  color: var(--text);
  line-height: 1.2;
}
.head-sub { margin-top: 6px; font-size: 0.88rem; color: var(--text-3); }
.head-btn {
  flex-shrink: 0;
  padding: 9px 22px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-2);
  font-family: var(--font-sans);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.head-btn:hover { border-color: var(--primary); color: var(--primary); }

/* ===== 状态筛选 Tabs ===== */
.status-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-2);
  font-family: var(--font-sans);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  white-space: nowrap;
}
.status-tab:hover { background: var(--surface-3); color: var(--primary); }
.status-tab.active {
  background: var(--primary-soft);
  border-color: var(--border-strong);
  color: var(--primary);
}
.status-tab-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.status-tab-dot.dot--all { background: var(--text-3); }
.status-tab-dot.dot--pending { background: var(--warning); }
.status-tab-dot.dot--paid { background: var(--primary); }
.status-tab-dot.dot--shipped { background: var(--accent); }
.status-tab-dot.dot--completed { background: var(--success); }
.status-tab-dot.dot--cancelled { background: var(--text-3); }
.status-tab-dot.dot--refunded { background: var(--danger); }
.status-tab-dot.dot--unknown { background: var(--text-4); }

/* ===== 订单面板 ===== */
.orders-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 28px;
}

/* ===== 加载 & 空态 ===== */
.state-box { text-align: center; padding: 72px 16px; color: var(--text-3); }
.state-icon { font-size: 3rem; margin-bottom: 12px; }
.state-title { font-size: 1.05rem; font-weight: 600; color: var(--text-2); margin-bottom: 6px; }
.state-hint { font-size: 0.85rem; color: var(--text-3); margin-bottom: 18px; }

/* ===== 订单卡片 ===== */
.order-list { display: flex; flex-direction: column; gap: 18px; }
.order-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.3s, border-color 0.3s, transform 0.3s;
}
.order-card:hover {
  box-shadow: var(--shadow);
  border-color: var(--border-strong);
  transform: translateY(-2px);
}

/* 订单头部 */
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}
.order-meta { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.order-no { font-size: 0.82rem; font-weight: 700; color: var(--text-2); letter-spacing: 0.3px; }
.order-time { font-size: 0.75rem; color: var(--text-3); }

/* 状态标签 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.4;
}
.status-badge.status--pending { color: var(--warning); background: var(--warning-soft); }
.status-badge.status--paid { color: var(--primary); background: var(--primary-soft); }
.status-badge.status--shipped { color: var(--accent); background: var(--accent-soft); }
.status-badge.status--completed { color: var(--success); background: var(--success-soft); }
.status-badge.status--cancelled { color: var(--text-3); background: var(--surface-3); }
.status-badge.status--refunded { color: var(--danger); background: var(--danger-soft); }
.status-badge.status--unknown { color: var(--text-4); background: var(--surface-3); }

/* 商品行 */
.order-items { padding: 14px 20px; }
.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.order-item:last-child { border-bottom: none; }
.item-thumb {
  width: 62px; height: 62px;
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
.item-qty { font-size: 0.85rem; color: var(--text-3); flex-shrink: 0; }

/* 订单底部 */
.order-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  flex-wrap: wrap;
}
.order-total { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
.total-count { font-size: 0.8rem; color: var(--text-3); margin-right: 8px; }
.total-label { font-size: 0.85rem; color: var(--text-2); font-weight: 500; }
.total-price { font-size: 1.15rem; font-weight: 800; color: var(--price); font-family: var(--font-sans); }
.order-actions { display: flex; gap: 10px; flex-wrap: wrap; }

/* ===== 按钮（覆盖全局 btn 以适配冷色风格） ===== */
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

/* ===== 分页 ===== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}
.page-btn {
  padding: 10px 22px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-2);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: var(--text-3); font-weight: 600; }

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
.orders-footer { padding: 24px 32px; text-align: center; }
.orders-footer span { font-size: 0.72rem; color: var(--text-4); }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 20px; }
  .nav-balance { display: none; }
  .orders-main { padding: 28px 16px 48px; }
  .orders-head { flex-direction: column; align-items: flex-start; }
  .head-btn { width: 100%; }
  .status-tabs { flex-wrap: nowrap; overflow-x: auto; padding: 6px; }
  .status-tab { flex-shrink: 0; padding: 8px 14px; font-size: 0.8rem; }
  .orders-panel { padding: 16px; }
  .order-head { flex-direction: column; align-items: flex-start; }
  .order-foot { flex-direction: column; align-items: stretch; }
  .order-actions { width: 100%; }
  .order-actions .btn { flex: 1; text-align: center; }
}
</style>
