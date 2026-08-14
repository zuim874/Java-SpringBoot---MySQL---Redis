<template>
  <div class="order-detail-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
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
            <button class="profile-menu-item" @click="profileMenuOpen = false; goProfile()">个人中心</button>
            <div class="profile-menu-divider"></div>
            <button class="profile-menu-item profile-menu-item--logout" @click="handleLogout">退出登录</button>
          </div>
        </transition>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="detail-wrapper">
      <div class="detail-card">
        <!-- 加载中 -->
        <div class="loading-state" v-if="loading">
          <div class="loading-spinner"></div>
          <p>加载订单详情...</p>
        </div>

        <!-- 订单不存在 -->
        <div class="empty-state" v-else-if="!order">
          <div class="error-icon">🔍</div>
          <h2>订单不存在</h2>
          <p>抱歉，未找到该订单信息</p>
          <button class="retry-btn" @click="goBack">返回订单列表</button>
        </div>

        <!-- 订单详情 -->
        <template v-else>
          <div class="detail-header">
            <div class="detail-icon">📋</div>
            <h2 class="detail-title">订单详情</h2>
            <p class="detail-desc">订单号: {{ order.orderNo || order.id }}</p>
          </div>

          <!-- 订单状态 -->
          <div class="status-section">
            <div class="status-timeline">
              <div class="timeline-step" :class="{ active: true }">
                <span class="step-dot"></span>
                <span class="step-label">{{ getOrderStatusText(order.status) }}</span>
              </div>
            </div>
            <div class="status-badge-large" :class="getOrderStatusClass(order.status)">
              {{ getOrderStatusText(order.status) }}
            </div>
          </div>

          <!-- 商品列表 -->
          <div class="section">
            <h3 class="section-title">商品信息</h3>
            <div class="items-list">
              <div v-for="item in (order.items || order.orderItems || [])" :key="item.id" class="item-row">
                <img :src="item.mainImageUrl || item.image || '/uploads/hero/hero-1.jpg'"
                     :alt="item.productName" class="item-img">
                <div class="item-info">
                  <h4>{{ item.productName }}</h4>
                  <p class="item-price">¥{{ (item.price || 0).toFixed(2) }}</p>
                </div>
                <span class="item-qty">x{{ item.quantity || item.qty || 1 }}</span>
                <span class="item-subtotal">¥{{ ((item.price || 0) * (item.quantity || item.qty || 1)).toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 订单汇总 -->
          <div class="section">
            <h3 class="section-title">订单汇总</h3>
            <div class="summary-grid">
              <div class="summary-row">
                <span class="summary-label">商品总额</span>
                <span class="summary-value">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">订单状态</span>
                <span class="summary-value status-value" :class="getOrderStatusClass(order.status)">
                  {{ getOrderStatusText(order.status) }}
                </span>
              </div>
              <div class="summary-row">
                <span class="summary-label">下单时间</span>
                <span class="summary-value">{{ formatDate(order.createTime) }}</span>
              </div>
              <div class="summary-row" v-if="order.payTime">
                <span class="summary-label">支付时间</span>
                <span class="summary-value">{{ formatDate(order.payTime) }}</span>
              </div>
              <div class="summary-row" v-if="order.shipTime">
                <span class="summary-label">发货时间</span>
                <span class="summary-value">{{ formatDate(order.shipTime) }}</span>
              </div>
              <div class="summary-row" v-if="order.completeTime">
                <span class="summary-label">完成时间</span>
                <span class="summary-value">{{ formatDate(order.completeTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 收货地址 -->
          <div class="section" v-if="order.receiverAddress || order.receiverName">
            <h3 class="section-title">收货信息</h3>
            <div class="address-card">
              <p class="address-name">{{ order.receiverName }}</p>
              <p class="address-phone">{{ order.receiverPhone }}</p>
              <p class="address-detail">{{ order.receiverAddress }}</p>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <button class="action-btn action-btn--primary" @click="handlePay"
                    v-if="order.status === 0" :disabled="actionLoading">
              立即支付
            </button>
            <button class="action-btn action-btn--cancel" @click="handleCancel"
                    v-if="order.status === 0" :disabled="actionLoading">
              取消订单
            </button>
            <button class="action-btn action-btn--refund" @click="handleRefund"
                    v-if="order.status === 1 || order.status === 2" :disabled="actionLoading">
              申请退款
            </button>
            <button class="action-btn action-btn--outline" @click="goBack">
              返回列表
            </button>
          </div>

          <!-- 消息提示 -->
          <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
        </template>
      </div>
    </div>

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
/* ===== 全局 ===== */
.order-detail-page {
  min-height: 100vh; display: flex; flex-direction: column; align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif; position: relative; overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-circle {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35;
}
.bg-circle--1 { width: 600px; height: 600px; top: -200px; right: -200px; background: radial-gradient(circle, #4a9eff, #2b6cb0); }
.bg-circle--2 { width: 500px; height: 500px; bottom: -150px; left: -150px; background: radial-gradient(circle, #7c3aed, #5b21b6); }
.bg-circle--3 { width: 300px; height: 300px; top: 40%; left: 10%; background: radial-gradient(circle, #4a9eff, transparent); }

/* ===== 导航 ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0; height: 72px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 56px; background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
}
.nav-logo {
  font-family: 'Playfair Display', Georgia, serif; font-size: 1.4rem; font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  cursor: pointer;
}
.nav-actions { display: flex; gap: 12px; }
.nav-btn {
  padding: 8px 20px; border: 1px solid rgba(43,108,176,0.20); border-radius: 10px;
  background: rgba(255,255,255,0.60); font-family: 'DM Sans', sans-serif;
  font-size: 0.8rem; font-weight: 600; color: #2b6cb0; cursor: pointer; transition: all 0.3s;
}
.nav-btn:hover { background: #2b6cb0; color: white; border-color: #2b6cb0; }
.nav-btn--logout { color: #c92a2a; border-color: rgba(201,42,42,0.20); }
.nav-btn--logout:hover { background: #c92a2a; border-color: #c92a2a; color: white; }
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; padding: 0 4px; }

/* ===== 用户头像与下拉菜单 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 6px; }
.nav-balance {
  padding: 5px 12px; border-radius: 50px;
  background: rgba(43,108,176,0.08); border: 1px solid rgba(43,108,176,0.15);
  color: #2b6cb0; font-size: 0.75rem; font-weight: 600;
  white-space: nowrap;
}
.avatar-btn { display: flex; align-items: center; gap: 4px; background: none; border: none; cursor: pointer; padding: 2px; transition: transform 0.3s; }
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle { width: 38px; height: 38px; border-radius: 50%; background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); color: white; font-family: 'DM Sans', sans-serif; font-size: 1rem; font-weight: 600; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(43,108,176,0.25); overflow: hidden; }
.avatar-circle img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-caret { font-size: 0.7rem; color: #868e96; transition: transform 0.3s; }
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu { position: absolute; top: calc(100% + 14px); right: 0; min-width: 180px; padding: 8px; background: rgba(255,255,255,0.96); backdrop-filter: blur(20px) saturate(1.8); -webkit-backdrop-filter: blur(20px) saturate(1.8); border: 1px solid rgba(255,255,255,0.60); border-radius: 14px; box-shadow: 0 12px 40px rgba(0,0,0,0.10); }
.profile-menu-header { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-bottom: 1px solid #f1f3f5; margin-bottom: 6px; }
.profile-menu-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); color: white; font-family: 'DM Sans', sans-serif; font-size: 1.05rem; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; }
.profile-menu-avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name { font-size: 0.9rem; font-weight: 600; color: #212529; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-menu-role { font-size: 0.72rem; color: #adb5bd; margin: 2px 0 0; }
.profile-menu-item { display: block; width: 100%; text-align: left; padding: 10px 14px; border: none; border-radius: 8px; background: transparent; cursor: pointer; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; color: #495057; transition: all 0.2s; }
.profile-menu-item:hover { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.profile-menu-item--logout:hover { background: rgba(220,53,69,0.06); color: #dc3545; }
.profile-menu-divider { height: 1px; background: #f1f3f5; margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主卡片 ===== */
.detail-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.detail-card {
  width: 800px; max-width: 100%; padding: 48px 40px;
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.detail-header { text-align: center; margin-bottom: 32px; }
.detail-icon { font-size: 2rem; margin-bottom: 12px; }
.detail-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700; color: #212529; margin-bottom: 8px; }
.detail-desc { font-size: 0.9rem; color: #868e96; }

/* ===== 状态区域 ===== */
.status-section {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px; background: #f8f9fa; border-radius: 16px; margin-bottom: 24px;
}
.status-timeline { display: flex; gap: 12px; }
.timeline-step { display: flex; align-items: center; gap: 8px; }
.step-dot {
  width: 12px; height: 12px; border-radius: 50%; background: #2b6cb0;
  box-shadow: 0 0 0 3px rgba(43,108,176,0.15);
}
.step-label { font-size: 0.9rem; font-weight: 600; color: #212529; }
.status-badge-large {
  padding: 8px 20px; border-radius: 20px; font-size: 0.9rem; font-weight: 600;
}
.status-badge-large.status--pending { background: rgba(240,140,0,0.1); color: #f08c00; }
.status-badge-large.status--paid { background: rgba(43,108,176,0.1); color: #2b6cb0; }
.status-badge-large.status--shipped { background: rgba(124,58,237,0.1); color: #7c3aed; }
.status-badge-large.status--completed { background: rgba(43,138,62,0.1); color: #2b8a3e; }
.status-badge-large.status--cancelled { background: rgba(134,142,150,0.1); color: #868e96; }
.status-badge-large.status--refunded { background: rgba(201,42,42,0.1); color: #c92a2a; }

/* ===== 分区 ===== */
.section { margin-bottom: 24px; }
.section-title {
  font-family: 'Playfair Display', Georgia, serif; font-size: 1.1rem;
  font-weight: 700; color: #343a40; margin-bottom: 16px;
  padding-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.06);
}

/* ===== 商品列表 ===== */
.items-list { display: flex; flex-direction: column; gap: 12px; }
.item-row {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 16px; background: #f8f9fa; border-radius: 12px;
}
.item-img { width: 60px; height: 60px; border-radius: 10px; object-fit: cover; background: #dee2e6; }
.item-info { flex: 1; min-width: 0; }
.item-info h4 { font-size: 0.9rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.item-price { font-size: 0.85rem; font-weight: 600; color: #2b6cb0; }
.item-qty { font-size: 0.85rem; color: #868e96; }
.item-subtotal { font-size: 0.9rem; font-weight: 700; color: #212529; }

/* ===== 订单汇总 ===== */
.summary-grid { display: flex; flex-direction: column; gap: 12px; }
.summary-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid #f1f3f5;
}
.summary-row:last-child { border-bottom: none; }
.summary-label { font-size: 0.85rem; color: #868e96; }
.summary-value { font-size: 0.9rem; font-weight: 600; color: #212529; }
.status-value { padding: 2px 10px; border-radius: 12px; font-size: 0.8rem; }
.status-value.status--pending { background: rgba(240,140,0,0.08); color: #f08c00; }
.status-value.status--paid { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.status-value.status--shipped { background: rgba(124,58,237,0.08); color: #7c3aed; }
.status-value.status--completed { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.status-value.status--cancelled { background: rgba(134,142,150,0.08); color: #868e96; }
.status-value.status--refunded { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 收货地址 ===== */
.address-card {
  padding: 16px 20px; background: #f8f9fa; border-radius: 12px;
}
.address-name { font-size: 1rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.address-phone { font-size: 0.85rem; color: #495057; margin-bottom: 8px; }
.address-detail { font-size: 0.85rem; color: #868e96; }

/* ===== 操作按钮 ===== */
.action-section {
  display: flex; gap: 12px; flex-wrap: wrap; margin-top: 24px; padding-top: 20px;
  border-top: 1px solid #dee2e6;
}
.action-btn {
  padding: 12px 28px; border: none; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn--primary {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: white;
}
.action-btn--primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.action-btn--cancel {
  background: #f1f3f5; color: #c92a2a;
}
.action-btn--cancel:hover:not(:disabled) { background: #e9ecef; }
.action-btn--refund {
  background: linear-gradient(135deg, #f08c00, #fab005); color: white;
}
.action-btn--refund:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(240,140,0,0.25); }
.action-btn--outline {
  border: 1px solid #dee2e6; background: transparent; color: #495057;
}
.action-btn--outline:hover { border-color: #4a9eff; color: #4a9eff; }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 80px 0; color: #868e96; }
.loading-spinner {
  width: 40px; height: 40px; margin: 0 auto 12px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-state h2 { font-family: 'Playfair Display', Georgia, serif; color: #212529; margin-bottom: 8px; }
.retry-btn {
  margin-top: 16px; padding: 10px 24px; border: 1px solid #2b6cb0; border-radius: 50px;
  background: transparent; color: #2b6cb0; font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.3s;
}
.retry-btn:hover { background: rgba(43,108,176,0.06); }

/* ===== 消息提示 ===== */
.msg { margin-top: 16px; text-align: center; font-size: 0.85rem; padding: 10px; border-radius: 10px; animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: #2b8a3e; background: rgba(43,138,62,0.06); border: 1px solid rgba(43,138,62,0.12); }
.msg--error { color: #c92a2a; background: rgba(201,42,42,0.06); border: 1px solid rgba(201,42,42,0.12); }

/* ===== 底部 ===== */
.detail-footer { position: relative; z-index: 1; padding: 24px 56px; text-align: center; }
.detail-footer span { font-size: 0.7rem; color: #adb5bd; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .detail-card { padding: 32px 20px; }
  .status-section { flex-direction: column; gap: 12px; align-items: flex-start; }
  .item-row { flex-wrap: wrap; }
  .item-subtotal { margin-left: auto; }
  .action-section { flex-direction: column; }
  .action-btn { width: 100%; text-align: center; }
}
</style>