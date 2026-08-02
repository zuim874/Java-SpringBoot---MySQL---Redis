<template>
  <div class="store-page">
    <!-- ===== 毛玻璃导航 ===== -->
    <nav class="nav" :class="{ scrolled: scrolled }">
      <div class="nav-logo">XuWenYeShop</div>
      <ul class="nav-links" :class="{ open: menuOpen }">
        <li><a href="#hero" @click="closeMenu">首页</a></li>
        <li><a href="#products" @click="closeMenu">商品</a></li>
        <li><a href="#categories" @click="closeMenu">分类</a></li>
        <li class="nav-user">
          <button class="nav-cart-btn" @click="showCart = true" aria-label="购物车">
            <span class="cart-icon">🛒</span>
            <span class="cart-badge" v-if="cart.length > 0">{{ cart.length }}</span>
          </button>
          <span class="nav-nickname">{{ nickname }}</span>
          <button class="nav-profile-btn" @click="goProfile">个人中心</button>
          <button class="nav-profile-btn" @click="goAdmin">管理</button>
          <button class="nav-logout" @click="handleLogout">退出</button>
        </li>
      </ul>
      <button class="nav-toggle" @click="menuOpen = !menuOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </nav>

    <!-- ===== 轮播大图 ===== -->
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
          <button class="btn-outline" @click="scrollTo('#categories')">浏览分类</button>
        </div>
      </div>
      <div class="carousel-dots">
        <button v-for="(_, idx) in slides" :key="idx"
                class="carousel-dot" :class="{ active: currentSlide === idx }"
                @click="goToSlide(idx)" :aria-label="'Slide ' + (idx+1)"></button>
      </div>
    </section>

    <!-- ===== 分类导航 ===== -->
    <section class="section categories" id="categories">
      <div class="container">
        <div class="section-header" ref="catHeader">
          <span class="section-tag">商品分类</span>
          <h2 class="section-title">一站式<span class="highlight">科技购物</span></h2>
          <p class="section-desc">覆盖数码配件、智能家居、办公设备等热门品类，满足你的所有需求。</p>
        </div>
        <div class="cats-grid">
          <div v-for="(cat, idx) in categories" :key="idx"
               class="cat-card" :ref="el => { if(el) catRefs[idx] = el }"
               @click="filterByCategory(cat.name)">
            <div class="cat-icon" v-html="cat.icon"></div>
            <h3 class="cat-name">{{ cat.name }}</h3>
            <span class="cat-count">{{ cat.count }} 件商品</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 商品列表 ===== -->
    <section class="section products" id="products">
      <div class="container">
        <div class="section-header" ref="prodHeader">
          <span class="section-tag">精选商品</span>
          <h2 class="section-title">热门<span class="highlight">推荐</span></h2>
          <p class="section-desc">{{ activeCategory === '全部' ? '所有商品' : activeCategory }} · 共 {{ filteredProducts.length }} 件</p>
        </div>
        <div class="filter-tabs">
          <button v-for="cat in ['全部', ...categories.map(c => c.name)]" :key="cat"
                  class="filter-tab" :class="{ active: activeCategory === cat }"
                  @click="filterByCategory(cat)">{{ cat }}</button>
        </div>
        <div class="products-grid">
          <div v-for="(prod, idx) in filteredProducts" :key="prod.id"
               class="product-card" :ref="el => { if(el) prodRefs[idx] = el }"
               :style="{ transitionDelay: (idx % 4) * 0.08 + 's' }">
            <div class="product-image">
              <img :src="prod.image" :alt="prod.name" loading="lazy">
              <div class="product-tag" v-if="prod.tag">{{ prod.tag }}</div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ prod.name }}</h3>
              <p class="product-desc">{{ prod.desc }}</p>
              <div class="product-bottom">
                <span class="product-price">¥{{ prod.price.toFixed(2) }}</span>
                <button class="add-cart-btn" @click.stop="addToCart(prod)">加入购物车</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 购物车侧边栏 ===== -->
    <div class="cart-overlay" :class="{ open: showCart }" @click="showCart = false"></div>
    <div class="cart-sidebar" :class="{ open: showCart }">
      <div class="cart-header">
        <h3>购物车</h3>
        <button class="cart-close" @click="showCart = false">✕</button>
      </div>
      <div class="cart-body" v-if="cart.length > 0">
        <div v-for="(item, idx) in cart" :key="idx" class="cart-item">
          <img :src="item.image" :alt="item.name" class="cart-item-img">
          <div class="cart-item-info">
            <h4>{{ item.name }}</h4>
            <p class="cart-item-price">¥{{ item.price.toFixed(2) }}</p>
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
        <button class="checkout-btn">去结算</button>
      </div>
    </div>

    <!-- ===== 底部版权栏 ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">XuWenYeShop</div>
        <div class="footer-links">
          <a href="#hero">首页</a>
          <a href="#products">商品</a>
          <a href="#categories">分类</a>
          <a href="javascript:void(0)" @click="goProfile">我的</a>
        </div>
        <div class="footer-copy">© 2026 XuWenYeShop. All rights reserved.</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const scrolled = ref(false)
const menuOpen = ref(false)
const showCart = ref(false)
const activeCategory = ref('全部')

// ===== 退出登录 =====
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}
function closeMenu() { menuOpen.value = false }
function goProfile() { router.push('/profile') }
function goAdmin() { router.push('/admin/users') }

// ===== 轮播数据 =====
const slides = [
  { image: 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=1920&q=85', alt: 'Tech Shopping' },
  { image: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=1920&q=85', alt: 'Shopping Mall' },
  { image: 'https://images.unsplash.com/photo-1556742111-a301076d9d18?w=1920&q=85', alt: 'Digital Products' }
]
const currentSlide = ref(0)
let carouselTimer = null
function goToSlide(idx) { currentSlide.value = idx; resetCarousel() }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % slides.length }
function resetCarousel() { clearInterval(carouselTimer); carouselTimer = setInterval(nextSlide, 5000) }

// ===== 分类数据 =====
const categories = [
  { icon: '📱', name: '手机配件', count: 24 },
  { icon: '💻', name: '电脑外设', count: 18 },
  { icon: '🎧', name: '音频设备', count: 15 },
  { icon: '🏠', name: '智能家居', count: 12 },
  { icon: '⌚', name: '穿戴设备', count: 9 },
  { icon: '📷', name: '摄影器材', count: 7 }
]

// ===== 商品数据（模拟） =====
const products = [
  { id: 1, name: '极速无线充电器', desc: '15W 快充 · 兼容 iPhone/Android', price: 129.00, image: 'https://images.unsplash.com/photo-1583394838336-acd977736f90?w=400&q=80', category: '手机配件', tag: '热卖' },
  { id: 2, name: '机械键盘 K8 Pro', desc: '87键 · 青轴 · RGB背光', price: 399.00, image: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400&q=80', category: '电脑外设', tag: '新品' },
  { id: 3, name: '降噪耳机 AirSound', desc: '主动降噪 · 40h续航 · 蓝牙5.3', price: 599.00, image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', category: '音频设备', tag: '推荐' },
  { id: 4, name: '智能台灯 Lumina', desc: '无级调光 · 色温调节 · 护眼', price: 249.00, image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80', category: '智能家居', tag: '' },
  { id: 5, name: '运动手环 FitBand', desc: '心率监测 · 睡眠分析 · IP68防水', price: 199.00, image: 'https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=400&q=80', category: '穿戴设备', tag: '特价' },
  { id: 6, name: '便携三脚架 ProPod', desc: '碳纤维 · 1.5kg承重 · 折叠便携', price: 159.00, image: 'https://images.unsplash.com/photo-1586101447506-1e0b6b312f63?w=400&q=80', category: '摄影器材', tag: '' },
  { id: 7, name: '手机壳 防摔系列', desc: '军工级防摔 · 磁吸兼容 · 多色可选', price: 49.00, image: 'https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=400&q=80', category: '手机配件', tag: '爆款' },
  { id: 8, name: '显示器支架 ArmOne', desc: '气动悬臂 · 17-32寸 · 理线设计', price: 299.00, image: 'https://images.unsplash.com/photo-1611532736597-de2d4265fba3?w=400&q=80', category: '电脑外设', tag: '' },
  { id: 9, name: '蓝牙音箱 MiniBeat', desc: '360°环绕声 · 12h续航 · 防水', price: 179.00, image: 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=400&q=80', category: '音频设备', tag: '' },
  { id: 10, name: '智能插座 SmartPlug', desc: '远程控制 · 电量统计 · 语音控制', price: 89.00, image: 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=400&q=80', category: '智能家居', tag: '特价' },
  { id: 11, name: '智能手表 WatchX', desc: 'AMOLED屏 · eSIM · 7天续航', price: 899.00, image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&q=80', category: '穿戴设备', tag: '高端' },
  { id: 12, name: 'USB-C 扩展坞 HubMax', desc: '12合1 · 4K60Hz · 100W PD', price: 259.00, image: 'https://images.unsplash.com/photo-1623869675781-80aa31029cb0?w=400&q=80', category: '电脑外设', tag: '推荐' }
]

const filteredProducts = computed(() => {
  if (activeCategory.value === '全部') return products
  return products.filter(p => p.category === activeCategory.value)
})

function filterByCategory(name) {
  activeCategory.value = name
  setTimeout(() => scrollTo('#products'), 100)
}

// ===== 购物车逻辑 =====
const cart = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

const cartTotal = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.price * item.qty, 0)
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

// ===== 滚动动画 =====
const catHeader = ref(null)
const prodHeader = ref(null)
const catRefs = reactive({})
const prodRefs = reactive({})
let observer = null

onMounted(() => {
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

  const targets = [
    catHeader.value, prodHeader.value,
    ...Object.values(catRefs), ...Object.values(prodRefs)
  ].filter(Boolean)
  targets.forEach(el => observer.observe(el))
})

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
  letter-spacing: -0.03em;
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
.nav-logout {
  border: 1px solid #dee2e6; background: transparent; color: #868e96;
}
.nav-logout:hover { border-color: #dc3545; color: #dc3545; }
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

/* ===== Categories ===== */
.categories { background: #f8f9fa; }
.cats-grid {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px;
}
.cat-card {
  background: #ffffff; border: 1px solid #dee2e6; border-radius: 20px;
  padding: 32px 20px; text-align: center; cursor: pointer;
  transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0; transform: translateY(40px);
}
.cat-card.visible { opacity: 1; transform: translateY(0); }
.cat-card:hover {
  transform: translateY(-6px); box-shadow: 0 16px 48px rgba(0,0,0,0.08);
  border-color: transparent;
  background: linear-gradient(135deg, #f8f9ff, #ffffff);
}
.cat-card.visible:hover { transform: translateY(-6px); }
.cat-icon { font-size: 2.4rem; margin-bottom: 12px; }
.cat-name { font-size: 0.9rem; font-weight: 600; color: #212529; margin-bottom: 4px; }
.cat-count { font-size: 0.75rem; color: #adb5bd; }

/* ===== Products ===== */
.products { background: #ffffff; }
.filter-tabs {
  display: flex; justify-content: center; flex-wrap: wrap; gap: 8px;
  margin-bottom: 40px;
}
.filter-tab {
  padding: 8px 20px; border: 1px solid #dee2e6; border-radius: 50px;
  background: transparent; color: #868e96; font-size: 0.8rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.filter-tab:hover { border-color: #4a9eff; color: #4a9eff; }
.filter-tab.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  border-color: transparent; color: white;
}
.products-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;
}
.product-card {
  background: #ffffff; border: 1px solid #dee2e6; border-radius: 20px;
  overflow: hidden; transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0; transform: translateY(40px);
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
.product-info { padding: 20px; }
.product-name {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1rem; font-weight: 600; color: #212529; margin-bottom: 6px;
}
.product-desc { font-size: 0.8rem; color: #868e96; line-height: 1.5; margin-bottom: 16px; }
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
  .cats-grid { grid-template-columns: repeat(3, 1fr); }
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
  .nav-user { margin-top: 8px; padding-top: 16px; border-top: 1px solid #dee2e6; width: 100%; justify-content: space-between; flex-wrap: wrap; }
  .hero-content { padding: 0 24px; bottom: 20%; }
  .hero-actions { flex-direction: column; gap: 12px; }
  .section { padding: 80px 0; }
  .container { padding: 0 24px; }
  .cats-grid { grid-template-columns: repeat(2, 1fr); }
  .products-grid { grid-template-columns: 1fr; }
  .carousel-dots { left: 24px; }
  .footer { padding: 40px 24px; }
  .footer-inner { flex-direction: column; gap: 24px; text-align: center; }
  .footer-links { flex-wrap: wrap; justify-content: center; gap: 20px; }
}
</style>