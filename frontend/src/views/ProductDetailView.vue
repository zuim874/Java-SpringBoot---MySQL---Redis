<template>
  <div class="product-detail-page">
    <!-- 毛玻璃导航 -->
    <nav class="nav" :class="{ scrolled: scrolled }">
      <div class="nav-logo" @click="goHome">ZuiMShop</div>
      <ul class="nav-links">
        <li><a href="#product" @click.prevent="goHome">返回首页</a></li>
        <li v-if="product">
          <span class="breadcrumb">
            商品详情 / {{ product.category }}
          </span>
        </li>
      </ul>
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
            <div class="profile-menu-divider"></div>
            <button class="profile-menu-item profile-menu-item--logout" @click="handleLogout">退出登录</button>
          </div>
        </transition>
      </div>
    </nav>

    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载商品信息中...</p>
    </div>

    <!-- 商品不存在 -->
    <div class="error-container" v-else-if="!product">
      <div class="error-icon">🔍</div>
      <h2>商品不存在或已下架</h2>
      <p>抱歉，您访问的商品可能已下架或不存在</p>
      <button class="btn-primary" @click="goHome">返回首页</button>
    </div>

    <!-- 商品详情 -->
    <div class="detail-content" v-else>
      <div class="detail-container">
        <!-- 左侧：图片区域 -->
        <div class="image-section">
          <div class="main-image">
            <img :src="currentImage" :alt="product.productName" />
          </div>
          <div class="thumbnail-list" v-if="images.length > 0">
            <div v-for="(img, idx) in images" :key="img.id"
                 class="thumbnail"
                 :class="{ active: currentImageIndex === idx }"
                 @click="selectImage(idx)">
              <img :src="img.imageUrl" :alt="'图片 ' + (idx + 1)" />
            </div>
          </div>
        </div>

        <!-- 右侧：商品信息 -->
        <div class="info-section">
          <div class="category-tag">{{ product.category }}</div>
          <h1 class="product-title">{{ product.productName }}</h1>
          <p class="product-description">{{ product.description }}</p>

          <div class="price-section">
            <span class="price-label">价格</span>
            <span class="price-value">¥{{ product.price.toFixed(2) }}</span>
          </div>

          <div class="stats-section">
            <div class="stat-item">
              <span class="stat-label">库存</span>
              <span class="stat-value">{{ product.stock }} 件</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已售</span>
              <span class="stat-value">{{ product.sold }} 件</span>
            </div>
          </div>

          <!-- 卖家信息 -->
          <div class="seller-section" v-if="product.seller">
            <h3 class="section-title">卖家信息</h3>
            <div class="seller-card">
              <div class="seller-avatar" v-if="product.seller.sellerAvatar">
                <img :src="product.seller.sellerAvatar" :alt="product.seller.sellerName" />
              </div>
              <div class="seller-avatar-placeholder" v-else>
                {{ product.seller.sellerName.charAt(0) }}
              </div>
              <div class="seller-info">
                <h4>{{ product.seller.sellerName }}</h4>
                <p class="seller-address">📍 {{ product.seller.address }}</p>
                <p class="seller-contact" v-if="product.seller.sellerContact">📞 {{ product.seller.sellerContact }}</p>
              </div>
              <button class="seller-chat-btn" @click="openChat">
                <span>💬</span>
                <span>联系卖家</span>
              </button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <div class="quantity-selector">
              <button @click="decreaseQty" :disabled="quantity <= 1">−</button>
              <span>{{ quantity }}</span>
              <button @click="increaseQty" :disabled="quantity >= product.stock">+</button>
            </div>
            <button class="btn-cart" @click="addToCart">加入购物车</button>
            <button class="btn-buy" @click="buyNow">立即购买</button>
          </div>
        </div>
      </div>

      <!-- 商品详情描述 -->
      <div class="description-section">
        <div class="container">
          <h2 class="section-title">商品详情</h2>
          <div class="description-content">
            <p>{{ product.description }}</p>
            <p class="description-hint">我们承诺提供最优质的商品和服务，所有商品均为全新正品，享受完善的售后服务。</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="footer" v-if="product">
      <div class="footer-inner">
        <div class="footer-logo">ZuiMShop</div>
        <div class="footer-links">
          <a href="#" @click.prevent="goHome">首页</a>
          <a href="#" @click.prevent="scrollTo('#product')">商品</a>
        </div>
        <div class="footer-copy">© 2026 ZuiMShop. All rights reserved.</div>
      </div>
    </footer>

    <!-- ===== 地址选择弹窗（立即购买用）===== -->
    <Teleport to="body">
      <div v-if="showAddressModal" class="modal-overlay" @click.self="cancelCheckout">
        <div class="address-modal">
          <div class="address-modal-header">
            <h3>确认收货信息</h3>
            <button class="address-modal-close" @click="cancelCheckout">✕</button>
          </div>

          <!-- 已有地址列表 -->
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

          <!-- 手动填写地址 -->
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

          <!-- 优惠券选择 -->
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
                :sellerAvatar="product && product.seller ? product.seller.sellerAvatar : ''"
                @close="showChat = false" />
  </div></template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, createOrder, getAvailableCoupons } from '../api/index.js'
import { getUserAddresses, addAddress } from '../api/index.js'
import { toastError } from '../utils/toast.js'
import ChatDrawer from '../components/ChatDrawer.vue'

const route = useRoute()
const router = useRouter()

const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'

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
.product-detail-page {
  font-family: 'DM Sans', -apple-system, sans-serif;
  color: #212529;
  background: #ffffff;
  line-height: 1.6;
  min-height: 100vh;
}

/* ===== Navigation ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0;
  height: 80px; z-index: 1000;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav.scrolled { background: rgba(255,255,255,0.85); box-shadow: 0 1px 40px rgba(0,0,0,0.06); }
.nav-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.6rem; font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0 0%, #4a9eff 50%, #7c3aed 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  cursor: pointer;
}
.nav-links { display: flex; align-items: center; gap: 32px; list-style: none; margin: 0; padding: 0; }
.nav-links a {
  font-size: 0.875rem; font-weight: 500; color: #868e96;
  letter-spacing: 0.04em; text-transform: uppercase;
  position: relative; transition: color 0.3s; cursor: pointer;
}
.nav-links a:hover { color: #212529; }
.breadcrumb {
  font-size: 0.85rem; color: #495057;
}
.nav-user { display: flex; align-items: center; gap: 8px; }
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; padding: 0 4px; }
.nav-profile-btn {
  padding: 8px 14px; border-radius: 50px;
  border: 1px solid #dee2e6; background: transparent; color: #2b6cb0;
  font-family: 'DM Sans', sans-serif; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; min-height: 32px; display: flex; align-items: center;
}
.nav-profile-btn:hover { border-color: #2b6cb0; background: rgba(43,108,176,0.04); transform: translateY(-1px); }
.nav-logout {
  padding: 8px 14px; border-radius: 50px;
  border: 1px solid #dee2e6; background: transparent; color: #868e96;
  font-family: 'DM Sans', sans-serif; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; min-height: 32px; display: flex; align-items: center;
}
.nav-logout:hover { border-color: #dc3545; color: #dc3545; transform: translateY(-1px); }

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

/* ===== Loading ===== */
.loading-container {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 60vh; gap: 20px;
}
.loading-spinner {
  width: 50px; height: 50px;
  border: 4px solid #f1f3f5;
  border-top-color: #2b6cb0;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Error ===== */
.error-container {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 60vh; text-align: center; gap: 16px;
}
.error-icon { font-size: 4rem; }
.error-container h2 { font-family: 'Playfair Display', Georgia, serif; color: #212529; }
.error-container p { color: #868e96; }
.btn-primary {
  padding: 14px 36px; border: none; border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  letter-spacing: 0.04em; cursor: pointer; transition: all 0.3s;
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(43,108,176,0.40); }

/* ===== Detail Content ===== */
.detail-content {
  padding-top: 80px;
}
.detail-container {
  max-width: 1200px; margin: 0 auto;
  display: grid; grid-template-columns: 1fr 1fr; gap: 60px;
  padding: 60px 40px;
}

/* Image Section */
.image-section {
  position: sticky; top: 100px;
}
.main-image {
  aspect-ratio: 1;
  background: #f8f9fa;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 16px;
}
.main-image img {
  width: 100%; height: 100%; object-fit: cover;
}
.thumbnail-list {
  display: flex; gap: 12px;
  overflow-x: auto; padding-bottom: 8px;
}
.thumbnail {
  flex-shrink: 0;
  width: 80px; height: 80px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
}
.thumbnail.active { border-color: #2b6cb0; }
.thumbnail img {
  width: 100%; height: 100%; object-fit: cover;
}

/* Info Section */
.info-section {
  display: flex; flex-direction: column; gap: 24px;
}
.category-tag {
  display: inline-block;
  padding: 6px 16px;
  background: linear-gradient(135deg, rgba(43,108,176,0.1), rgba(124,58,237,0.1));
  border-radius: 50px;
  font-size: 0.75rem; font-weight: 600; color: #2b6cb0;
  width: fit-content;
}
.product-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2rem; font-weight: 700; color: #212529;
  line-height: 1.3;
  margin: 0;
}
.product-description {
  font-size: 1rem; color: #495057; line-height: 1.7;
}
.price-section {
  display: flex; align-items: baseline; gap: 12px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9ff, #ffffff);
  border-radius: 16px;
}
.price-label { font-size: 0.875rem; color: #868e96; }
.price-value {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2.5rem; font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.stats-section {
  display: flex; gap: 40px;
  padding: 16px 0;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 0.75rem; color: #868e96; text-transform: uppercase; letter-spacing: 0.04em; }
.stat-value { font-size: 1.1rem; font-weight: 600; color: #212529; }

/* Seller Section */
.seller-section {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 16px;
}
.section-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.1rem; font-weight: 600; color: #212529;
  margin: 0 0 16px 0;
}
.seller-card {
  display: flex; gap: 16px; align-items: center;
}
.seller-avatar, .seller-avatar-placeholder {
  width: 60px; height: 60px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.seller-avatar img {
  width: 100%; height: 100%; object-fit: cover;
}
.seller-avatar-placeholder {
  background: linear-gradient(135deg, #2b6cb0, #7c3aed);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 1.5rem; font-weight: 600;
}
.seller-info h4 {
  font-size: 1rem; font-weight: 600; color: #212529; margin: 0 0 4px 0;
}
.seller-address, .seller-contact {
  font-size: 0.85rem; color: #868e96; margin: 0;
}
.seller-chat-btn {
  flex-shrink: 0; display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border: 1px solid rgba(43,108,176,0.3); border-radius: 50px;
  background: transparent; color: #2b6cb0; font-family: 'DM Sans', sans-serif;
  font-size: 0.78rem; font-weight: 600; cursor: pointer; transition: all 0.3s;
  margin-left: auto;
}
.seller-chat-btn:hover { background: rgba(43,108,176,0.06); border-color: #2b6cb0; transform: translateY(-1px); }

/* Action Section */
.action-section {
  display: flex; gap: 16px; align-items: center;
  padding-top: 20px;
}
.quantity-selector {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px;
  background: #f8f9fa;
  border-radius: 50px;
}
.quantity-selector button {
  width: 32px; height: 32px;
  border: none; background: white;
  border-radius: 50%;
  font-size: 1.2rem;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
.quantity-selector button:hover:not(:disabled) { background: #e9ecef; }
.quantity-selector button:disabled { opacity: 0.5; cursor: not-allowed; }
.quantity-selector span { font-size: 1rem; font-weight: 600; min-width: 30px; text-align: center; }

.btn-cart, .btn-buy {
  flex: 1;
  padding: 16px 32px;
  border: none;
  border-radius: 50px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}
.btn-cart {
  background: #fff;
  color: #2b6cb0;
  border: 2px solid #2b6cb0;
}
.btn-cart:hover { background: #f8f9ff; }
.btn-buy {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
}
.btn-buy:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(43,108,176,0.40); }

/* Description Section */
.description-section {
  background: #f8f9fa;
  padding: 60px 0;
  margin-top: 40px;
}
.description-section .container {
  max-width: 800px; margin: 0 auto; padding: 0 40px;
}
.description-content {
  background: white;
  padding: 40px;
  border-radius: 20px;
}
.description-content p {
  font-size: 1rem; color: #495057; line-height: 1.8;
  margin: 0 0 16px 0;
}
.description-hint {
  color: #868e96 !important;
  font-size: 0.875rem !important;
}

/* ===== Footer ===== */
.footer {
  background: #212529;
  padding: 48px 56px;
  margin-top: 60px;
}
.footer-inner {
  max-width: 1200px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
}
.footer-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.1rem; font-weight: 700; color: rgba(255,255,255,0.60);
}
.footer-links { display: flex; gap: 32px; }
.footer-links a {
  font-size: 0.75rem; color: #868e96;
  letter-spacing: 0.04em; text-transform: uppercase;
  text-decoration: none; transition: color 0.3s; cursor: pointer;
}
.footer-links a:hover { color: #ced4da; }
.footer-copy { font-size: 0.75rem; color: #495057; }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .detail-container { grid-template-columns: 1fr; gap: 40px; }
  .image-section { position: static; }
}
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .detail-container { padding: 40px 24px; }
  .action-section { flex-wrap: wrap; }
  .btn-cart, .btn-buy { flex: 1 1 auto; }
  .description-section .container { padding: 0 24px; }
  .footer { padding: 40px 24px; }
  .footer-inner { flex-direction: column; gap: 24px; text-align: center; }
}

/* ===== 地址选择弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.3); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.address-modal {
  width: 520px; max-width: 92vw; max-height: 85vh; overflow-y: auto;
  background: white; border-radius: 20px; padding: 28px 32px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.15);
  animation: slideUp 0.3s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.address-modal-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.address-modal-header h3 {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.3rem; font-weight: 700; color: #212529;
}
.address-modal-close {
  background: none; border: none; font-size: 1.2rem; color: #adb5bd;
  cursor: pointer; padding: 4px; transition: color 0.2s;
}
.address-modal-close:hover { color: #212529; }
.address-section-label {
  font-size: 0.8rem; font-weight: 600; color: #868e96;
  margin-bottom: 10px; letter-spacing: 0.02em;
}
/* ===== 优惠券选择 ===== */
.coupon-select { margin-bottom: 16px; }
.coupon-options { display: flex; flex-wrap: wrap; gap: 8px; }
.coupon-option {
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px;
  padding: 8px 14px; border: 1px solid #dee2e6; border-radius: 12px;
  background: #fff; cursor: pointer; transition: all 0.2s;
  font-family: 'DM Sans', sans-serif; text-align: left;
}
.coupon-option:hover { border-color: #4a9eff; }
.coupon-option.selected {
  border-color: #2b6cb0; background: rgba(43,108,176,0.05);
  box-shadow: 0 0 0 2px rgba(43,108,176,0.12);
}
.co-name { font-size: 0.78rem; font-weight: 600; color: #212529; }
.co-value { font-size: 0.85rem; font-weight: 700; color: #2b6cb0; }
.co-threshold { font-size: 0.68rem; color: #adb5bd; }
.address-list { margin-bottom: 20px; }
.address-card {
  padding: 12px 16px; border: 1px solid #dee2e6; border-radius: 12px;
  margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
  background: white;
}
.address-card:hover { border-color: #4a9eff; background: rgba(74,158,255,0.02); }
.address-card.selected { border-color: #2b6cb0; background: rgba(43,108,176,0.04); box-shadow: 0 0 0 2px rgba(43,108,176,0.10); }
.address-card-info { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.address-card-name { font-size: 0.9rem; font-weight: 600; color: #212529; }
.address-card-phone { font-size: 0.8rem; color: #868e96; }
.address-card-tag {
  padding: 1px 8px; border-radius: 10px; background: rgba(43,108,176,0.08);
  color: #2b6cb0; font-size: 0.7rem; font-weight: 600;
}
.address-card-detail { font-size: 0.8rem; color: #495057; }
.address-form { margin-bottom: 16px; }
.address-form-row { display: flex; gap: 10px; margin-bottom: 10px; }
.address-input {
  flex: 1; padding: 10px 14px; border: 1px solid #dee2e6; border-radius: 10px;
  font-size: 0.85rem; font-family: 'DM Sans', sans-serif; color: #212529;
  outline: none; transition: border-color 0.2s;
}
.address-input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.08); }
.address-input::placeholder { color: #adb5bd; }
.address-input--wide { width: 100%; }
.address-save-check {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  font-size: 0.8rem; color: #868e96; margin-top: 8px;
}
.address-save-check input[type="checkbox"] { accent-color: #2b6cb0; }
.address-msg {
  margin: 12px 0; text-align: center; font-size: 0.85rem;
  padding: 10px; border-radius: 10px;
}
.address-msg--success { color: #2b8a3e; background: rgba(43,138,62,0.06); border: 1px solid rgba(43,138,62,0.12); }
.address-msg--error { color: #c92a2a; background: rgba(201,42,42,0.06); border: 1px solid rgba(201,42,42,0.12); }
.address-modal-actions {
  display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px;
}
.address-btn {
  padding: 12px 28px; border: none; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.address-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.address-btn--cancel {
  border: 1px solid #dee2e6; background: transparent; color: #495057;
}
.address-btn--cancel:hover { border-color: #4a9eff; color: #4a9eff; }
.address-btn--confirm {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: white;
}
.address-btn--confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.btn-loading {
  display: inline-block; width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: white;
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
