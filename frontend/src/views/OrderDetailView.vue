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
      <div class="nav-actions">
        <button class="nav-btn" @click="goBack">返回订单列表</button>
        <button class="nav-btn nav-btn--logout" @click="handleLogout">退出</button>
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder, refundOrder } from '../api/index.js'

const route = useRoute()
const router = useRouter()

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
      order.value = res.data
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
      showMessage(res.mes || '支付成功', true)
      fetchOrderDetail()
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
async function handleCancel() {
  if (!confirm('确定要取消该订单吗？')) return
  actionLoading.value = true
  try {
    const res = await cancelOrder(order.value.id)
    if (res && res.code === 200) {
      showMessage('订单已取消', true)
      fetchOrderDetail()
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
async function handleRefund() {
  if (!confirm('确定要申请退款吗？')) return
  actionLoading.value = true
  try {
    const res = await refundOrder(order.value.id)
    if (res && res.code === 200) {
      showMessage('退款申请已提交', true)
      fetchOrderDetail()
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
 * 获取订单状态文本
 */
function getOrderStatusText(status) {
  const map = {
    0: '待支付', 1: '已支付', 2: '已发货',
    3: '已完成', 4: '已取消', 5: '已退款'
  }
  return map[status] !== undefined ? map[status] : '未知'
}

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
function goBack() { router.push('/orders') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  fetchOrderDetail()
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