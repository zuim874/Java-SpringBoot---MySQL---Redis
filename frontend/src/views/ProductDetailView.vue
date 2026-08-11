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
      <div class="nav-user">
        <span class="nav-nickname" v-if="nickname">{{ nickname }}</span>
        <button class="nav-logout" @click="handleLogout">退出</button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '../api/index.js'

const route = useRoute()
const router = useRouter()

const productId = ref(route.params.id)
const product = ref(null)
const images = ref([])
const loading = ref(true)
const scrolled = ref(false)
const nickname = ref(localStorage.getItem('nickname') || '')
const currentImageIndex = ref(0)
const quantity = ref(1)

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
      name: product.value.productName,
      price: product.value.price,
      image: product.value.mainImageUrl,
      category: product.value.category,
      qty: quantity.value
    })
  }

  localStorage.setItem('cart', JSON.stringify(cart))
  showToast('已加入购物车')
}

// 立即购买
function buyNow() {
  addToCart()
  showToast('已添加到购物车，请前往结算')
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
  fetchProductDetail()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
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
.nav-user { display: flex; align-items: center; gap: 12px; }
.nav-nickname { font-size: 0.85rem; font-weight: 500; color: #2b6cb0; }
.nav-logout {
  padding: 6px 16px; border-radius: 50px;
  border: 1px solid #dee2e6; background: transparent; color: #868e96;
  font-family: 'DM Sans', sans-serif; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s;
}
.nav-logout:hover { border-color: #dc3545; color: #dc3545; }

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
</style>
