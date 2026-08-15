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

    <!-- 主内容 -->
    <main class="coupons-main">
      <!-- 顶部标题 -->
      <header class="coupons-header">
        <div class="coupons-header__inner">
          <div class="coupons-icon">🎟️</div>
          <div class="coupons-title-box">
            <h1 class="coupons-title">我的优惠券</h1>
            <p class="coupons-desc">
              共 {{ coupons.length }} 张
              <span v-if="isVip" class="vip-chip">★ 会员买家</span>
            </p>
          </div>
        </div>
      </header>

      <!-- 状态筛选 Tabs -->
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
        <p class="empty-title">{{ activeFilter === 0 ? '暂无可用优惠券' : '该分类暂无优惠券' }}</p>
        <p class="empty-hint" v-if="activeFilter === 0">去商城逛逛，领取更多惊喜优惠</p>
        <button class="retry-btn" @click="goHome">去逛逛</button>
      </div>

      <!-- 优惠券列表 -->
      <div class="coupon-list" v-else>
        <div v-for="c in filteredCoupons" :key="c.id" class="coupon-card"
             :class="{ 'coupon-card--disabled': c.status !== 0 }">
          <!-- 状态角标 -->
          <span class="coupon-corner" :class="'coupon-corner--' + c.status">{{ statusText(c.status) }}</span>

          <!-- 左侧面值 -->
          <div class="coupon-value" :class="'coupon-value--' + c.status">
            <template v-if="c.type === 2">
              <div class="coupon-num-wrap">
                <span class="coupon-num">{{ displayValue(c) }}</span>
                <span class="coupon-type">折</span>
              </div>
              <span class="coupon-cap">优惠折扣</span>
            </template>
            <template v-else>
              <div class="coupon-num-wrap">
                <span class="coupon-symbol">¥</span>
                <span class="coupon-num">{{ displayValue(c) }}</span>
              </div>
              <span class="coupon-cap">立减优惠</span>
            </template>
          </div>

          <!-- 撕票分隔线 -->
          <div class="coupon-divider">
            <span class="coupon-notch coupon-notch--top"></span>
            <span class="coupon-notch coupon-notch--bottom"></span>
          </div>

          <!-- 右侧内容 -->
          <div class="coupon-info">
            <div class="coupon-name-row">
              <p class="coupon-name">{{ c.name }}</p>
              <span v-if="c.targetType === 2" class="coupon-vip-chip">VIP券</span>
            </div>
            <p class="coupon-threshold">满 ¥{{ Number(c.minAmount || 0).toFixed(2) }} 可用</p>
            <p class="coupon-expire">
              <template v-if="c.status === 0">有效期至 {{ formatDate(c.expireTime) }}</template>
              <template v-else>{{ statusText(c.status) }}</template>
            </p>
            <div class="coupon-action">
              <button v-if="c.status === 0" class="coupon-use-btn" @click="goHome">去使用 ›</button>
              <span v-else class="coupon-stamp" :class="'coupon-stamp--' + c.status">{{ statusText(c.status) }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部 -->
    <footer class="coupons-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllCoupons, getUserInfo } from '../api/index.js'
import { getCachedRoles } from '../utils/auth.js'

const router = useRouter()

const coupons = ref([])
const loading = ref(true)
const activeFilter = ref(0)
const isVip = ref(false)
const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'
const avatarUrl = ref(DEFAULT_AVATAR)
const userBalance = ref(null)
const profileMenuOpen = ref(false)
const nickname = ref(localStorage.getItem('nickname') || '用户')

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

function closePopups() { profileMenuOpen.value = false }

function goProfile() { router.push('/profile') }

function goHome() { router.push('/home') }
function goOrders() { router.push('/orders') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  fetchCoupons()
  fetchProfile()
  fetchUserProfile()
  document.addEventListener('click', closePopups)
})

onUnmounted(() => { document.removeEventListener('click', closePopups) })
</script>

<style scoped>
/* ===== 全局 ===== */
.coupons-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--bg);
  font-family: var(--font-sans);
  position: relative;
  overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-circle { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.18; }
.bg-circle--1 { width: 560px; height: 560px; top: -180px; right: -160px; background: radial-gradient(circle, var(--primary), transparent 70%); }
.bg-circle--2 { width: 480px; height: 480px; bottom: -160px; left: -160px; background: radial-gradient(circle, var(--accent), transparent 70%); }
.bg-circle--3 { width: 320px; height: 320px; top: 42%; left: 12%; background: radial-gradient(circle, var(--primary), transparent 70%); }

/* ===== 导航 ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0; height: 68px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 32px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(18px) saturate(1.6);
  -webkit-backdrop-filter: blur(18px) saturate(1.6);
  border-bottom: 1px solid var(--border);
}
.nav-logo {
  font-family: var(--font-display); font-size: 1.35rem; font-weight: 700;
  background: linear-gradient(120deg, var(--primary-2), var(--primary) 55%, var(--accent));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.nav-actions { display: flex; gap: 12px; }
.nav-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border: none; border-radius: var(--radius-sm);
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff; font-family: var(--font-sans); font-size: 0.8rem; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
}
.nav-btn:hover { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.nav-btn--outline { background: transparent; color: var(--text-2); border: 1px solid var(--border-strong); }
.nav-btn--outline:hover { background: var(--danger-soft); border-color: var(--danger); color: var(--danger); box-shadow: none; }

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

/* ===== 主内容 ===== */
.coupons-main {
  width: 100%;
  max-width: 780px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 96px 20px 40px;
  position: relative;
  z-index: 1;
}

/* ===== 顶部标题 ===== */
.coupons-header {
  background: linear-gradient(120deg, var(--primary-2) 0%, var(--primary) 60%, #7c5cff 100%);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-primary);
  padding: 22px 26px;
  color: #fff;
  animation: cardIn 0.5s ease both;
}
.coupons-header__inner { display: flex; align-items: center; gap: 16px; }
.coupons-icon {
  width: 56px; height: 56px; flex-shrink: 0;
  border-radius: var(--radius);
  background: rgba(255,255,255,0.18);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem;
}
.coupons-title-box { min-width: 0; }
.coupons-title { font-family: var(--font-display); font-size: 1.5rem; font-weight: 700; margin: 0 0 2px; }
.coupons-desc {
  font-size: 0.85rem; color: rgba(255,255,255,0.85); margin: 0;
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.vip-chip {
  padding: 2px 12px; border-radius: var(--radius-pill);
  background: linear-gradient(120deg, #ffd76a, #ff9f43);
  color: #7a3b00; font-size: 0.72rem; font-weight: 700;
}

/* ===== 状态筛选 ===== */
.filter-tabs {
  display: flex; gap: 10px;
  padding: 6px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  animation: cardIn 0.5s 0.05s ease both;
}
.filter-tab {
  flex: 1;
  padding: 10px 16px;
  border: none; border-radius: var(--radius-pill);
  background: transparent; color: var(--text-3);
  font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
  font-family: var(--font-sans);
  white-space: nowrap;
}
.filter-tab:hover { color: var(--primary); }
.filter-tab.active {
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff;
  box-shadow: var(--shadow-primary);
}

/* ===== 优惠券列表 ===== */
.coupon-list { display: flex; flex-direction: column; gap: 16px; }
.coupon-card {
  position: relative;
  display: flex;
  align-items: stretch;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  overflow: visible;
  transition: all 0.25s ease;
  animation: cardIn 0.5s ease both;
}
.coupon-card:hover { transform: translateY(-2px); box-shadow: var(--shadow); }
.coupon-card--disabled { opacity: 0.78; }
.coupon-card--disabled:hover { transform: none; }

/* 状态角标 */
.coupon-corner {
  position: absolute;
  top: -1px; right: 16px;
  z-index: 2;
  padding: 4px 14px;
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
  font-size: 0.72rem; font-weight: 700;
  letter-spacing: 0.04em;
}
.coupon-corner--0 { background: linear-gradient(120deg, var(--primary) 0%, var(--accent) 100%); color: #fff; }
.coupon-corner--1 { background: var(--surface-3); color: var(--text-3); }
.coupon-corner--2 { background: var(--surface-3); color: var(--text-3); }

/* 左侧面值 */
.coupon-value {
  flex-shrink: 0;
  width: 128px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  padding: 22px 12px;
  border-radius: var(--radius) 0 0 var(--radius);
  background: linear-gradient(150deg, rgba(47,84,235,0.10) 0%, rgba(19,194,194,0.08) 100%);
}
.coupon-value--0 { color: var(--primary); }
.coupon-value--1 { color: var(--text-3); }
.coupon-value--2 { color: var(--text-4); }
.coupon-num-wrap { display: flex; align-items: baseline; gap: 2px; line-height: 1; }
.coupon-symbol { font-size: 1rem; font-weight: 700; }
.coupon-num {
  font-family: var(--font-display);
  font-size: 2.3rem; font-weight: 700;
}
.coupon-type { font-size: 1.1rem; font-weight: 700; }
.coupon-cap { font-size: 0.72rem; color: var(--text-3); }

/* 撕票分隔线 */
.coupon-divider {
  position: relative;
  width: 0;
  border-left: 1.5px dashed var(--border-strong);
  flex-shrink: 0;
}
.coupon-notch {
  position: absolute;
  left: -10px;
  width: 19px; height: 19px;
  border-radius: 50%;
  background: var(--bg);
  border: 1px solid var(--border);
}
.coupon-notch--top { top: -10px; }
.coupon-notch--bottom { bottom: -10px; }

/* 右侧内容 */
.coupon-info {
  flex: 1;
  min-width: 0;
  display: flex; flex-direction: column;
  justify-content: center;
  padding: 20px 18px 20px 20px;
}
.coupon-name-row {
  display: flex; align-items: center; gap: 8px; margin: 0 0 6px;
}
.coupon-name {
  font-size: 1rem; font-weight: 700; color: var(--text);
  margin: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.coupon-vip-chip {
  flex-shrink: 0; padding: 2px 8px; border-radius: 999px;
  background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff;
  font-size: 0.66rem; font-weight: 600; line-height: 1.4;
}
.coupon-threshold { font-size: 0.82rem; color: var(--text-3); margin: 0 0 4px; }
.coupon-expire { font-size: 0.75rem; color: var(--text-4); margin: 0 0 12px; }
.coupon-action { margin-top: auto; }
.coupon-use-btn {
  padding: 7px 18px;
  border: none; border-radius: var(--radius-pill);
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff;
  font-family: var(--font-sans); font-size: 0.8rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.coupon-use-btn:hover { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.coupon-stamp {
  display: inline-block;
  padding: 5px 16px;
  border: 1.5px dashed var(--border-strong);
  border-radius: var(--radius-pill);
  font-size: 0.78rem; font-weight: 700;
  letter-spacing: 0.08em;
  transform: rotate(-4deg);
}
.coupon-stamp--1 { color: var(--text-3); border-color: var(--border-strong); }
.coupon-stamp--2 { color: var(--text-4); border-color: var(--border); }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state {
  text-align: center;
  padding: 60px 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  color: var(--text-3);
  animation: cardIn 0.5s ease both;
}
.loading-spinner {
  width: 36px; height: 36px; margin: 0 auto 12px;
  border: 3px solid var(--border); border-top-color: var(--primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-title { font-size: 1rem; font-weight: 600; color: var(--text-2); }
.empty-hint { font-size: 0.85rem; color: var(--text-3); margin-top: 6px; }
.retry-btn {
  margin-top: 18px; padding: 10px 28px;
  border: 1px solid var(--primary); border-radius: var(--radius-pill);
  background: transparent; color: var(--primary);
  font-family: var(--font-sans); font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
}
.retry-btn:hover { background: var(--primary-soft); }

/* ===== 底部 ===== */
.coupons-footer { position: relative; z-index: 1; padding: 8px 0 28px; text-align: center; }
.coupons-footer span { font-size: 0.72rem; color: var(--text-4); }

@keyframes cardIn { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 640px) {
  .nav { padding: 0 16px; }
  .coupons-main { padding: 84px 14px 32px; }
  .coupon-card { flex-direction: column; }
  .coupon-value {
    width: 100%;
    flex-direction: row;
    border-radius: var(--radius) var(--radius) 0 0;
    padding: 16px 12px;
    gap: 8px;
  }
  .coupon-divider {
    width: 100%;
    height: 0;
    border-left: none;
    border-top: 1.5px dashed var(--border-strong);
  }
  .coupon-notch--top { top: -10px; left: 20%; }
  .coupon-notch--bottom { top: -10px; left: 80%; }
  .coupon-corner { right: 8px; }
  .coupon-info { padding: 16px; }
  .nav-btn-text { display: none; }
}
</style>
