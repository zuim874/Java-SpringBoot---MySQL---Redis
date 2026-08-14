<template>
  <div class="coupons-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-actions">
        <button class="nav-btn" @click="goHome">
          <span class="nav-btn-icon">🏪</span>
          <span class="nav-btn-text">返回商城</span>
        </button>
        <button class="nav-btn nav-btn--outline" @click="handleLogout">
          <span class="nav-btn-icon">🚪</span>
          <span class="nav-btn-text">退出登录</span>
        </button>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="coupons-wrapper">
      <div class="coupons-card">
        <div class="coupons-header">
          <div class="coupons-icon">🎟️</div>
          <h2 class="coupons-title">我的优惠券</h2>
          <p class="coupons-desc">
            共 {{ coupons.length }} 张
            <span v-if="isVip" class="vip-chip">★ 会员买家</span>
          </p>
        </div>

        <!-- 状态筛选 -->
        <div class="filter-tabs">
          <button v-for="f in filters" :key="f.value"
                  class="filter-tab" :class="{ active: activeFilter === f.value }"
                  @click="activeFilter = f.value">
            {{ f.label }}（{{ countBy(f.value) }}）
          </button>
        </div>

        <!-- 加载中 -->
        <div class="loading-state" v-if="loading">
          <div class="loading-spinner"></div>
          <p>加载优惠券...</p>
        </div>

        <!-- 空状态 -->
        <div class="empty-state" v-else-if="filteredCoupons.length === 0">
          <div class="empty-icon">🎫</div>
          <p>{{ activeFilter === 0 ? '暂无可用优惠券，去商城逛逛吧' : '该分类暂无优惠券' }}</p>
          <button class="retry-btn" @click="goHome">去逛逛</button>
        </div>

        <!-- 优惠券列表 -->
        <div class="coupon-list" v-else>
          <div v-for="c in filteredCoupons" :key="c.id" class="coupon-card"
               :class="{ 'coupon-card--disabled': c.status !== 0 }">
            <div class="coupon-value">
              <span class="coupon-symbol">¥</span>
              <span class="coupon-num">{{ displayValue(c) }}</span>
              <span class="coupon-type">{{ c.type === 2 ? '折' : '' }}</span>
            </div>
            <div class="coupon-info">
              <p class="coupon-name">{{ c.name }}</p>
              <p class="coupon-threshold">满 ¥{{ Number(c.minAmount || 0).toFixed(2) }} 可用</p>
              <p class="coupon-expire">
                {{ statusText(c.status) }}
                <template v-if="c.status === 0">· 有效期至 {{ formatDate(c.expireTime) }}</template>
              </p>
            </div>
            <div class="coupon-status">
              <span :class="['coupon-badge', 'coupon-badge--' + c.status]">{{ statusText(c.status) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <div class="coupons-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllCoupons, getUserInfo } from '../api/index.js'

const router = useRouter()

const coupons = ref([])
const loading = ref(true)
const activeFilter = ref(0)
const isVip = ref(false)

const filters = [
  { value: 0, label: '可用' },
  { value: 1, label: '已使用' },
  { value: 2, label: '已过期' }
]

function countBy(status) {
  return coupons.value.filter(c => c.status === status).length
}

const filteredCoupons = computed(() => coupons.value.filter(c => c.status === activeFilter.value))

function statusText(status) {
  if (status === 0) return '可用'
  if (status === 1) return '已使用'
  return '已过期'
}

// 满减显示金额，折扣显示折数
function displayValue(c) {
  if (c.type === 2) return Number(c.discountValue).toFixed(1)
  return Number(c.discountValue).toFixed(0)
}

function formatDate(t) {
  if (!t) return ''
  try {
    return new Date(t).toLocaleDateString('zh-CN')
  } catch {
    return ''
  }
}

async function fetchCoupons() {
  loading.value = true
  try {
    const res = await getAllCoupons()
    if (res && res.code === 200) {
      coupons.value = res.data || []
    }
  } catch {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

async function fetchProfile() {
  try {
    const res = await getUserInfo()
    if (res && res.code === 200 && res.data) {
      const role = res.data.userRole || ''
      isVip.value = role.includes('ROLE_VIP_USER')
    }
  } catch {
    // ignore
  }
}

function goHome() { router.push('/home') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  fetchCoupons()
  fetchProfile()
})
</script>

<style scoped>
/* ===== 全局 ===== */
.coupons-page {
  min-height: 100vh; display: flex; flex-direction: column; align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif; position: relative; overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-circle { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35; }
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
}
.nav-actions { display: flex; gap: 12px; }
.nav-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: #fff; font-family: 'DM Sans', sans-serif; font-size: 0.8rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.nav-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.nav-btn--outline { background: transparent; color: #495057; border: 1px solid #dee2e6; }
.nav-btn--outline:hover { background: rgba(201,42,42,0.06); border-color: #c92a2a; color: #c92a2a; box-shadow: none; }

/* ===== 主卡片 ===== */
.coupons-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.coupons-card {
  width: 720px; max-width: 100%; padding: 40px 36px;
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06);
  animation: cardIn 0.6s both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.coupons-header { text-align: center; margin-bottom: 24px; }
.coupons-icon { font-size: 2.2rem; margin-bottom: 10px; }
.coupons-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.7rem; font-weight: 700; color: #212529; margin-bottom: 6px; }
.coupons-desc { font-size: 0.9rem; color: #868e96; display: flex; align-items: center; justify-content: center; gap: 8px; }
.vip-chip {
  padding: 2px 12px; border-radius: 50px;
  background: linear-gradient(135deg, #7c3aed, #4a9eff);
  color: #fff; font-size: 0.72rem; font-weight: 700;
}

/* ===== 筛选 ===== */
.filter-tabs { display: flex; gap: 10px; justify-content: center; margin-bottom: 24px; flex-wrap: wrap; }
.filter-tab {
  padding: 8px 20px; border: 1px solid #dee2e6; border-radius: 50px;
  background: transparent; color: #868e96; font-size: 0.82rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.filter-tab:hover { border-color: #4a9eff; color: #4a9eff; }
.filter-tab.active { background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); border-color: transparent; color: #fff; }

/* ===== 列表 ===== */
.coupon-list { display: flex; flex-direction: column; gap: 14px; }
.coupon-card {
  display: flex; align-items: center; gap: 18px;
  padding: 18px 20px; border-radius: 16px;
  background: #fff; border: 1px solid #f1f3f5;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
  transition: all 0.3s;
}
.coupon-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(43,108,176,0.08); }
.coupon-card--disabled { opacity: 0.55; }
.coupon-value {
  min-width: 110px; text-align: center;
  padding: 12px 8px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(43,108,176,0.08), rgba(124,58,237,0.08));
  border: 1px dashed rgba(43,108,176,0.25);
  color: #2b6cb0;
}
.coupon-symbol { font-size: 0.9rem; font-weight: 700; }
.coupon-num { font-family: 'Playfair Display', Georgia, serif; font-size: 2rem; font-weight: 700; }
.coupon-type { font-size: 0.9rem; font-weight: 700; }
.coupon-info { flex: 1; min-width: 0; }
.coupon-name { font-size: 0.98rem; font-weight: 600; color: #212529; margin: 0 0 4px; }
.coupon-threshold { font-size: 0.8rem; color: #868e96; margin: 0 0 4px; }
.coupon-expire { font-size: 0.72rem; color: #adb5bd; margin: 0; }
.coupon-status { flex-shrink: 0; }
.coupon-badge { display: inline-block; padding: 3px 12px; border-radius: 50px; font-size: 0.75rem; font-weight: 600; }
.coupon-badge--0 { background: rgba(43,138,62,0.1); color: #2b8a3e; }
.coupon-badge--1 { background: rgba(134,142,150,0.1); color: #868e96; }
.coupon-badge--2 { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 60px 0; color: #868e96; }
.loading-spinner {
  width: 36px; height: 36px; margin: 0 auto 12px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 3rem; margin-bottom: 12px; }
.retry-btn {
  margin-top: 16px; padding: 10px 24px; border: 1px solid #2b6cb0; border-radius: 50px;
  background: transparent; color: #2b6cb0; font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.3s;
}
.retry-btn:hover { background: rgba(43,108,176,0.06); }

/* ===== 底部 ===== */
.coupons-footer { position: relative; z-index: 1; padding: 24px 56px; text-align: center; }
.coupons-footer span { font-size: 0.7rem; color: #adb5bd; }

@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .coupons-card { padding: 28px 20px; }
  .nav-btn-text { display: none; }
}
</style>
