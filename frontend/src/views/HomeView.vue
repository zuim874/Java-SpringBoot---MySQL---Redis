<template>
  <div class="store-page">
    <!-- ===== 顶部公告条 ===== -->
    <div class="announce-bar">
      <div class="announce-inner">
        <span class="announce-item">🚚 全场满 ¥99 免运费</span>
        <span class="announce-dot">·</span>
        <span class="announce-item">🎁 新用户注册立享新人礼券</span>
        <span class="announce-dot">·</span>
        <span class="announce-item">🎉 会员专享价 · 买得越多省得越多</span>
      </div>
    </div>

    <!-- ===== 毛玻璃导航 ===== -->
    <nav class="nav" :class="{ scrolled: scrolled }">
      <div class="nav-logo" @click="scrollToTop">ZuiMShop</div>
      <!-- 导航搜索框 -->
      <div class="nav-search">
        <input v-model="searchInput" type="text" placeholder="搜索商品名称或描述..." maxlength="50"
               @keyup.enter="handleSearch" class="nav-search-input" />
        <button class="nav-search-btn" @click="handleSearch" aria-label="搜索">🔍</button>
      </div>
      <ul class="nav-links" :class="{ open: menuOpen }">
        <li><a href="#hero" @click.prevent="closeMenu; scrollTo('#hero')">首页</a></li>
        <li><a href="#products" @click.prevent="closeMenu; scrollTo('#products')">商品</a></li>
        <!-- 分类下拉菜单：点击弹出商品种类列表（@click.stop 阻止冒泡，避免触发空白处关闭） -->
        <li class="nav-dropdown" @click.stop>
          <a href="javascript:void(0)" @click.prevent="catMenuOpen = !catMenuOpen">
            分类 <span class="dropdown-caret">▾</span>
          </a>
          <transition name="dropdown">
            <div class="dropdown-menu" v-if="catMenuOpen">
              <button v-for="cat in allCategoryNames" :key="cat"
                      class="dropdown-item" :class="{ active: activeCategory === cat }"
                      @click="selectCategory(cat)">{{ cat }}</button>
            </div>
          </transition>
        </li>
        <li class="nav-user">
          <button class="nav-cart-btn" @click="showCart = true" aria-label="购物车">
            <span class="cart-icon">🛒</span>
            <span class="cart-badge" v-if="cartTotalCount > 0">{{ cartTotalCount }}</span>
          </button>
          <!-- 用户头像组件：左侧显示余额与用户名，点击头像弹出个人中心相关功能与退出登录 -->
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
        </li>
      </ul>
      <button class="nav-toggle" @click="menuOpen = !menuOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </nav>

    <!-- ===== 轮播大图（英雄区域） ===== -->
    <section class="hero" id="hero">
      <div v-for="(slide, idx) in slides" :key="idx"
           class="hero-slide" :class="{ active: currentSlide === idx }">
        <img :src="slide.image" :alt="slide.alt" loading="lazy">
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-badge">✦ 新人专享 · 全场低至 5 折</div>
        <h1 class="hero-title">科技好物<br>触手可及</h1>
        <p class="hero-sub">精选全球优质好物，从数码配件到智能家居，正品保障、极速发货，一站式购齐。</p>
        <div class="hero-actions">
          <button class="btn-primary" @click="scrollTo('#products')">立即选购</button>
          <button class="btn-outline" @click="scrollTo('#products')">浏览商品</button>
        </div>
      </div>
      <div class="carousel-dots">
        <button v-for="(_, idx) in slides" :key="idx"
                class="carousel-dot" :class="{ active: currentSlide === idx }"
                @click="goToSlide(idx)" :aria-label="'Slide ' + (idx+1)"></button>
      </div>
      <button class="carousel-pause-btn" @click="toggleCarousel" aria-label="暂停/播放">
        {{ carouselPaused ? '▶' : '⏸' }}
      </button>
    </section>

    <!-- ===== 横向分类导航条 ===== -->
    <section class="category-bar">
      <div class="container">
        <div class="category-bar-inner">
          <button v-for="cat in allCategoryNames" :key="cat"
                  class="category-chip" :class="{ active: activeCategory === cat }"
                  @click="filterByCategory(cat)">
            {{ cat }}
          </button>
        </div>
      </div>
    </section>

    <!-- ===== 商品列表（分页） ===== -->
    <section class="section products" id="products">
      <div class="container">
        <div class="section-header" ref="prodHeader">
          <span class="section-tag">精选商品</span>
          <h2 class="section-title">热门<span class="highlight">推荐</span></h2>
          <p class="section-desc">{{ activeCategory === '全部' ? '所有商品' : activeCategory }} · 共 {{ totalProducts }} 件</p>
        </div>
        <!-- 搜索栏：关键词模糊搜索商品名/描述 -->
        <div class="search-bar">
          <input v-model="searchInput" type="text" placeholder="搜索商品名称或描述..." maxlength="50"
                 @keyup.enter="handleSearch" class="search-input" />
          <button class="search-btn" @click="handleSearch">搜索</button>
          <button v-if="searchKeyword" class="search-clear" @click="clearSearch">✕ 清除</button>
        </div>
        <!-- 商品加载中 -->
        <div class="loading-state" v-if="loading">
          <div class="loading-spinner"></div>
          <p>正在加载商品...</p>
        </div>

        <!-- 商品加载错误 -->
        <div class="empty-state" v-else-if="error">
          <div class="error-icon">⚠️</div>
          <p>商品加载失败，请稍后重试</p>
          <button class="retry-btn" @click="fetchProducts">重新加载</button>
        </div>

        <!-- 商品为空 -->
        <div class="empty-state" v-else-if="products.length === 0">
          <div class="empty-icon">📦</div>
          <p>暂无商品</p>
          <p class="empty-hint">该分类下暂无商品，去看看其他分类吧</p>
        </div>

        <!-- 商品网格 -->
        <div class="products-grid" v-else>
          <div v-for="(prod, idx) in products" :key="prod.id"
               class="product-card" :ref="el => { if(el) prodRefs[idx] = el }"
               :style="{ transitionDelay: (idx % 4) * 0.08 + 's' }"
               @click="goToProductDetail(prod.id)">
            <div class="product-image">
              <img :src="prod.mainImageUrl || '/uploads/hero/hero-1.jpg'" :alt="prod.productName" loading="lazy">
              <div class="product-tag" v-if="prod.sold > 100">热卖</div>
              <div class="product-tag tag-new" v-else-if="prod.stock > 0 && prod.sold < 10">新品</div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ prod.productName }}</h3>
              <p class="product-desc">{{ prod.description }}</p>
              <div class="product-meta">
                <span class="product-seller">{{ prod.sellerName || '官方自营' }}</span>
                <span class="product-sold">已售 {{ prod.sold || 0 }}</span>
              </div>
              <div class="product-bottom">
                <span class="product-price">¥{{ (prod.price || 0).toFixed(2) }}</span>
                <div class="product-actions">
                  <button class="chat-btn" v-if="prod.sellerId" @click.stop="openChat(prod)">💬 客服</button>
                  <button class="add-cart-btn" @click.stop="addToCart(prod)">加入购物车</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ===== 分页器 ===== -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
            ‹ 上一页
          </button>
          <button v-for="p in pageNumbers" :key="p"
                  class="page-btn" :class="{ active: p === currentPage }"
                  @click="goToPage(p)">
            {{ p }}
          </button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
            下一页 ›
          </button>
        </div>
      </div>
    </section>

    <!-- ===== 领券中心 ===== -->
    <section class="section coupon-center" id="coupons">
      <div class="container">
        <div class="section-header">
          <span class="section-tag">领券中心</span>
          <h2 class="section-title">新人<span class="highlight">大礼包</span></h2>
          <p class="section-desc">先领券，再购物，立省不止一点点</p>
        </div>
        <div class="loading-state" v-if="couponLoading">
          <div class="loading-spinner"></div>
          <p>正在加载优惠券...</p>
        </div>
        <div class="coupon-grid" v-else-if="visibleClaimableCoupons.length > 0">
          <div v-for="c in visibleClaimableCoupons" :key="c.id" class="coupon-card">
            <span v-if="c.targetType === 2" class="coupon-vip-tag">仅VIP</span>
            <div class="coupon-value">
              <span class="coupon-value-sym">{{ c.type === 2 ? '' : '¥' }}</span>
              <span class="coupon-value-num">{{ couponValue(c) }}</span>
              <span class="coupon-value-unit">{{ c.type === 2 ? '折' : '立减券' }}</span>
            </div>
            <div class="coupon-body">
              <h4 class="coupon-name">{{ c.name }}</h4>
              <p class="coupon-threshold">满 ¥{{ Number(c.minAmount || 0).toFixed(0) }} 可用</p>
              <button class="coupon-claim-btn" :disabled="claimingId === c.id" @click="handleClaim(c)">
                {{ claimingId === c.id ? '领取中...' : '立即领取' }}
              </button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-else>
          <div class="empty-icon">🎫</div>
          <p>暂无更多可领取的优惠券</p>
          <p class="empty-hint">关注商城动态，好券不错过</p>
        </div>
      </div>
    </section>

    <!-- ===== 服务保障栏 ===== -->
    <section class="guarantee-bar">
      <div class="container">
        <div class="guarantee-grid">
          <div class="guarantee-item">
            <div class="guarantee-icon">🛡️</div>
            <div class="guarantee-text">
              <h4>正品保障</h4>
              <p>全场正品 · 假一赔十</p>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="guarantee-icon">🚚</div>
            <div class="guarantee-text">
              <h4>极速发货</h4>
              <p>现货速发 · 风雨无阻</p>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="guarantee-icon">↩️</div>
            <div class="guarantee-text">
              <h4>七天无理由</h4>
              <p>七天无理由退换货</p>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="guarantee-icon">🎧</div>
            <div class="guarantee-text">
              <h4>售后无忧</h4>
              <p>一对一贴心客服</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 购物车侧边栏 ===== -->
    <div class="cart-overlay" :class="{ open: showCart }" @click="showCart = false"></div>
    <div class="cart-sidebar" :class="{ open: showCart }">
      <div class="cart-header">
        <h3>购物车 ({{ cartTotalCount }})</h3>
        <button class="cart-close" @click="showCart = false">✕</button>
      </div>
      <div class="cart-body" v-if="cart.length > 0">
        <div v-for="(item, idx) in cart" :key="idx" class="cart-item">
          <img :src="item.mainImageUrl || item.image || '/uploads/hero/hero-1.jpg'"
               :alt="item.productName" class="cart-item-img">
          <div class="cart-item-info">
            <h4>{{ item.productName }}</h4>
            <p class="cart-item-price">¥{{ (item.price || 0).toFixed(2) }}</p>
          </div>
          <div class="cart-item-qty">
            <button @click="decreaseQty(idx)" :disabled="item.qty <= 1">−</button>
            <span>{{ item.qty }}</span>
            <button @click="increaseQty(idx)">+</button>
          </div>
          <button class="cart-item-remove" @click="removeFromCart(idx)">✕</button>
        </div>
      </div>
      <div class="cart-empty" v-else>
        <span class="cart-empty-icon">🛒</span>
        <p>购物车是空的</p>
        <p class="cart-empty-hint">去逛逛吧</p>
      </div>
      <div class="cart-footer" v-if="cart.length > 0">
        <div class="cart-total">
          <span>合计</span>
          <span class="cart-total-price">¥{{ cartTotal.toFixed(2) }}</span>
        </div>
        <button class="checkout-btn" @click="goToCheckout">去结算</button>
      </div>
    </div>

    <!-- ===== 地址选择/填写弹窗（下单用） ===== -->
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

    <!-- ===== 底部版权栏 ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">ZuiMShop</div>
        <div class="footer-links">
          <a href="#hero" @click.prevent="scrollTo('#hero')">首页</a>
          <a href="#products" @click.prevent="scrollTo('#products')">商品</a>
          <a href="javascript:void(0)" @click="goOrders">我的订单</a>
          <a href="javascript:void(0)" @click="goProfile">我的</a>
        </div>
        <div class="footer-copy">© 2026 ZuiMShop. All rights reserved.</div>
      </div>
    </footer>

    <!-- ===== 聊天抽屉 ===== -->
    <ChatDrawer :show="showChat" :sellerId="chatSellerId" :sellerName="chatSellerName" :sellerAvatar="chatSellerAvatar"
                @close="showChat = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getProductPage, getCategories, createOrder, getAvailableCoupons, getClaimableCoupons, claimCoupon, getAllCoupons } from '../api/index.js'
import { getUserAddresses, addAddress, getUserInfo } from '../api/index.js'
import { toastError, toastSuccess } from '../utils/toast.js'
import ChatDrawer from '../components/ChatDrawer.vue'
import { getCachedRoles } from '../utils/auth.js'

const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const scrolled = ref(false)
const menuOpen = ref(false)
const showCart = ref(false)
const catMenuOpen = ref(false)

// ===== 地址选择弹窗状态 =====
const showAddressModal = ref(false)
const addresses = ref([])
// ===== 聊天抽屉状态 =====
const showChat = ref(false)
const chatSellerId = ref(0)
const chatSellerName = ref('')
const chatSellerAvatar = ref('')
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

// ===== 首页模块状态：领券中心 =====
const claimableCoupons = ref([])
const couponLoading = ref(true)
const claimingId = ref(null)
// 已领取优惠券模板ID集合（登录状态下获取，用于隐藏已领取的优惠券）
const claimedCouponIds = ref([])
const isLogin = computed(() => !!localStorage.getItem('token'))

/**
 * 拉取可自助领取的优惠券模板（领券中心）
 * 调用 GET /api/coupon/user/templates
 */
async function fetchClaimableCoupons() {
  try {
    const res = await getClaimableCoupons()
    if (res && res.code === 200 && res.data) {
      claimableCoupons.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    console.error('获取可领优惠券失败:', e)
  } finally {
    couponLoading.value = false
  }
}

/**
 * 优惠券面值展示
 * type=2 为折扣券（如 8.5 折），其余为立减券（如 30 元）
 */
function couponValue(c) {
  const v = Number(c.discountValue || 0)
  return c.type === 2 ? v.toFixed(1) : v.toFixed(0)
}

/**
 * 用户自助领取优惠券
 * 1.未登录先跳登录页
 * 2.领取成功刷新可领列表并提示
 */
async function handleClaim(c) {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }
  claimingId.value = c.id
  try {
    const res = await claimCoupon(c.id)
    if (res && res.code === 200) {
      toastSuccess(`已领取「${c.name}」，快去下单吧！`)
      fetchClaimableCoupons()
      fetchClaimedCouponIds()
    } else {
      toastError((res && res.mes) || '领取失败')
    }
  } catch (e) {
    toastError('领取失败：' + ((e && e.message) || '网络异常'))
  } finally {
    claimingId.value = null
  }
}

// ===== 角色判断（用于导航中显示按钮） =====
const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))

// 当前用户是否为 VIP 会员（VIP用户 或 VIP卖家），用于领券中心过滤 VIP 券
const isVipMember = computed(() =>
  cachedRoles.includes('ROLE_VIP_USER') || cachedRoles.includes('ROLE_VIP_SELLER')
)

// 领券中心可见列表：
// 1.VIP券（targetType=2）仅 VIP 用户 / VIP 卖家可见，普通券所有人可见
// 2.已领取过的优惠券（登录状态下）不再展示，避免重复领取提示
const visibleClaimableCoupons = computed(() =>
  claimableCoupons.value.filter(c => {
    if (c.targetType === 2 && !isVipMember.value) return false
    if (isLogin.value && claimedCouponIds.value.includes(c.id)) return false
    return true
  })
)

/**
 * 拉取当前用户已领取的优惠券模板ID（含已用/已过期）
 * 用于领券中心隐藏已领取的优惠券
 */
async function fetchClaimedCouponIds() {
  if (!isLogin.value) return
  try {
    const res = await getAllCoupons()
    if (res && res.code === 200 && Array.isArray(res.data)) {
      claimedCouponIds.value = res.data.map(uc => uc.couponId)
    }
  } catch (e) {
    console.error('获取已领取优惠券失败:', e)
  }
}

// ===== 用户头像菜单 =====
const profileMenuOpen = ref(false)
// 头像 URL（用户未上传时为空，回退为首字头像）
const avatarUrl = ref(DEFAULT_AVATAR)
// 用户余额（未加载时为 null）
const userBalance = ref(null)
// 取昵称首字符作为头像内容（未提供头像上传时使用首字头像）
const avatarText = computed(() => (nickname.value || '用').charAt(0))
const roleLabel = computed(() => {
  if (isAdminUser.value) return '管理员'
  if (isSellerUser.value) return '商家'
  return '普通用户'
})

/**
 * 拉取当前用户资料（头像 + 余额 + 昵称）
 * 调用 GET /user/me，登录态下由 Token 自动鉴权
 */
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

// ===== 商品数据（分页） =====
const products = ref([])
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const totalProducts = ref(0)
const totalPages = ref(0)
const activeCategory = ref('全部')

/**
 * 从后端 API 获取商品分页列表
 * 调用 GET /api/product/page?page=&size=&category=
 * 支持分类筛选
 */
async function fetchProducts() {
  loading.value = true
  error.value = false
  try {
    const category = activeCategory.value === '全部' ? '' : activeCategory.value
    const res = await getProductPage(currentPage.value, pageSize.value, category, searchKeyword.value)
    if (res && res.code === 200 && res.data) {
      products.value = res.data.records || []
      totalProducts.value = res.data.total || 0
      totalPages.value = res.data.pages || 0
    } else {
      console.error('获取商品列表失败:', res?.mes)
      products.value = []
      totalProducts.value = 0
      totalPages.value = 0
    }
  } catch (e) {
    console.error('获取商品列表异常:', e)
    error.value = true
    products.value = []
    totalProducts.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
    nextTick(() => observeCards())
  }
}

// ===== 分类数据（从后端 API 获取，供导航栏下拉菜单使用） =====
const categories = ref([])

/**
 * 从后端获取分类列表（导航栏下拉菜单数据源）
 * 调用 GET /api/product/categories
 */
async function fetchCategories() {
  try {
    const res = await getCategories()
    if (res && res.code === 200 && res.data) {
      categories.value = Array.isArray(res.data) ? res.data : []
    } else {
      categories.value = []
    }
  } catch (e) {
    console.error('获取分类列表异常:', e)
    categories.value = []
  }
}

/**
 * 所有分类名称（含"全部"）
 */
const allCategoryNames = computed(() => {
  return ['全部', ...categories.value]
})

/**
 * 切换分类筛选
 * 调用后端 API 获取该分类下的商品（分页）
 */
function filterByCategory(name) {
  activeCategory.value = name
  currentPage.value = 1
  fetchProducts()
  setTimeout(() => scrollTo('#products'), 100)
}

/**
 * 导航栏分类下拉菜单选中项
 * 1.按分类筛选商品并滚动到商品区
 * 2.关闭下拉菜单与移动端折叠菜单
 */
function selectCategory(name) {
  catMenuOpen.value = false
  closeMenu()
  filterByCategory(name)
}

/**
 * 点击页面空白处关闭导航浮层（分类下拉菜单 / 用户头像菜单）
 * 通过 document 点击监听实现：点击浮层外部任意区域即收起
 */
function closePopups() {
  catMenuOpen.value = false
  profileMenuOpen.value = false
}

// ===== 关键词搜索（模糊匹配商品名/描述） =====
// 输入框临时值 searchInput，生效值 searchKeyword（提交后固定，便于清除）
const searchInput = ref('')
const searchKeyword = ref('')

/**
 * 执行搜索
 * 1.取输入框关键词
 * 2.重置到第一页
 * 3.重新拉取商品列表（可与分类组合筛选）
 */
function handleSearch() {
  searchKeyword.value = searchInput.value.trim()
  currentPage.value = 1
  fetchProducts()
  setTimeout(() => scrollTo('#products'), 100)
}

/**
 * 清除搜索
 * 1.清空输入框与生效关键词
 * 2.重置到第一页并重新拉取
 */
function clearSearch() {
  searchInput.value = ''
  searchKeyword.value = ''
  currentPage.value = 1
  fetchProducts()
}

/**
 * 分页跳转
 */
function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchProducts()
  scrollTo('#products')
}

/**
 * 计算分页器显示的页码
 */
const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})

/**
 * 打开与卖家的聊天抽屉
 */
function openChat(prod) {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }
  chatSellerId.value = prod.sellerId
  chatSellerName.value = (prod.seller && prod.seller.sellerName) || prod.sellerName || '卖家'
  chatSellerAvatar.value = (prod.seller && prod.seller.sellerAvatar) || ''
  showChat.value = true
}

/**
 * 跳转商品详情页
 */
function goToProductDetail(id) {
  router.push(`/product/${id}`)
}

// ===== 退出登录 =====
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}
function closeMenu() { menuOpen.value = false }
function goProfile() { router.push('/profile') }
function goOrders() { router.push('/orders') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }
function scrollToTop() { window.scrollTo({ top: 0, behavior: 'smooth' }) }

// ===== 轮播数据 =====
const slides = [
  { image: '/uploads/hero/hero-1.jpg', alt: 'Tech Shopping' },
  { image: '/uploads/hero/hero-2.jpg', alt: 'Shopping Mall' },
  { image: '/uploads/hero/hero-3.jpg', alt: 'Digital Products' }
]
const currentSlide = ref(0)
const carouselPaused = ref(false)
let carouselTimer = null

function toggleCarousel() {
  carouselPaused.value = !carouselPaused.value
  if (carouselPaused.value) {
    clearInterval(carouselTimer)
    carouselTimer = null
  } else {
    carouselTimer = setInterval(nextSlide, 5000)
  }
}

function goToSlide(idx) { currentSlide.value = idx; if (!carouselPaused.value) resetCarousel() }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % slides.length }
function resetCarousel() { clearInterval(carouselTimer); if (!carouselPaused.value) carouselTimer = setInterval(nextSlide, 5000) }

// ===== 购物车逻辑 =====
const cart = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

const cartTotal = computed(() => {
  return cart.value.reduce((sum, item) => sum + (item.price || 0) * item.qty, 0)
})

const cartTotalCount = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.qty, 0)
})

function addToCart(prod) {
  const existing = cart.value.find(item => item.id === prod.id)
  if (existing) {
    existing.qty++
  } else {
    cart.value.push({ ...prod, qty: 1 })
  }
  saveCart()
  showCart.value = true
}

function increaseQty(idx) {
  cart.value[idx].qty++
  saveCart()
}

function decreaseQty(idx) {
  if (cart.value[idx].qty > 1) {
    cart.value[idx].qty--
    saveCart()
  }
}

function removeFromCart(idx) {
  cart.value.splice(idx, 1)
  saveCart()
}

function saveCart() {
  localStorage.setItem('cart', JSON.stringify(cart.value))
}

/**
 * 打开地址选择弹窗，准备下单
 * 1.检查登录状态
 * 2.检查购物车是否为空
 * 3.加载已有地址，选中默认地址
 * 4.显示地址选择弹窗
 */
async function goToCheckout() {
  // 检查是否登录
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return
  }
  // 检查购物车是否为空
  if (cart.value.length === 0) {
    toastError('购物车是空的，先去挑选商品吧')
    return
  }

  // 关闭购物车侧边栏，避免弹窗叠加
  showCart.value = false

  // 加载已有地址
  addressMsg.value = ''
  try {
    const res = await getUserAddresses()
    if (res && res.code === 200 && res.data) {
      addresses.value = res.data
      // 自动选中默认地址
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

/**
 * 选择已有地址
 */
function selectAddress(addr) {
  selectedAddressId.value = addr.id
  formReceiverName.value = addr.receiverName
  formReceiverPhone.value = addr.receiverPhone
  formReceiverAddress.value = addr.receiverAddress
  addressMsg.value = ''
}

/**
 * 确认下单
 */
async function confirmCheckout() {
  // 校验地址信息
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

  // 如果选择了新建地址且勾选了保存，先保存地址
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
    const items = cart.value.map(item => ({
      productId: item.id,
      quantity: item.qty
    }))
    const res = await createOrder({
      items,
      receiverName: name,
      receiverPhone: phone,
      receiverAddress: addr,
      remark: '',
      userCouponId: selectedCoupon.value ? selectedCoupon.value.id : null
    })
    if (res && res.code === 200) {
      // 下单成功
      cart.value = []
      saveCart()
      showCart.value = false
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

/**
 * 取消下单
 */
function cancelCheckout() {
  showAddressModal.value = false
  addressMsg.value = ''
}

// ===== 滚动动画 =====
const prodHeader = ref(null)
const prodRefs = reactive({})
let observer = null

onMounted(() => {
  // 加载商品、分类、可领优惠券和用户资料（头像/余额/昵称）
  fetchProducts()
  fetchCategories()
  fetchUserProfile()
  fetchClaimableCoupons()
  fetchClaimedCouponIds()

  window.addEventListener('scroll', onScroll)
  // 点击页面空白处关闭导航浮层（分类下拉菜单 / 用户头像菜单）
  document.addEventListener('click', closePopups)
  carouselTimer = setInterval(nextSlide, 5000)

  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.10, rootMargin: '0px 0px -60px 0px' })

  observeCards()
})

function observeCards() {
  if (!observer) return
  if (typeof IntersectionObserver === 'undefined') {
    document.querySelectorAll('.section-header, .product-card')
      .forEach(el => el.classList.add('visible'))
    return
  }
  const targets = [
    prodHeader.value,
    ...Object.values(prodRefs)
  ].filter(Boolean)
  targets.forEach(el => {
    const rect = el.getBoundingClientRect()
    if (rect.top < window.innerHeight && rect.bottom > 0) {
      el.classList.add('visible')
      observer.unobserve(el)
    } else {
      observer.observe(el)
    }
  })
}

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  // 移除导航浮层的点击关闭监听
  document.removeEventListener('click', closePopups)
  clearInterval(carouselTimer)
  if (observer) observer.disconnect()
})

function onScroll() { scrolled.value = window.scrollY > 60 }
function scrollTo(selector) {
  const el = document.querySelector(selector)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
/* ===== Reset & Base ===== */
.store-page {
  font-family: 'DM Sans', -apple-system, sans-serif;
  color: #212529;
  background: #ffffff;
  line-height: 1.6;
  overflow-x: hidden;
  -webkit-font-smoothing: antialiased;
}
img { max-width: 100%; height: auto; display: block; }
a { text-decoration: none; color: inherit; }
.section { padding: 44px 0; }
.container { max-width: 1240px; margin: 0 auto; padding: 0 40px; }

/* ===== Announce Bar ===== */
.announce-bar {
  position: fixed; top: 0; left: 0; right: 0;
  height: 36px; z-index: 1001;
  display: flex; align-items: center;
  background: linear-gradient(90deg, #1d39c4 0%, #2f54eb 45%, #0e7490 100%);
  color: rgba(255,255,255,0.92);
  font-size: 0.74rem;
  letter-spacing: 0.03em;
}
.announce-inner {
  max-width: var(--container, 1240px);
  width: 100%;
  margin: 0 auto;
  padding: 0 56px;
  display: flex; align-items: center; justify-content: center;
  gap: 18px;
  white-space: nowrap; overflow: hidden;
}
.announce-item { font-weight: 500; }
.announce-dot { opacity: 0.5; }

/* ===== Glass Navigation ===== */
.nav {
  position: fixed; top: 36px; left: 0; right: 0;
  height: 80px; z-index: 1000;
  display: flex; align-items: center; justify-content: space-between;
  gap: 32px;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
/* 导航搜索框 */
.nav-search {
  flex: 1; max-width: 480px;
  display: flex; align-items: center; gap: 0;
  background: rgba(255,255,255,0.72);
  border: 1px solid var(--border, #e4e9f0);
  border-radius: var(--radius-pill, 999px);
  padding-left: 16px;
  transition: all 0.3s;
  overflow: hidden;
}
.nav-search:focus-within {
  border-color: var(--primary, #2f54eb);
  box-shadow: 0 0 0 3px rgba(47,84,235,0.10);
  background: #fff;
}
.nav-search-input {
  flex: 1; border: none; background: transparent;
  padding: 10px 0; font-size: 0.88rem; outline: none;
  color: var(--text, #17233d);
  min-width: 0;
}
.nav-search-input::placeholder { color: var(--text-3, #8a94a6); }
.nav-search-btn {
  flex-shrink: 0;
  border: none; background: linear-gradient(135deg, var(--primary, #2f54eb), var(--accent, #13c2c2));
  color: #fff; font-size: 1.1rem;
  width: 44px; align-self: stretch;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: opacity 0.2s;
}
.nav-search-btn:hover { opacity: 0.9; }
.nav.scrolled { background: rgba(255,255,255,0.85); box-shadow: 0 1px 40px rgba(0,0,0,0.06); }
.nav-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.6rem; font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0 0%, #4a9eff 50%, #7c3aed 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  letter-spacing: -0.03em; cursor: pointer;
}
.nav-links { display: flex; align-items: center; gap: 32px; list-style: none; margin: 0; padding: 0; }
.nav-links a {
  font-size: 0.875rem; font-weight: 500; color: #868e96;
  letter-spacing: 0.04em; text-transform: uppercase;
  position: relative; transition: color 0.3s; padding: 4px 0;
}
.nav-links a::after {
  content: ''; position: absolute; bottom: 0; left: 0;
  width: 0; height: 2px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  transition: width 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav-links a:hover { color: #212529; }
.nav-links a:hover::after { width: 100%; }
.nav-user { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; padding: 0 4px; }
.nav-cart-btn {
  position: relative; background: none; border: none; cursor: pointer;
  font-size: 1.3rem; padding: 4px; transition: transform 0.3s;
}
.nav-cart-btn:hover { transform: scale(1.1); }
.cart-badge {
  position: absolute; top: -4px; right: -6px;
  min-width: 18px; height: 18px; border-radius: 9px;
  background: linear-gradient(135deg, #e03131, #c92a2a);
  color: white; font-size: 0.65rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  line-height: 1; padding: 0 4px;
}
.nav-profile-btn, .nav-logout {
  padding: 8px 14px; border-radius: 50px;
  font-family: 'DM Sans', sans-serif; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s;
  min-height: 32px; display: flex; align-items: center;
}
.nav-profile-btn {
  border: 1px solid #dee2e6; background: transparent; color: #2b6cb0;
}
.nav-profile-btn:hover { border-color: #2b6cb0; background: rgba(43,108,176,0.04); transform: translateY(-1px); }
.nav-admin-btn { color: #7c3aed; border-color: rgba(124,58,237,0.2); }
.nav-admin-btn:hover { border-color: #7c3aed; background: rgba(124,58,237,0.04); }
.nav-logout {
  border: 1px solid #dee2e6; background: transparent; color: #868e96;
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

/* ===== 导航栏分类下拉菜单 ===== */
.nav-dropdown { position: relative; }
.dropdown-caret {
  display: inline-block; font-size: 0.7rem; margin-left: 4px;
  transition: transform 0.3s; color: inherit;
}
.nav-dropdown:hover .dropdown-caret { transform: rotate(180deg); }
.dropdown-menu {
  position: absolute; top: calc(100% + 14px); left: 50%;
  transform: translateX(-50%);
  /* 多列网格布局：种类较多时分列展示，避免单列过长超出屏幕 */
  display: grid; grid-template-columns: repeat(3, minmax(96px, 1fr));
  gap: 2px;
  min-width: 360px; max-width: 480px;
  /* 种类过多时限制最大高度并滚动，防止超出视口 */
  max-height: 70vh; overflow-y: auto;
  padding: 8px;
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.60);
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.10);
}
/* 下拉滚动条美化 */
.dropdown-menu::-webkit-scrollbar { width: 6px; }
.dropdown-menu::-webkit-scrollbar-thumb { background: #ced4da; border-radius: 3px; }
.dropdown-menu::-webkit-scrollbar-thumb:hover { background: #adb5bd; }
/* 下拉动画 */
.dropdown-enter-active, .dropdown-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateX(-50%) translateY(-6px); }
.dropdown-item {
  display: block; width: 100%; text-align: left;
  padding: 10px 14px; border: none; border-radius: 8px;
  background: transparent; cursor: pointer;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem;
  color: #495057; transition: all 0.2s;
}
.dropdown-item:hover { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.dropdown-item.active { background: linear-gradient(135deg, rgba(43,108,176,0.10), rgba(124,58,237,0.10)); color: #2b6cb0; font-weight: 600; }
.nav-toggle { display: none; flex-direction: column; gap: 5px; cursor: pointer; background: none; border: none; padding: 4px; }
.nav-toggle span { display: block; width: 24px; height: 2px; background: #212529; border-radius: 2px; transition: all 0.3s; }

/* ===== 横向分类导航条 ===== */
.category-bar {
  background: #ffffff;
  border-bottom: 1px solid #f1f3f5;
  padding: 14px 0 8px;
}
.category-bar-inner {
  display: flex; align-items: center; gap: 10px;
  overflow-x: auto; padding-bottom: 8px;
  scrollbar-width: none;               /* Firefox 隐藏滚动条 */
  -webkit-overflow-scrolling: touch;
}
.category-bar-inner::-webkit-scrollbar { display: none; }  /* Chrome/Safari */
.category-chip {
  flex-shrink: 0;
  padding: 8px 18px; border-radius: 50px;
  border: 1px solid #e9ecef; background: #f8f9fa;
  color: #495057; font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem; font-weight: 500; cursor: pointer;
  white-space: nowrap; transition: all 0.25s;
}
.category-chip:hover { border-color: #4a9eff; color: #2b6cb0; }
.category-chip.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  border-color: transparent; color: #fff; font-weight: 600;
  box-shadow: 0 4px 14px rgba(43,108,176,0.25);
}

/* ===== Hero ===== */
.hero {
  position: relative; height: 100vh; min-height: 700px; overflow: hidden;
}
.hero-slide {
  position: absolute; inset: 0; opacity: 0; transition: opacity 1.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.hero-slide.active { opacity: 1; }
.hero-slide img {
  width: 100%; height: 100%; object-fit: cover;
  filter: blur(2px) saturate(1.05); transform: scale(1.05); transition: transform 8s ease;
}
.hero-slide.active img { transform: scale(1.0); }
.hero-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0.20) 40%, rgba(0,0,0,0.05) 60%, rgba(0,0,0,0.30) 100%);
}
.hero-content {
  position: absolute; bottom: 15%; left: 0; right: 0;
  padding: 0 56px; max-width: 1240px; margin: 0 auto; z-index: 2;
}
.hero-badge {
  display: inline-block; padding: 8px 20px;
  background: rgba(255,255,255,0.12); backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.15); border-radius: 50px;
  color: rgba(255,255,255,0.85); font-size: 0.75rem; font-weight: 500;
  letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 24px;
  animation: fadeUp 0.8s 0.3s both;
}
.hero-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(3rem, 8vw, 6.5rem); font-weight: 700; color: white;
  line-height: 1.08; letter-spacing: -0.03em; max-width: 800px;
  animation: fadeUp 0.8s 0.5s both;
}
.hero-sub {
  font-size: clamp(1rem, 2vw, 1.25rem); font-weight: 300;
  color: rgba(255,255,255,0.75); max-width: 540px; margin-top: 20px; line-height: 1.7;
  animation: fadeUp 0.8s 0.7s both;
}
.hero-actions { display: flex; gap: 16px; margin-top: 36px; animation: fadeUp 0.8s 0.9s both; }
.btn-primary, .btn-outline {
  padding: 16px 40px; border-radius: 50px;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  letter-spacing: 0.04em; text-transform: uppercase; cursor: pointer;
  transition: all 0.3s;
}
.btn-primary {
  border: none;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
}
.btn-primary:hover { transform: translateY(-3px); box-shadow: 0 12px 32px rgba(43,108,176,0.40); }
.btn-outline {
  border: 1px solid rgba(255,255,255,0.30); background: transparent; color: white;
}
.btn-outline:hover { background: rgba(255,255,255,0.10); border-color: rgba(255,255,255,0.50); }
.carousel-dots {
  position: absolute; bottom: 8%; left: 56px; display: flex; gap: 12px; z-index: 2;
}
.carousel-dot {
  width: 40px; height: 3px; background: rgba(255,255,255,0.25);
  border-radius: 2px; cursor: pointer; transition: all 0.5s; border: none;
}
.carousel-dot.active { background: rgba(255,255,255,0.85); width: 60px; }

.carousel-pause-btn {
  position: absolute; bottom: 8%; right: 56px; z-index: 2;
  width: 40px; height: 40px; border-radius: 50%;
  background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.25);
  color: rgba(255,255,255,0.75); font-size: 1rem; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s; backdrop-filter: blur(4px);
}
.carousel-pause-btn:hover { background: rgba(255,255,255,0.25); color: white; }

/* ===== Section Header ===== */
.section-header {
  text-align: center; max-width: 680px; margin: 0 auto 24px;
  opacity: 0; transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.section-header.visible { opacity: 1; transform: translateY(0); }
.section-tag {
  display: inline-block; padding: 4px 14px; background: #f1f3f5; border-radius: 50px;
  font-size: 0.68rem; font-weight: 600; color: #868e96;
  letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 10px;
}
.section-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(1.6rem, 3vw, 2.2rem); font-weight: 700; color: #212529;
  line-height: 1.2; letter-spacing: -0.02em;
}
.section-title .highlight {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.section-desc { font-size: 0.95rem; color: #868e96; margin-top: 8px; line-height: 1.6; }

/* ===== Products ===== */
.products { background: #ffffff; }
/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}
.search-input:focus { border-color: #4a9eff; }
.search-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #4a9eff, #1c7ed6);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.95rem;
  white-space: nowrap;
}
.search-clear {
  padding: 10px 16px;
  background: transparent;
  color: #868e96;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
}
.products-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;
}
.product-card {
  background: #ffffff; border: 1px solid #dee2e6; border-radius: 20px;
  overflow: hidden; transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0; transform: translateY(40px); cursor: pointer;
}
.product-card.visible { opacity: 1; transform: translateY(0); }
.product-card:hover {
  transform: translateY(-8px); box-shadow: 0 20px 60px rgba(0,0,0,0.08);
  border-color: transparent;
}
.product-card.visible:hover { transform: translateY(-8px); }
.product-image {
  position: relative; aspect-ratio: 1; overflow: hidden;
  background: #f8f9fa;
}
.product-image img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.product-card:hover .product-image img { transform: scale(1.06); }
.product-tag {
  position: absolute; top: 12px; left: 12px;
  padding: 4px 12px; border-radius: 50px;
  background: linear-gradient(135deg, #e03131, #c92a2a);
  color: white; font-size: 0.7rem; font-weight: 600;
}
.product-tag.tag-new {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
}
.product-info { padding: 20px; }
.product-name {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1rem; font-weight: 600; color: #212529; margin-bottom: 6px;
}
.product-desc {
  font-size: 0.8rem; color: #868e96; line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.product-meta {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; font-size: 0.75rem; color: #adb5bd;
}
.product-seller { color: #2b6cb0; font-weight: 500; }
.product-bottom { display: flex; align-items: center; justify-content: space-between; }
.product-price { font-size: 1.15rem; font-weight: 700; color: #2b6cb0; }
.product-actions { display: flex; align-items: center; gap: 8px; }
.chat-btn {
  padding: 8px 12px; border: 1px solid rgba(43,108,176,0.3); border-radius: 50px;
  background: transparent; color: #2b6cb0; font-family: 'DM Sans', sans-serif;
  font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.3s;
}
.chat-btn:hover { background: rgba(43,108,176,0.06); border-color: #2b6cb0; transform: translateY(-1px); }
.add-cart-btn {
  padding: 8px 16px; border: none; border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif;
  font-size: 0.75rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.add-cart-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(43,108,176,0.30); }

/* ===== Pagination ===== */
.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 8px; margin-top: 32px; flex-wrap: wrap;
}
.page-btn {
  padding: 10px 18px; border: 1px solid #dee2e6; border-radius: 10px;
  background: transparent; color: #495057; font-size: 0.85rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
  min-width: 44px; text-align: center;
}
.page-btn:hover:not(:disabled):not(.active) {
  border-color: #4a9eff; color: #4a9eff;
}
.page-btn.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  border-color: transparent; color: white;
}
.page-btn:disabled {
  opacity: 0.4; cursor: not-allowed;
}

/* ===== 领券中心 ===== */
.coupon-center { background: linear-gradient(180deg, #ffffff 0%, #f6f8fc 100%); }
.coupon-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px;
}
.coupon-card {
  display: flex; align-items: stretch; background: #fff;
  border: 1px solid #e9ecef; border-radius: 16px; overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.04);
  transition: all 0.3s;
  position: relative;
}
/* VIP券专属角标 */
.coupon-vip-tag {
  position: absolute; top: 10px; right: 10px; z-index: 2;
  padding: 3px 10px; border-radius: 999px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff; font-size: 0.68rem; font-weight: 600;
  box-shadow: 0 4px 10px rgba(217,119,6,0.3);
}
.coupon-card:hover { transform: translateY(-4px); box-shadow: 0 16px 40px rgba(0,0,0,0.08); }
.coupon-value {
  flex: 0 0 132px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(160deg, #2b6cb0, #4a9eff); color: #fff; padding: 16px 12px;
  position: relative;
}
/* 券面打孔装饰（纯视觉） */
.coupon-value::after {
  content: ''; position: absolute; right: -11px; top: 50%;
  width: 22px; height: 22px; transform: translateY(-50%);
  border-radius: 50%; background: #f6f8fc;
}
.coupon-value-sym { font-size: 0.9rem; font-weight: 600; }
.coupon-value-num {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2rem; font-weight: 700; line-height: 1.1;
}
.coupon-value-unit { font-size: 0.72rem; opacity: 0.9; margin-top: 2px; }
.coupon-body { flex: 1; padding: 18px 20px; display: flex; flex-direction: column; align-items: flex-start; }
.coupon-name { font-size: 0.95rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.coupon-threshold { font-size: 0.78rem; color: #868e96; margin-bottom: 14px; }
.coupon-claim-btn {
  margin-top: auto; padding: 8px 20px; border: none; border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: #fff;
  font-family: 'DM Sans', sans-serif; font-size: 0.8rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.coupon-claim-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(43,108,176,0.3); }
.coupon-claim-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 服务保障栏（页尾） ===== */
.guarantee-bar { background: #ffffff; border-top: 1px solid #f1f3f5; padding: 24px 0; }
.guarantee-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;
}
.guarantee-item {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 20px; border-radius: 14px;
  background: #f8f9fa; border: 1px solid #f1f3f5;
  transition: all 0.3s;
}
.guarantee-item:hover { background: #fff; box-shadow: 0 8px 24px rgba(0,0,0,0.05); transform: translateY(-2px); }
.guarantee-icon {
  width: 48px; height: 48px; flex-shrink: 0; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 1.4rem;
  background: linear-gradient(135deg, rgba(43,108,176,0.12), rgba(124,58,237,0.12));
}
.guarantee-text h4 { font-size: 0.95rem; font-weight: 600; color: #212529; margin-bottom: 2px; }
.guarantee-text p { font-size: 0.8rem; color: #868e96; }

/* ===== Loading & Empty & Error ===== */
.loading-state, .empty-state {
  text-align: center; padding: 80px 0; color: #868e96;
}
.loading-spinner {
  width: 40px; height: 40px; margin: 0 auto 16px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-icon, .empty-icon { font-size: 3rem; margin-bottom: 16px; }
.empty-hint { font-size: 0.85rem; color: #adb5bd; margin-top: 8px; }
.retry-btn {
  margin-top: 16px; padding: 10px 24px; border: 1px solid #2b6cb0;
  border-radius: 50px; background: transparent; color: #2b6cb0;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.retry-btn:hover { background: rgba(43,108,176,0.06); }

/* ===== Cart Sidebar ===== */
.cart-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.30); opacity: 0; pointer-events: none;
  transition: opacity 0.4s;
}
.cart-overlay.open { opacity: 1; pointer-events: auto; }
.cart-sidebar {
  position: fixed; top: 0; right: 0; bottom: 0; width: 400px; max-width: 90vw;
  z-index: 2001; background: white;
  transform: translateX(100%); transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  display: flex; flex-direction: column;
  box-shadow: -8px 0 40px rgba(0,0,0,0.08);
}
.cart-sidebar.open { transform: translateX(0); }
.cart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px; border-bottom: 1px solid #dee2e6;
}
.cart-header h3 { font-family: 'Playfair Display', Georgia, serif; font-size: 1.3rem; color: #212529; }
.cart-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #868e96; transition: color 0.3s; }
.cart-close:hover { color: #212529; }
.cart-body { flex: 1; overflow-y: auto; padding: 16px 24px; }
.cart-item {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 0; border-bottom: 1px solid #f1f3f5;
}
.cart-item-img { width: 60px; height: 60px; border-radius: 12px; object-fit: cover; background: #f8f9fa; }
.cart-item-info { flex: 1; min-width: 0; }
.cart-item-info h4 { font-size: 0.85rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.cart-item-price { font-size: 0.85rem; font-weight: 700; color: #2b6cb0; }
.cart-item-qty { display: flex; align-items: center; gap: 8px; }
.cart-item-qty button {
  width: 28px; height: 28px; border-radius: 50%;
  border: 1px solid #dee2e6; background: transparent;
  font-size: 1rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.cart-item-qty button:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.cart-item-qty button:disabled { opacity: 0.3; cursor: not-allowed; }
.cart-item-qty span { font-size: 0.85rem; font-weight: 600; min-width: 20px; text-align: center; }
.cart-item-remove {
  background: none; border: none; color: #adb5bd; font-size: 0.85rem;
  cursor: pointer; transition: color 0.3s; padding: 4px;
}
.cart-item-remove:hover { color: #dc3545; }
.cart-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; color: #adb5bd; gap: 8px;
}
.cart-empty-icon { font-size: 3rem; }
.cart-empty-hint { font-size: 0.85rem; }
.cart-footer {
  padding: 20px 24px; border-top: 1px solid #dee2e6;
}
.cart-total { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.cart-total span { font-size: 0.9rem; color: #868e96; }
.cart-total-price { font-size: 1.3rem; font-weight: 700; color: #212529; }
.checkout-btn {
  width: 100%; padding: 16px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif; font-size: 0.9rem;
  font-weight: 600; cursor: pointer; transition: all 0.3s;
}
.checkout-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(43,108,176,0.30); }

/* ===== Footer ===== */
.footer {
  background: #212529; border-top: 1px solid rgba(255,255,255,0.06);
  padding: 48px 56px;
}
.footer-inner {
  max-width: 1240px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
}
.footer-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.1rem; font-weight: 700; color: rgba(255,255,255,0.60);
  letter-spacing: -0.02em;
}
.footer-links { display: flex; gap: 32px; }
.footer-links a { font-size: 0.75rem; color: #868e96; letter-spacing: 0.04em; text-transform: uppercase; transition: color 0.3s; }
.footer-links a:hover { color: #ced4da; }
.footer-copy { font-size: 0.75rem; color: #495057; }

/* ===== Keyframes ===== */
@keyframes fadeUp { to { opacity: 1; transform: translateY(0); } }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .products-grid { grid-template-columns: repeat(2, 1fr); }
  .guarantee-grid { grid-template-columns: repeat(2, 1fr); }
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

@media (max-width: 768px) {
  .address-modal { padding: 24px 20px; }
  .address-form-row { flex-direction: column; gap: 8px; }
  .address-modal-actions { flex-direction: column; }
  .address-btn { width: 100%; text-align: center; }
  .nav { padding: 0 24px; }
  .nav-links {
    display: none; flex-direction: column;
    position: absolute; top: 80px; left: 0; right: 0;
    background: rgba(255,255,255,0.92); backdrop-filter: blur(20px);
    padding: 24px 32px; gap: 20px;
    border-bottom: 1px solid #dee2e6;
  }
  .nav-links.open { display: flex; }
  .nav-toggle { display: flex; }
  /* 移动端：分类下拉改为静态单列展开，避免浮层遮挡与宽度溢出 */
  .nav-dropdown { width: 100%; }
  .dropdown-menu {
    position: static; transform: none;
    width: 100%; min-width: 0; max-width: none;
    grid-template-columns: 1fr;
    max-height: none; overflow-y: visible;
    margin-top: 8px;
    box-shadow: none; border: 1px solid #dee2e6;
  }
  .dropdown-enter-from, .dropdown-leave-to { transform: translateY(-6px); }
  .nav-user { margin-top: 8px; padding-top: 16px; border-top: 1px solid #dee2e6; width: 100%; justify-content: space-between; flex-wrap: wrap; }
  /* 移动端：头像菜单右对齐，避免超出屏幕 */
  .avatar-wrapper { flex: 1; justify-content: flex-end; }
  .profile-menu { right: 0; }
  .hero-content { padding: 0 24px; bottom: 20%; }
  .hero-actions { flex-direction: column; gap: 12px; }
  .section { padding: 32px 0; }
  .container { padding: 0 24px; }
  /* 移动端商品 2 列，避免一屏只见一种商品 */
  .products-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .product-info { padding: 12px; }
  .product-name { font-size: 0.9rem; }
  .product-price { font-size: 1rem; }
  .product-actions { flex-wrap: wrap; gap: 6px; }
  .chat-btn { padding: 6px 10px; }
  .add-cart-btn { padding: 6px 12px; font-size: 0.7rem; }
  /* 分类条横向滑动 */
  .category-bar { padding: 8px 0 4px; }
  .category-bar-inner { gap: 8px; }
  /* 领券中心单列 */
  .coupon-grid { grid-template-columns: 1fr; gap: 14px; }
  .coupon-value { flex-basis: 120px; }
  /* 服务保障两列 */
  .guarantee-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .guarantee-item { padding: 12px 14px; gap: 10px; }
  .guarantee-icon { width: 40px; height: 40px; font-size: 1.15rem; }
  .carousel-dots { left: 24px; }
  .pagination { gap: 4px; }
  .page-btn { padding: 8px 14px; font-size: 0.8rem; min-width: 38px; }
  .footer { padding: 40px 24px; }
  .footer-inner { flex-direction: column; gap: 24px; text-align: center; }
  .footer-links { flex-wrap: wrap; justify-content: center; gap: 20px; }
}
</style>