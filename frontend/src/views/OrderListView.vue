<template>
  <div class="orders-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo" @click="goHome">ZuiMShop</div>
      <div class="nav-actions">
        <!-- 头像组件 -->
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
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="orders-wrapper">
      <div class="orders-layout">
        <!-- 左侧纵向筛选 -->
        <div class="status-sidebar">
          <div class="sidebar-header">
            <div class="sidebar-icon">📋</div>
            <h3>订单筛选</h3>
          </div>
          <button v-for="tab in statusTabs" :key="tab.value"
                  class="sidebar-tab" :class="{ active: activeStatus === tab.value }"
                  @click="switchStatus(tab.value)">
            <span class="sidebar-tab-dot" :class="'dot--' + getStatusDotClass(tab.value)"></span>
            {{ tab.label }}
          </button>
        </div>

        <!-- 右侧订单列表 -->
        <div class="orders-content">
          <div class="orders-card">
            <div class="orders-header">
              <div class="orders-icon">📋</div>
              <h2 class="orders-title">我的订单</h2>
              <p class="orders-desc">查看和管理您的所有订单</p>
            </div>

            <!-- 加载中 -->
            <div class="loading-state" v-if="loading">
              <div class="loading-spinner"></div>
              <p>加载订单中...</p>
            </div>

            <!-- 错误状态 -->
            <div class="empty-state" v-else-if="error">
              <div class="error-icon">⚠️</div>
              <p>订单加载失败</p>
              <button class="retry-btn" @click="fetchOrders">重新加载</button>
            </div>

            <!-- 空订单 -->
            <div class="empty-state" v-else-if="orders.length === 0">
              <div class="empty-icon">📭</div>
              <p>暂无订单</p>
              <p class="empty-hint">快去商城挑选心仪的商品吧</p>
              <button class="retry-btn" @click="goHome">去逛逛</button>
            </div>

            <!-- 订单列表 -->
            <div class="order-list" v-else>
              <div v-for="order in orders" :key="order.id" class="order-card" @click="goToOrderDetail(order.id)">
                <!-- 订单头部 -->
                <div class="order-header">
                  <div class="order-meta">
                    <span class="order-no">订单号: {{ order.orderNo || order.id }}</span>
                    <span class="order-time">{{ formatDate(order.createTime) }}</span>
                  </div>
                  <span class="order-status" :class="getOrderStatusClass(order.status)">
                    {{ getOrderStatusText(order.status) }}
                  </span>
                </div>

                <!-- 订单商品列表 -->
                <div class="order-items">
                  <div v-for="item in (order.items || order.orderItems || [])" :key="item.id" class="order-item">
                    <img :src="item.mainImageUrl || item.image || '/uploads/hero/hero-1.jpg'"
                         :alt="item.productName" class="item-image">
                    <div class="item-info">
                      <h4>{{ item.productName }}</h4>
                      <p class="item-price">¥{{ (item.price || 0).toFixed(2) }}</p>
                    </div>
                    <span class="item-qty">x{{ item.quantity || item.qty || 1 }}</span>
                  </div>
                  <!-- 如果 items 为空，显示商品名 -->
                  <div v-if="!order.items && !order.orderItems && order.productName" class="order-item">
                    <div class="item-info">
                      <h4>{{ order.productName }}</h4>
                    </div>
                  </div>
                </div>

                <!-- 订单底部 -->
                <div class="order-footer">
                  <div class="order-total">
                    <span>共 {{ getItemCount(order) }} 件商品</span>
                    <span class="total-amount">合计: ¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
                  </div>
                  <div class="order-actions" @click.stop>
                    <button class="action-btn" @click="handlePay(order)"
                            v-if="order.status === 0"
                            :disabled="actionLoading">立即支付</button>
                    <button class="action-btn action-btn--cancel" @click="handleCancel(order)"
                            v-if="order.status === 0"
                            :disabled="actionLoading">取消订单</button>
                    <button class="action-btn action-btn--refund" @click="handleRefund(order)"
                            v-if="order.status === 1 || order.status === 2"
                            :disabled="actionLoading">申请退款</button>
                    <button class="action-btn action-btn--detail" @click="goToOrderDetail(order.id)">查看详情</button>
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
        </div>
      </div>
    </div>

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

const roleLabel = computed(() => {
  const roles = ['普通用户']
  return roles[0]
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
    showMessage('网络错误', false)
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
    showMessage('网络错误', false)
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
    showMessage('网络错误', false)
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
function goProfile() { router.push('/profile') }
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
/* ===== 全局 ===== */
.orders-page {
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
.nav-actions { display: flex; gap: 12px; align-items: center; }

/* ===== 用户头像与下拉菜单 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 6px; }
.nav-balance {
  padding: 5px 12px; border-radius: 50px;
  background: rgba(43,108,176,0.08); border: 1px solid rgba(43,108,176,0.15);
  color: #2b6cb0; font-size: 0.75rem; font-weight: 600;
  white-space: nowrap;
}
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; padding: 0 4px; }
.avatar-btn {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none; cursor: pointer; padding: 2px;
  transition: transform 0.3s;
}
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 38px; height: 38px; border-radius: 50%;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif;
  font-size: 1rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(43,108,176,0.25);
  overflow: hidden;
}
.avatar-circle img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-caret {
  font-size: 0.7rem; color: #868e96; transition: transform 0.3s;
}
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 14px); right: 0;
  min-width: 180px; padding: 8px;
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.60);
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.10);
}
.profile-menu-header {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-bottom: 1px solid #f1f3f5; margin-bottom: 6px;
}
.profile-menu-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif;
  font-size: 1.05rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; overflow: hidden;
}
.profile-menu-avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name {
  font-size: 0.9rem; font-weight: 600; color: #212529; margin: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.profile-menu-role { font-size: 0.72rem; color: #adb5bd; margin: 2px 0 0; }
.profile-menu-item {
  display: block; width: 100%; text-align: left;
  padding: 10px 14px; border: none; border-radius: 8px;
  background: transparent; cursor: pointer;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem;
  color: #495057; transition: all 0.2s;
}
.profile-menu-item:hover { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.profile-menu-item--logout:hover { background: rgba(220,53,69,0.06); color: #dc3545; }
.profile-menu-divider { height: 1px; background: #f1f3f5; margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主卡片 ===== */
.orders-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.orders-layout {
  display: flex; gap: 24px; max-width: 1200px; width: 100%;
  align-items: flex-start;
}
/* 左侧状态筛选侧边栏 */
.status-sidebar {
  width: 220px; flex-shrink: 0;
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 20px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  padding: 24px 16px;
  position: sticky; top: 100px;
  animation: cardIn 0.6s both;
}
.sidebar-header {
  display: flex; align-items: center; gap: 10px;
  padding-bottom: 16px; margin-bottom: 12px;
  border-bottom: 1px solid #f1f3f5;
}
.sidebar-icon { font-size: 1.4rem; }
.sidebar-header h3 {
  margin: 0; font-size: 1.1rem; font-weight: 700; color: #212529;
  font-family: 'DM Sans', sans-serif;
}
.sidebar-tab {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 12px 14px; margin-bottom: 6px;
  border: none; border-radius: 10px;
  background: transparent; color: #495057; cursor: pointer;
  text-align: left; font-family: 'DM Sans', sans-serif; font-size: 0.85rem;
  transition: all 0.2s;
}
.sidebar-tab:hover { background: rgba(43,108,176,0.06); color: #2b6cb0; }
.sidebar-tab.active {
  background: linear-gradient(135deg, rgba(43,108,176,0.08), rgba(124,58,237,0.08));
  color: #2b6cb0; border: 1px solid rgba(43,108,176,0.2);
}
.sidebar-tab-dot {
  width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
}
/* 状态点颜色对应 */
.sidebar-tab-dot.dot--all { background: #495057; }
.sidebar-tab-dot.dot--pending { background: #f08c00; }
.sidebar-tab-dot.dot--paid { background: #2b6cb0; }
.sidebar-tab-dot.dot--shipped { background: #7c3aed; }
.sidebar-tab-dot.dot--completed { background: #2b8a3e; }
.sidebar-tab-dot.dot--cancelled { background: #868e96; }
.sidebar-tab-dot.dot--refunded { background: #c92a2a; }
.sidebar-tab-dot.dot--unknown { background: #adb5bd; }

/* 右侧内容区 */
.orders-content {
  flex: 1;
}
.orders-card {
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s both;
  padding: 48px 40px;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.orders-header { text-align: center; margin-bottom: 32px; }
.orders-icon { font-size: 2rem; margin-bottom: 12px; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.1); opacity: 1; } }
.orders-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700; color: #212529; margin-bottom: 8px; }
.orders-desc { font-size: 0.9rem; color: #868e96; }

/* 旧状态标签样式保留但隐藏（如果还需要的话） */
.status-tabs { display: none; }
.status-tab { display: none; }

/* ===== 订单卡片 ===== */
.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-card {
  background: white; border: 1px solid #dee2e6; border-radius: 16px;
  overflow: hidden; cursor: pointer; transition: all 0.3s;
}
.order-card:hover { box-shadow: 0 8px 32px rgba(0,0,0,0.06); border-color: transparent; }
.order-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; background: #f8f9fa; border-bottom: 1px solid #dee2e6;
}
.order-meta { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.order-no { font-size: 0.8rem; font-weight: 600; color: #495057; }
.order-time { font-size: 0.75rem; color: #adb5bd; }
.order-status {
  font-size: 0.8rem; font-weight: 600; padding: 4px 12px; border-radius: 20px;
}
.order-status.status--pending { background: rgba(240,140,0,0.08); color: #f08c00; }
.order-status.status--paid { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.order-status.status--shipped { background: rgba(124,58,237,0.08); color: #7c3aed; }
.order-status.status--completed { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.order-status.status--cancelled { background: rgba(134,142,150,0.08); color: #868e96; }
.order-status.status--refunded { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 订单商品 ===== */
.order-items { padding: 16px 20px; }
.order-item {
  display: flex; align-items: center; gap: 12px; padding: 8px 0;
  border-bottom: 1px solid #f1f3f5;
}
.order-item:last-child { border-bottom: none; }
.item-image { width: 56px; height: 56px; border-radius: 10px; object-fit: cover; background: #f8f9fa; }
.item-info { flex: 1; min-width: 0; }
.item-info h4 { font-size: 0.9rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.item-price { font-size: 0.85rem; font-weight: 600; color: #2b6cb0; }
.item-qty { font-size: 0.85rem; color: #868e96; }

/* ===== 订单底部 ===== */
.order-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-top: 1px solid #dee2e6; flex-wrap: wrap; gap: 12px;
}
.order-total { display: flex; gap: 16px; align-items: center; }
.order-total span { font-size: 0.8rem; color: #868e96; }
.total-amount { font-size: 1rem; font-weight: 700; color: #212529; }
.order-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.action-btn {
  padding: 8px 18px; border: 1px solid #dee2e6; border-radius: 8px;
  background: white; color: #495057; font-size: 0.78rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.action-btn:hover:not(:disabled) { border-color: #2b6cb0; color: #2b6cb0; }
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.action-btn--cancel { color: #c92a2a; }
.action-btn--cancel:hover:not(:disabled) { border-color: #c92a2a; }
.action-btn--refund { color: #f08c00; }
.action-btn--refund:hover:not(:disabled) { border-color: #f08c00; }
.action-btn--detail { background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: white; border-color: transparent; }
.action-btn--detail:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(43,108,176,0.25); }

/* ===== 分页 ===== */
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 32px;
}
.page-btn {
  padding: 10px 20px; border: 1px solid #dee2e6; border-radius: 10px;
  background: transparent; color: #495057; font-size: 0.85rem; cursor: pointer;
  transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.page-btn:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: #868e96; }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 60px 0; color: #868e96; }
.loading-spinner {
  width: 36px; height: 36px; margin: 0 auto 12px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-icon, .empty-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-hint { font-size: 0.85rem; color: #adb5bd; margin-top: 8px; }
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
.orders-footer { position: relative; z-index: 1; padding: 24px 56px; text-align: center; }
.orders-footer span { font-size: 0.7rem; color: #adb5bd; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .orders-layout { flex-direction: column; }
  .status-sidebar {
    width: 100%; position: static; display: flex; flex-wrap: wrap;
    gap: 8px; padding: 16px; overflow-x: auto;
  }
  .sidebar-header { display: none; }
  .sidebar-tab {
    flex: 0 0 auto; padding: 8px 14px; margin-bottom: 0;
    font-size: 0.8rem; border: 1px solid #dee2e6; border-radius: 50px;
  }
  .sidebar-tab.active {
    background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
    color: white; border-color: transparent;
  }
  .orders-card { padding: 32px 20px; }
  .order-header { flex-direction: column; gap: 8px; align-items: flex-start; }
  .order-footer { flex-direction: column; align-items: flex-start; }
  .order-actions { width: 100%; }
  .action-btn { flex: 1; text-align: center; }
  /* 移动端：头像菜单右对齐 */
  .avatar-wrapper { flex: 1; justify-content: flex-end; }
  .profile-menu { right: 0; }
}
</style>