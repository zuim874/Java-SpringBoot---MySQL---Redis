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
        <button class="nav-btn" @click="goHome">返回商城</button>
        <button class="nav-btn nav-btn--profile" @click="goProfile">个人中心</button>
        <button class="nav-btn nav-btn--logout" @click="handleLogout">退出</button>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="orders-wrapper">
      <div class="orders-card">
        <div class="orders-header">
          <div class="orders-icon">📋</div>
          <h2 class="orders-title">我的订单</h2>
          <p class="orders-desc">查看和管理您的所有订单</p>
        </div>

        <!-- 状态筛选标签 -->
        <div class="status-tabs">
          <button v-for="tab in statusTabs" :key="tab.value"
                  class="status-tab" :class="{ active: activeStatus === tab.value }"
                  @click="switchStatus(tab.value)">
            {{ tab.label }}
          </button>
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
              <span class="order-status" :class="'status--' + (order.status || 'UNKNOWN')">
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
                        v-if="order.status === 'PENDING'"
                        :disabled="actionLoading">立即支付</button>
                <button class="action-btn action-btn--cancel" @click="handleCancel(order)"
                        v-if="order.status === 'PENDING'"
                        :disabled="actionLoading">取消订单</button>
                <button class="action-btn action-btn--refund" @click="handleRefund(order)"
                        v-if="order.status === 'PAID' || order.status === 'SHIPPED'"
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

    <!-- 底部 -->
    <div class="orders-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserOrders, payOrder, cancelOrder, refundOrder } from '../api/index.js'

const router = useRouter()

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

// 状态标签选项
const statusTabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDING', label: '退款中' }
]

/**
 * 切换状态标签
 */
function switchStatus(status) {
  activeStatus.value = status
  currentPage.value = 1
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
      showMessage(res.mes || '支付成功', true)
      fetchOrders()
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
      showMessage('订单已取消', true)
      fetchOrders()
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
      showMessage('退款申请已提交', true)
      fetchOrders()
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
    'PENDING': '待支付', 'PAID': '已支付', 'SHIPPED': '已发货',
    'COMPLETED': '已完成', 'CANCELLED': '已取消', 'REFUNDING': '退款中'
  }
  return map[status] || status || '未知'
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

onMounted(() => {
  fetchOrders()
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
.nav-actions { display: flex; gap: 12px; }
.nav-btn {
  padding: 8px 20px; border: 1px solid rgba(43,108,176,0.20); border-radius: 10px;
  background: rgba(255,255,255,0.60); font-family: 'DM Sans', sans-serif;
  font-size: 0.8rem; font-weight: 600; color: #2b6cb0; cursor: pointer;
  transition: all 0.3s;
}
.nav-btn:hover { background: #2b6cb0; color: white; border-color: #2b6cb0; }
.nav-btn--profile { color: #7c3aed; border-color: rgba(124,58,237,0.2); }
.nav-btn--profile:hover { background: #7c3aed; color: white; border-color: #7c3aed; }
.nav-btn--logout { color: #c92a2a; border-color: rgba(201,42,42,0.20); }
.nav-btn--logout:hover { background: #c92a2a; color: white; border-color: #c92a2a; }

/* ===== 主卡片 ===== */
.orders-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.orders-card {
  width: 900px; max-width: 100%; padding: 48px 40px;
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.orders-header { text-align: center; margin-bottom: 32px; }
.orders-icon { font-size: 2rem; margin-bottom: 12px; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.1); opacity: 1; } }
.orders-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700; color: #212529; margin-bottom: 8px; }
.orders-desc { font-size: 0.9rem; color: #868e96; }

/* ===== 状态标签 ===== */
.status-tabs {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; justify-content: center;
}
.status-tab {
  padding: 8px 18px; border: 1px solid #dee2e6; border-radius: 50px;
  background: transparent; color: #868e96; font-size: 0.8rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.status-tab:hover { border-color: #4a9eff; color: #4a9eff; }
.status-tab.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  border-color: transparent; color: white;
}

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
.order-status.status--PENDING { background: rgba(240,140,0,0.08); color: #f08c00; }
.order-status.status--PAID { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.order-status.status--SHIPPED { background: rgba(124,58,237,0.08); color: #7c3aed; }
.order-status.status--COMPLETED { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.order-status.status--CANCELLED { background: rgba(134,142,150,0.08); color: #868e96; }
.order-status.status--REFUNDING { background: rgba(201,42,42,0.08); color: #c92a2a; }

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
  .orders-card { padding: 32px 20px; }
  .order-header { flex-direction: column; gap: 8px; align-items: flex-start; }
  .order-footer { flex-direction: column; align-items: flex-start; }
  .order-actions { width: 100%; }
  .action-btn { flex: 1; text-align: center; }
}
</style>