<template>
  <div class="store-page">
    <!-- ===== 毛玻璃导航 ===== -->
    <nav class="nav" :class="{ scrolled: scrolled }">
      <div class="nav-logo" @click="scrollToTop">ZuiMShop</div>
      <ul class="nav-links" :class="{ open: menuOpen }">
        <li><a href="#hero" @click.prevent="closeMenu; scrollTo('#hero')">首页</a></li>
        <li><a href="#products" @click.prevent="closeMenu; scrollTo('#products')">商品</a></li>
        <!-- 分类下拉菜单：点击/悬停弹出商品种类列表 -->
        <li class="nav-dropdown"
            @mouseenter="catMenuOpen = true"
            @mouseleave="catMenuOpen = false">
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
          <span class="nav-nickname">{{ nickname }}</span>
          <button class="nav-profile-btn" @click="goProfile">个人中心</button>
          <button class="nav-profile-btn nav-admin-btn" v-if="isAdminUser" @click="goAdmin">管理后台</button>
          <button class="nav-profile-btn" v-if="isSellerUser" @click="goSeller">商家管理</button>
          <button class="nav-logout" @click="handleLogout">退出</button>
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
        <div class="hero-badge">✦ 春季新品首发</div>
        <h1 class="hero-title">科技好物<br>触手可及</h1>
        <p class="hero-sub">精选全球优质科技产品，从数码配件到智能家居，一站式购齐。</p>
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
                <button class="add-cart-btn" @click.stop="addToCart(prod)">加入购物车</button>
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

    <!-- ===== 底部版权栏 ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">ZuiMShop</div>
        <div class="footer-links">
          <a href="#hero" @click.prevent="scrollTo('#hero')">首页</a>
          <a href="#products" @click.prevent="scrollTo('#products')">商品</a>
          <a href="javascript:void(0)" @click="goProfile">我的</a>
        </div>
        <div class="footer-copy">© 2026 ZuiMShop. All rights reserved.</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getProductPage, getCategories, createOrder } from '../api/index.js'
import { getCachedRoles } from '../utils/auth.js'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const scrolled = ref(false)
const menuOpen = ref(false)
const showCart = ref(false)
const catMenuOpen = ref(false)

// ===== 角色判断（用于导航中显示按钮） =====
const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))

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
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/products') }
function scrollToTop() { window.scrollTo({ top: 0, behavior: 'smooth' }) }

// ===== 轮播数据 =====
const slides = [
  { image: '/uploads/hero/hero-1.jpg', alt: 'Tech Shopping' },
  { image: '/uploads/hero/hero-2.jpg', alt: 'Shopping Mall' },
  { image: '/uploads/hero/hero-3.jpg', alt: 'Digital Products' }
]
const currentSlide = ref(0)
let carouselTimer = null
function goToSlide(idx) { currentSlide.value = idx; resetCarousel() }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % slides.length }
function resetCarousel() { clearInterval(carouselTimer); carouselTimer = setInterval(nextSlide, 5000) }

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
 * 购物车结算：用购物车中的商品创建订单
 * 1.检查登录状态
 * 2.收集收货信息（与商品详情页一致的简单弹窗方式）
 * 3.调用后端创建订单接口
 * 4.成功后清空购物车并跳转订单列表
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
    alert('购物车是空的，先去挑选商品吧')
    return
  }
  // 收集收货信息
  const receiverName = prompt('收货人姓名：', (localStorage.getItem('nickname') || ''))
  if (!receiverName) return
  const receiverPhone = prompt('收货人电话：')
  if (!receiverPhone) return
  const receiverAddress = prompt('收货地址：')
  if (!receiverAddress) return

  try {
    // 组装订单项：购物车中每个商品转为 { productId, quantity }
    const items = cart.value.map(item => ({
      productId: item.id,
      quantity: item.qty
    }))
    const res = await createOrder({
      items,
      receiverName,
      receiverPhone,
      receiverAddress,
      remark: ''
    })
    if (res && res.code === 200) {
      // 下单成功：清空购物车并跳转订单列表
      cart.value = []
      saveCart()
      showCart.value = false
      alert('下单成功，请前往订单页支付')
      router.push('/orders')
    } else {
      alert((res && res.mes) || '下单失败')
    }
  } catch (err) {
    alert('下单失败：' + ((err && err.message) || '网络错误'))
  }
}

// ===== 滚动动画 =====
const prodHeader = ref(null)
const prodRefs = reactive({})
let observer = null

onMounted(() => {
  // 加载商品和分类数据
  fetchProducts()
  fetchCategories()

  window.addEventListener('scroll', onScroll)
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
.section { padding: 100px 0; }
.container { max-width: 1240px; margin: 0 auto; padding: 0 40px; }

/* ===== Glass Navigation ===== */
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
.nav-user { display: flex; align-items: center; gap: 12px; }
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; }
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
  padding: 6px 16px; border-radius: 50px;
  font-family: 'DM Sans', sans-serif; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s;
}
.nav-profile-btn {
  border: 1px solid #dee2e6; background: transparent; color: #2b6cb0;
}
.nav-profile-btn:hover { border-color: #2b6cb0; background: rgba(43,108,176,0.04); }
.nav-admin-btn { color: #7c3aed; border-color: rgba(124,58,237,0.2); }
.nav-admin-btn:hover { border-color: #7c3aed; background: rgba(124,58,237,0.04); }
.nav-logout {
  border: 1px solid #dee2e6; background: transparent; color: #868e96;
}
.nav-logout:hover { border-color: #dc3545; color: #dc3545; }

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

/* ===== Section Header ===== */
.section-header {
  text-align: center; max-width: 680px; margin: 0 auto 56px;
  opacity: 0; transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.section-header.visible { opacity: 1; transform: translateY(0); }
.section-tag {
  display: inline-block; padding: 6px 16px; background: #f1f3f5; border-radius: 50px;
  font-size: 0.7rem; font-weight: 600; color: #868e96;
  letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 16px;
}
.section-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(2rem, 4vw, 3.2rem); font-weight: 700; color: #212529;
  line-height: 1.2; letter-spacing: -0.02em;
}
.section-title .highlight {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.section-desc { font-size: 1.05rem; color: #868e96; margin-top: 16px; line-height: 1.7; }

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
  gap: 8px; margin-top: 48px; flex-wrap: wrap;
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
}
@media (max-width: 768px) {
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
  .hero-content { padding: 0 24px; bottom: 20%; }
  .hero-actions { flex-direction: column; gap: 12px; }
  .section { padding: 80px 0; }
  .container { padding: 0 24px; }
  .products-grid { grid-template-columns: 1fr; }
  .carousel-dots { left: 24px; }
  .pagination { gap: 4px; }
  .page-btn { padding: 8px 14px; font-size: 0.8rem; min-width: 38px; }
  .footer { padding: 40px 24px; }
  .footer-inner { flex-direction: column; gap: 24px; text-align: center; }
  .footer-links { flex-wrap: wrap; justify-content: center; gap: 20px; }
}
</style>