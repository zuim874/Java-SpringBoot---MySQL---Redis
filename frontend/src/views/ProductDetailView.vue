<template>
  <div class="product-detail-page">
    <!-- ===== 顶部导航 ===== -->
    <nav class="top-nav" :class="{ scrolled: scrolled }">
      <div class="nav-inner">
        <div class="nav-left">
          <div class="nav-logo" @click="goHome">ZuiMShop</div>
          <div class="nav-breadcrumb" v-if="product">
            <span class="breadcrumb-arrow">›</span>
            <span class="breadcrumb-cat">{{ product.category }}</span>
            <span class="breadcrumb-arrow">›</span>
            <span class="breadcrumb-current">{{ product.productName }}</span>
          </div>
        </div>
        <div class="nav-right">
          <button class="nav-cart-btn" @click="goHome" aria-label="购物车">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
            </svg>
          </button>
          <div class="avatar-wrapper" @click.stop>
            <span class="nav-balance" v-if="userBalance !== null">¥{{ Number(userBalance).toFixed(2) }}</span>
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
      </div>
    </nav>

    <!-- ===== 加载状态 ===== -->
    <div class="loading-container" v-if="loading">
      <div class="spinner"></div>
      <p class="loading-text">正在加载商品信息...</p>
    </div>

    <!-- ===== 商品不存在 ===== -->
    <div class="error-container" v-else-if="!product">
      <div class="error-icon">📦</div>
      <h2 class="error-title">商品不存在或已下架</h2>
      <p class="error-desc">抱歉，您访问的商品可能已下架或不存在</p>
      <button class="btn btn--primary" @click="goHome">返回首页</button>
    </div>

    <!-- ===== 商品详情主体 ===== -->
    <div class="detail-main" v-else>
      <div class="detail-container">
        <!-- 左侧：图片画廊 -->
        <div class="gallery-section">
          <div class="main-image-wrap">
            <div class="main-image">
              <img :src="currentImage" :alt="product.productName" />
              <div class="image-zoom-hint">鼠标悬停查看细节</div>
            </div>
          </div>
          <div class="thumbnail-list" v-if="images.length > 0">
            <div v-for="(img, idx) in images" :key="img.id"
                 class="thumbnail-item"
                 :class="{ active: currentImageIndex === idx }"
                 @click="selectImage(idx)">
              <img :src="img.imageUrl" :alt="'图片 ' + (idx + 1)" />
            </div>
          </div>
        </div>

        <!-- 右侧：信息面板 -->
        <div class="info-panel">
          <!-- 分类标签 -->
          <div class="info-header">
            <span class="cat-tag">{{ product.category }}</span>
            <span class="badge badge-hot" v-if="product.sold > 100">热卖</span>
          </div>

          <!-- 商品名称 -->
          <h1 class="product-title">{{ product.productName }}</h1>

          <!-- 商品描述 -->
          <p class="product-desc">{{ product.description }}</p>

          <!-- 价格区 -->
          <div class="price-block">
            <div class="price-main">
              <span class="price-label">促销价</span>
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ product.price.toFixed(2) }}</span>
            </div>
            <span class="tag tag--price">限时优惠</span>
          </div>

          <!-- 统计信息 -->
          <div class="stats-row">
            <div class="stat-item">
              <span class="stat-num">{{ product.stock }}</span>
              <span class="stat-desc">库存</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ product.sold }}</span>
              <span class="stat-desc">已售</span>
            </div>
          </div>

          <!-- 数量选择器 -->
          <div class="qty-row">
            <span class="qty-label">数量</span>
            <div class="qty-selector">
              <button class="qty-btn" @click="decreaseQty" :disabled="quantity <= 1">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14"/></svg>
              </button>
              <span class="qty-value">{{ quantity }}</span>
              <button class="qty-btn" @click="increaseQty" :disabled="quantity >= product.stock">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
              </button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-row">
            <button class="btn-cart" @click="addToCart">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              <span>加入购物车</span>
            </button>
            <button class="btn-buy" @click="buyNow">立即购买</button>
          </div>

          <!-- 卖家店铺卡片 -->
          <div class="seller-card" v-if="product.seller">
            <div class="seller-card-top">
              <div class="seller-avatar-wrap">
                <img v-if="product.seller.sellerAvatar" :src="getCachedAvatar(product.seller.sellerAvatar)" :alt="product.seller.sellerName" class="seller-avatar-img" />
                <span v-else class="seller-avatar-placeholder">{{ product.seller.sellerName.charAt(0) }}</span>
              </div>
              <div class="seller-meta">
                <div class="seller-name-row">
                  <h4 class="seller-name">{{ product.seller.sellerName }}</h4>
                  <span class="seller-badge">已认证</span>
                </div>
                <p class="seller-address">{{ product.seller.address }}</p>
              </div>
            </div>
            <div class="seller-card-actions">
              <button class="seller-btn seller-btn--visit" @click="goHome">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                <span>进入店铺</span>
              </button>
              <button class="seller-btn seller-btn--chat" @click="openChat">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <span>联系客服</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 服务保障条 ===== -->
      <div class="guarantee-bar">
        <div class="guarantee-item">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          <span>正品保障</span>
        </div>
        <div class="guarantee-item">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
          <span>极速发货</span>
        </div>
        <div class="guarantee-item">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-9-9"/><path d="M12 6v6l4 2"/></svg>
          <span>七天无理由</span>
        </div>
        <div class="guarantee-item">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 12H4"/><path d="M16 16l4-4-4-4"/></svg>
          <span>售后无忧</span>
        </div>
      </div>

      <!-- ===== 商品详情区 ===== -->
      <div class="detail-section">
        <div class="section-label"><span>商品详情</span></div>
        <div class="detail-content-card">
          <p class="detail-text">{{ product.description }}</p>
          <p class="detail-hint">我们承诺提供最优质的商品和服务，所有商品均为全新正品，享受完善的售后服务。</p>
        </div>
      </div>

      <!-- ===== 评论占位区 ===== -->
      <div class="review-section">
        <div class="section-label"><span>用户评价</span></div>
        <div class="review-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-4)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <p class="review-empty-text">暂无评价，快来抢沙发吧~</p>
        </div>
      </div>
    </div>

    <!-- ===== 底部 ===== -->
    <footer class="footer" v-if="product">
      <div class="footer-inner">
        <div class="footer-logo">ZuiMShop</div>
        <div class="footer-links">
          <a href="#" @click.prevent="goHome">首页</a>
          <span class="footer-dot">·</span>
          <a href="#" @click.prevent="goHome">商品</a>
        </div>
        <div class="footer-copy">© 2026 ZuiMShop. All rights reserved.</div>
      </div>
    </footer>

    <!-- ===== 地址选择弹窗（立即购买用） ===== -->
    <Teleport to="body">
      <div v-if="showAddressModal" class="modal-overlay" @click.self="cancelCheckout">
        <div class="address-modal">
          <div class="address-modal-header">
            <h3>确认收货信息</h3>
            <button class="address-modal-close" @click="cancelCheckout">✕</button>
          </div>

          <div class="address-list" v-if="addresses.length > 0">
            <p class="address-section-label">选择已有地址</p>
            <div v-for="addr in addresses" :key="addr.id"
                 class="address-card"
                 :class="{ selected: selectedAddressId === addr.id }"
                 @click="selectAddress(addr)">
              <div class="address-card-info">
                <span class="address-card-name">{{ addr.receiverName }}</span>
                <span class="address-card-phone">{{ addr.receiverPhone }}</span>
                <span class="address-card-tag" v-if="addr.isDefault === 1">默认</span>
              </div>
              <p class="address-card-detail">{{ addr.receiverAddress }}</p>
            </div>
          </div>

          <div class="address-form">
            <p class="address-section-label">{{ addresses.length > 0 ? '或填写新地址' : '填写收货地址' }}</p>
            <div class="address-form-row">
              <input v-model="formReceiverName" placeholder="收货人姓名" class="address-input" />
              <input v-model="formReceiverPhone" placeholder="收货人电话" class="address-input" />
            </div>
            <div class="address-form-row">
              <input v-model="formReceiverAddress" placeholder="收货地址（如：xx省xx市xx区xx路xx号）" class="address-input address-input--wide" />
            </div>
            <label class="address-save-check" v-if="selectedAddressId === null">
              <input type="checkbox" v-model="formSaveAddress" />
              <span>保存为常用地址</span>
            </label>
          </div>

          <p v-if="addressMsg" :class="['address-msg', addressMsgSuccess ? 'address-msg--success' : 'address-msg--error']">{{ addressMsg }}</p>

          <div class="coupon-select" v-if="availableCoupons.length > 0">
            <p class="address-section-label">使用优惠券</p>
            <div class="coupon-options">
              <button class="coupon-option" :class="{ selected: selectedCoupon === null }"
                      @click="selectedCoupon = null">
                <span>不使用</span>
              </button>
              <button v-for="c in availableCoupons" :key="c.id"
                      class="coupon-option" :class="{ selected: selectedCoupon && selectedCoupon.id === c.id }"
                      @click="selectedCoupon = c">
                <span class="co-name">{{ c.name }}</span>
                <span class="co-value">
                  {{ c.type === 2 ? Number(c.discountValue).toFixed(1) + '折' : '减¥' + Number(c.discountValue).toFixed(0) }}
                </span>
                <span class="co-threshold">满¥{{ Number(c.minAmount || 0).toFixed(0) }}可用</span>
              </button>
            </div>
          </div>

          <div class="address-modal-actions">
            <button class="address-btn address-btn--cancel" @click="cancelCheckout">取消</button>
            <button class="address-btn address-btn--confirm" @click="confirmCheckout" :disabled="checkouting">
              <span v-if="checkouting" class="btn-loading"></span>
              <span v-else>确认下单</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ===== 聊天抽屉 ===== -->
    <ChatDrawer :show="showChat" :sellerId="product && product.seller ? product.seller.id : 0"
                :sellerName="product && product.seller ? product.seller.sellerName : ''"
                :sellerAvatar="product && product.seller ? getCachedAvatar(product.seller.sellerAvatar) : ''"
                @close="showChat = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, createOrder, getAvailableCoupons } from '../api/index.js'
import { getUserAddresses, addAddress, getUserInfo } from '../api/index.js'
import { toastError } from '../utils/toast.js'
import { getCachedAvatar, DEFAULT_AVATAR } from '../utils/avatarCache.js'
import ChatDrawer from '../components/ChatDrawer.vue'

const route = useRoute()
const router = useRouter()

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

const productId = ref(route.params.id)
const product = ref(null)
const images = ref([])
const loading = ref(true)
const scrolled = ref(false)
const nickname = ref(localStorage.getItem('nickname') || '')
const currentImageIndex = ref(0)
const quantity = ref(1)

// ===== 地址选择弹窗状态 =====
const showAddressModal = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const formReceiverName = ref('')
const formReceiverPhone = ref('')
const formReceiverAddress = ref('')
const formSaveAddress = ref(false)
const addressMsg = ref('')
const addressMsgSuccess = ref(false)
const checkouting = ref(false)
// ===== 优惠券状态 =====
const availableCoupons = ref([])
const selectedCoupon = ref(null)
// ===== 聊天抽屉状态 =====
const showChat = ref(false)

// 当前显示的图片
const currentImage = computed(() => {
  if (images.value.length > 0 && images.value[currentImageIndex.value]) {
    return images.value[currentImageIndex.value].imageUrl
  }
  return product.value?.mainImageUrl || ''
})

// 获取商品详情（使用 axios API 服务）
async function fetchProductDetail() {
  loading.value = true
  try {
    const res = await getProductDetail(productId.value)
    if (res && res.code === 200) {
      product.value = res.data
      images.value = res.data.images || []
    } else {
      console.error('获取商品详情失败:', res?.mes)
    }
  } catch (error) {
    console.error('获取商品详情错误:', error)
  } finally {
    loading.value = false
  }
}

// 选择图片
function selectImage(index) {
  currentImageIndex.value = index
}

// 增加数量
function increaseQty() {
  if (quantity.value < product.value.stock) {
    quantity.value++
  }
}

// 减少数量
function decreaseQty() {
  if (quantity.value > 1) {
    quantity.value--
  }
}

// 加入购物车
function addToCart() {
  const cart = JSON.parse(localStorage.getItem('cart') || '[]')
  const existingIndex = cart.findIndex(item => item.id === product.value.id)

  if (existingIndex !== -1) {
    cart[existingIndex].qty += quantity.value
  } else {
    cart.push({
      id: product.value.id,
      productName: product.value.productName,
      price: product.value.price,
      mainImageUrl: product.value.mainImageUrl,
      category: product.value.category,
      qty: quantity.value
    })
  }

  localStorage.setItem('cart', JSON.stringify(cart))
  showToast('已加入购物车')
}

// 立即购买：打开地址选择弹窗
async function buyNow() {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }

  addressMsg.value = ''
  try {
    const res = await getUserAddresses()
    if (res && res.code === 200 && res.data) {
      addresses.value = res.data
      const defaultAddr = res.data.find(a => a.isDefault === 1)
      if (defaultAddr) {
        selectedAddressId.value = defaultAddr.id
        formReceiverName.value = defaultAddr.receiverName
        formReceiverPhone.value = defaultAddr.receiverPhone
        formReceiverAddress.value = defaultAddr.receiverAddress
      } else {
        selectedAddressId.value = null
        formReceiverName.value = ''
        formReceiverPhone.value = ''
        formReceiverAddress.value = ''
      }
    }
  } catch {
    addresses.value = []
    selectedAddressId.value = null
  }

  // 加载可用优惠券
  availableCoupons.value = []
  selectedCoupon.value = null
  try {
    const cres = await getAvailableCoupons()
    if (cres && cres.code === 200) {
      availableCoupons.value = cres.data || []
    }
  } catch {
    // 优惠券加载失败不影响下单
  }

  showAddressModal.value = true
}

// 选择已有地址
function selectAddress(addr) {
  selectedAddressId.value = addr.id
  formReceiverName.value = addr.receiverName
  formReceiverPhone.value = addr.receiverPhone
  formReceiverAddress.value = addr.receiverAddress
  addressMsg.value = ''
}

// 确认下单
async function confirmCheckout() {
  const name = formReceiverName.value.trim()
  const phone = formReceiverPhone.value.trim()
  const addr = formReceiverAddress.value.trim()

  if (!name) {
    addressMsg.value = '请输入收货人姓名'
    addressMsgSuccess.value = false
    return
  }
  if (!phone) {
    addressMsg.value = '请输入收货人电话'
    addressMsgSuccess.value = false
    return
  }
  if (!addr) {
    addressMsg.value = '请输入收货地址'
    addressMsgSuccess.value = false
    return
  }

  if (selectedAddressId.value === null && formSaveAddress.value) {
    try {
      await addAddress({
        receiverName: name,
        receiverPhone: phone,
        receiverAddress: addr,
        isDefault: addresses.value.length === 0 ? 1 : 0
      })
    } catch {
      // 保存失败不影响下单
    }
  }

  checkouting.value = true
  addressMsg.value = ''

  try {
    const res = await createOrder({
      items: [{ productId: product.value.id, quantity: quantity.value }],
      receiverName: name,
      receiverPhone: phone,
      receiverAddress: addr,
      remark: '',
      userCouponId: selectedCoupon.value ? selectedCoupon.value.id : null
    })
    if (res && res.code === 200) {
      showAddressModal.value = false
      addressMsg.value = '下单成功，请前往订单页支付'
      addressMsgSuccess.value = true
      setTimeout(() => {
        router.push('/orders')
      }, 800)
    } else {
      addressMsg.value = (res && res.mes) || '下单失败'
      addressMsgSuccess.value = false
    }
  } catch (err) {
    addressMsg.value = '下单失败：' + ((err && err.message) || '服务连接失败，请稍后重试')
    addressMsgSuccess.value = false
  } finally {
    checkouting.value = false
  }
}

// 取消下单
function cancelCheckout() {
  showAddressModal.value = false
  addressMsg.value = ''
}

// 显示提示
function showToast(message) {
  alert(message)
}

// 退出登录
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

// 返回首页
function goHome() {
  router.push('/home')
}
// 打开与卖家的聊天
function openChat() {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }
  showChat.value = true
}
function goOrders() {
  router.push('/orders')
}
function goProfile() {
  router.push('/profile')
}
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }

// 滚动到指定位置
function scrollTo(selector) {
  const el = document.querySelector(selector)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' })
  }
}

// 滚动监听
function onScroll() {
  scrolled.value = window.scrollY > 60
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  document.addEventListener('click', closePopups)
  fetchProductDetail()
  fetchUserProfile()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  document.removeEventListener('click', closePopups)
})
</script>

<style scoped>
/* ============================================================
   商品详情页 — 冷色电商风
   设计系统 CSS 变量定义于 style.css
   ============================================================ */

/* ===== 顶部导航 ===== */
.top-nav {
  position: fixed; top: 0; left: 0; right: 0;
  height: 68px; z-index: 1000;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  transition: box-shadow 0.3s;
}
.top-nav.scrolled {
  box-shadow: var(--shadow-sm);
}
.nav-inner {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.nav-logo {
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
  flex-shrink: 0;
}
.nav-breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}
.breadcrumb-arrow {
  color: var(--text-4);
  font-size: 1.1rem;
  font-weight: 600;
}
.breadcrumb-cat {
  font-size: 0.82rem;
  color: var(--text-3);
  flex-shrink: 0;
}
.breadcrumb-current {
  font-size: 0.82rem;
  color: var(--text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.nav-cart-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--surface);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.25s;
}
.nav-cart-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(47, 84, 235, 0.12);
}

/* ===== 用户头像与下拉菜单 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 8px; }
.nav-balance {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
.nav-nickname {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-2);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.avatar-btn {
  display: flex; align-items: center; gap: 3px;
  background: none; border: none; cursor: pointer;
  padding: 2px; transition: transform 0.25s;
}
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 36px; height: 36px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--border);
  flex-shrink: 0;
}
.avatar-circle img {
  width: 100%; height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-caret {
  font-size: 0.6rem;
  color: var(--text-4);
  transition: transform 0.3s;
}
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 10px); right: 0;
  min-width: 190px; padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  z-index: 1001;
}
.profile-menu-header {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 6px;
}
.profile-menu-avatar {
  width: 40px; height: 40px;
  border-radius: 50%; overflow: hidden;
  flex-shrink: 0;
}
.profile-menu-avatar img {
  width: 100%; height: 100%;
  border-radius: 50%; object-fit: cover;
}
.profile-menu-id { min-width: 0; }
.profile-menu-name {
  font-size: 0.9rem; font-weight: 600;
  color: var(--text); margin: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.profile-menu-role {
  font-size: 0.72rem; color: var(--text-3); margin: 2px 0 0;
}
.profile-menu-item {
  display: block; width: 100%; text-align: left;
  padding: 10px 14px; border: none; border-radius: var(--radius-sm);
  background: transparent; cursor: pointer;
  font-size: 0.85rem; color: var(--text-2);
  transition: all 0.2s;
}
.profile-menu-item:hover { background: var(--primary-soft); color: var(--primary); }
.profile-menu-item--logout:hover { background: var(--danger-soft); color: var(--danger); }
.profile-menu-divider { height: 1px; background: var(--border); margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 加载状态 ===== */
.loading-container {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  min-height: 60vh; gap: 20px;
  padding-top: 68px;
}
.loading-text {
  font-size: 0.95rem;
  color: var(--text-3);
}

/* ===== 错误状态 ===== */
.error-container {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  min-height: 60vh; text-align: center; gap: 16px;
  padding-top: 68px;
}
.error-icon { font-size: 3.5rem; }
.error-title {
  font-family: var(--font-display);
  font-size: 1.5rem; color: var(--text);
}
.error-desc { color: var(--text-3); font-size: 0.9rem; }

/* ===== 商品详情主体 ===== */
.detail-main {
  padding-top: 68px;
  padding-bottom: 0;
}
.detail-container {
  max-width: var(--container);
  margin: 0 auto;
  padding: 32px 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
}

/* ===== 左侧：图片画廊 ===== */
.gallery-section {
  position: sticky;
  top: 92px;
  align-self: start;
}
.main-image-wrap {
  background: var(--surface-2);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 14px;
  position: relative;
}
.main-image {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.main-image-wrap:hover .main-image img {
  transform: scale(1.08);
}
.image-zoom-hint {
  position: absolute;
  bottom: 12px;
  right: 12px;
  padding: 4px 12px;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(6px);
  border-radius: var(--radius-pill);
  font-size: 0.7rem;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}
.main-image-wrap:hover .image-zoom-hint {
  opacity: 1;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.thumbnail-list::-webkit-scrollbar { height: 4px; }
.thumbnail-list::-webkit-scrollbar-thumb { background: var(--border-strong); border-radius: 2px; }
.thumbnail-item {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.25s;
  background: var(--surface-2);
}
.thumbnail-item:hover {
  border-color: var(--text-4);
}
.thumbnail-item.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(47, 84, 235, 0.15);
}
.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ===== 右侧：信息面板 ===== */
.info-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 信息头部 */
.info-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cat-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  background: var(--primary-soft);
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--primary);
}
.badge-hot {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
  border-radius: var(--radius-pill);
  font-size: 0.7rem;
  font-weight: 600;
  color: #fff;
}

/* 商品名称 */
.product-title {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text);
  line-height: 1.3;
  margin: 0;
}

/* 商品描述 */
.product-desc {
  font-size: 0.95rem;
  color: var(--text-2);
  line-height: 1.7;
  margin: 0;
}

/* 价格区 */
.price-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  background: linear-gradient(135deg, var(--danger-soft), var(--surface));
  border-radius: var(--radius);
  border: 1px solid rgba(255, 77, 79, 0.08);
}
.price-main {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.price-label {
  font-size: 0.82rem;
  color: var(--text-3);
  margin-right: 6px;
}
.price-symbol {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--price);
}
.price-value {
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 700;
  color: var(--price);
  line-height: 1;
}

/* 统计信息 */
.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 16px 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text);
}
.stat-desc {
  font-size: 0.78rem;
  color: var(--text-3);
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border);
}

/* 数量选择器 */
.qty-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.qty-label {
  font-size: 0.88rem;
  color: var(--text-2);
  font-weight: 500;
}
.qty-selector {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--surface);
}
.qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: var(--surface-2);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s;
}
.qty-btn:hover:not(:disabled) {
  background: var(--primary-soft);
  color: var(--primary);
}
.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.qty-value {
  width: 48px;
  text-align: center;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text);
  border-left: 1px solid var(--border);
  border-right: 1px solid var(--border);
  padding: 8px 0;
}

/* 操作按钮 */
.action-row {
  display: flex;
  gap: 12px;
}
.btn-cart, .btn-buy {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  border-radius: var(--radius);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  font-family: var(--font-sans);
}
.btn-cart {
  background: var(--surface);
  color: var(--primary);
  border: 2px solid var(--primary);
}
.btn-cart:hover {
  background: var(--primary-soft);
  transform: translateY(-1px);
}
.btn-buy {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff;
  box-shadow: 0 4px 14px rgba(47, 84, 235, 0.25);
}
.btn-buy:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(47, 84, 235, 0.35);
}

/* ===== 卖家店铺卡片 ===== */
.seller-card {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px;
}
.seller-card-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}
.seller-avatar-wrap {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--border);
}
.seller-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.seller-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 1.3rem;
  font-weight: 700;
}
.seller-meta {
  min-width: 0;
  flex: 1;
}
.seller-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}
.seller-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}
.seller-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background: var(--success-soft);
  border-radius: var(--radius-pill);
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--success);
}
.seller-address {
  font-size: 0.82rem;
  color: var(--text-3);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.seller-card-actions {
  display: flex;
  gap: 10px;
}
.seller-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  font-family: var(--font-sans);
}
.seller-btn--visit {
  background: var(--surface);
  border: 1px solid var(--border-strong);
  color: var(--text-2);
}
.seller-btn--visit:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.seller-btn--chat {
  background: var(--primary-soft);
  border: 1px solid transparent;
  color: var(--primary);
}
.seller-btn--chat:hover {
  background: var(--primary);
  color: #fff;
}

/* ===== 服务保障条 ===== */
.guarantee-bar {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 24px 32px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.guarantee-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 12px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text-2);
  font-size: 0.88rem;
  font-weight: 500;
  transition: all 0.25s;
}
.guarantee-item:hover {
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}
.guarantee-item svg {
  color: var(--primary);
  flex-shrink: 0;
}

/* ===== 商品详情区 ===== */
.detail-section {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 24px 32px;
}
.section-label {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.section-label::before,
.section-label::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}
.section-label span {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
}
.detail-content-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 32px;
}
.detail-text {
  font-size: 0.95rem;
  color: var(--text-2);
  line-height: 1.8;
  margin: 0 0 16px 0;
}
.detail-hint {
  font-size: 0.85rem;
  color: var(--text-3);
  line-height: 1.7;
  margin: 0;
  padding: 14px 18px;
  background: var(--surface-2);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--primary);
}

/* ===== 评论占位区 ===== */
.review-section {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 24px 48px;
}
.review-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  gap: 14px;
}
.review-empty-text {
  font-size: 0.9rem;
  color: var(--text-3);
  margin: 0;
}

/* ===== 底部 ===== */
.footer {
  background: var(--text);
  padding: 36px 24px;
  margin-top: 0;
}
.footer-inner {
  max-width: var(--container);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.footer-logo {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(255,255,255,0.5);
}
.footer-links {
  display: flex;
  align-items: center;
  gap: 12px;
}
.footer-links a {
  font-size: 0.78rem;
  color: var(--text-4);
  text-decoration: none;
  transition: color 0.3s;
  cursor: pointer;
}
.footer-links a:hover { color: #fff; }
.footer-dot { color: var(--text-4); font-size: 0.6rem; }
.footer-copy { font-size: 0.75rem; color: var(--text-4); }

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .detail-container {
    grid-template-columns: 1fr;
    gap: 36px;
  }
  .gallery-section {
    position: static;
  }
  .guarantee-bar {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .nav-inner { padding: 0 16px; }
  .nav-breadcrumb { display: none; }
  .detail-container { padding: 24px 16px; gap: 28px; }
  .product-title { font-size: 1.4rem; }
  .price-value { font-size: 1.8rem; }
  .action-row { flex-direction: column; }
  .guarantee-bar { padding: 0 16px 24px; gap: 8px; }
  .guarantee-item { font-size: 0.82rem; padding: 12px 8px; gap: 6px; }
  .detail-section { padding: 0 16px 24px; }
  .detail-content-card { padding: 20px; }
  .review-section { padding: 0 16px 32px; }
  .footer { padding: 28px 16px; }
  .footer-inner { flex-direction: column; gap: 16px; text-align: center; }
  .seller-card-actions { flex-direction: column; }
}
@media (max-width: 480px) {
  .nav-balance { display: none; }
  .nav-nickname { display: none; }
}

/* ===== 地址选择弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.address-modal {
  width: 520px; max-width: 92vw; max-height: 85vh; overflow-y: auto;
  background: var(--surface); border-radius: var(--radius-lg); padding: 28px 32px;
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.3s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.address-modal-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.address-modal-header h3 {
  font-family: var(--font-display);
  font-size: 1.2rem; font-weight: 700; color: var(--text);
  margin: 0;
}
.address-modal-close {
  background: none; border: none; font-size: 1.2rem;
  color: var(--text-4); cursor: pointer; padding: 4px; transition: color 0.2s;
}
.address-modal-close:hover { color: var(--text); }
.address-section-label {
  font-size: 0.8rem; font-weight: 600; color: var(--text-3);
  margin-bottom: 10px; letter-spacing: 0.02em;
}
.coupon-select { margin-bottom: 16px; }
.coupon-options { display: flex; flex-wrap: wrap; gap: 8px; }
.coupon-option {
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px;
  padding: 8px 14px; border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--surface); cursor: pointer; transition: all 0.2s;
  font-family: var(--font-sans); text-align: left;
}
.coupon-option:hover { border-color: var(--primary); }
.coupon-option.selected {
  border-color: var(--primary); background: var(--primary-soft);
  box-shadow: 0 0 0 2px rgba(47, 84, 235, 0.12);
}
.co-name { font-size: 0.78rem; font-weight: 600; color: var(--text); }
.co-value { font-size: 0.85rem; font-weight: 700; color: var(--primary); }
.co-threshold { font-size: 0.68rem; color: var(--text-4); }
.address-list { margin-bottom: 20px; }
.address-card {
  padding: 12px 16px; border: 1px solid var(--border); border-radius: var(--radius);
  margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
  background: var(--surface);
}
.address-card:hover { border-color: var(--primary); background: var(--primary-softer); }
.address-card.selected { border-color: var(--primary); background: var(--primary-soft); box-shadow: 0 0 0 2px rgba(47, 84, 235, 0.10); }
.address-card-info { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.address-card-name { font-size: 0.9rem; font-weight: 600; color: var(--text); }
.address-card-phone { font-size: 0.8rem; color: var(--text-3); }
.address-card-tag {
  padding: 1px 8px; border-radius: var(--radius-pill);
  background: var(--primary-soft); color: var(--primary); font-size: 0.7rem; font-weight: 600;
}
.address-card-detail { font-size: 0.8rem; color: var(--text-2); }
.address-form { margin-bottom: 16px; }
.address-form-row { display: flex; gap: 10px; margin-bottom: 10px; }
.address-input {
  flex: 1; padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  font-size: 0.85rem; font-family: var(--font-sans); color: var(--text);
  outline: none; transition: border-color 0.2s;
}
.address-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(47, 84, 235, 0.08); }
.address-input::placeholder { color: var(--text-4); }
.address-input--wide { width: 100%; }
.address-save-check {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  font-size: 0.8rem; color: var(--text-3); margin-top: 8px;
}
.address-save-check input[type="checkbox"] { accent-color: var(--primary); }
.address-msg {
  margin: 12px 0; text-align: center; font-size: 0.85rem;
  padding: 10px; border-radius: var(--radius-sm);
}
.address-msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47, 158, 68, 0.12); }
.address-msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224, 49, 49, 0.12); }
.address-modal-actions {
  display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px;
}
.address-btn {
  padding: 12px 28px; border: none; border-radius: var(--radius-sm);
  font-family: var(--font-sans); font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.address-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.address-btn--cancel {
  border: 1px solid var(--border-strong); background: transparent; color: var(--text-2);
}
.address-btn--cancel:hover { border-color: var(--primary); color: var(--primary); }
.address-btn--confirm {
  background: linear-gradient(135deg, var(--primary), var(--primary-2)); color: #fff;
}
.address-btn--confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(47, 84, 235, 0.25); }
.btn-loading {
  display: inline-block; width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 480px) {
  .address-modal { padding: 24px 20px; }
  .address-form-row { flex-direction: column; gap: 8px; }
  .address-modal-actions { flex-direction: column; }
  .address-btn { width: 100%; text-align: center; }
}
</style>