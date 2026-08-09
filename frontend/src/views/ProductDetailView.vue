<template>
  <div class="detail-page">
    <!-- ===== 顶部导航 ===== -->
    <nav class="detail-nav">
      <button class="back-btn" @click="goBack">← 返回</button>
      <span class="nav-title">商品详情</span>
      <span class="nav-placeholder"></span>
    </nav>

    <!-- ===== 加载状态 ===== -->
    <div class="loading-container" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ===== 商品详情内容 ===== -->
    <div class="detail-content" v-if="!loading && product">
      <!-- 图片展示区 -->
      <div class="image-section">
        <div class="main-image">
          <img :src="currentImage" :alt="product.productName">
        </div>
        <div class="thumbnails" v-if="images.length > 0">
          <img v-for="(img, idx) in images" :key="idx"
               :src="img.imageUrl" :class="{ active: currentImage === img.imageUrl }"
               @click="currentImage = img.imageUrl" loading="lazy">
        </div>
      </div>

      <!-- 商品信息区 -->
      <div class="info-section">
        <!-- 标题与价格 -->
        <div class="product-header">
          <h1 class="product-name">{{ product.productName }}</h1>
          <div class="product-price">¥{{ product.price.toFixed(2) }}</div>
        </div>

        <!-- 商品状态标签 -->
        <div class="status-tags">
          <span class="status-tag" :class="product.status === 1 ? 'on-sale' : 'off-sale'">
            {{ product.status === 1 ? '在售' : '已下架' }}
          </span>
          <span class="status-tag category">{{ product.category }}</span>
        </div>

        <!-- 商品描述 -->
        <div class="product-desc">
          <h3 class="section-label">商品描述</h3>
          <p>{{ product.description }}</p>
        </div>

        <!-- 卖家信息 -->
        <div class="seller-info" v-if="product.sellerName">
          <h3 class="section-label">卖家信息</h3>
          <div class="seller-detail">
            <span class="seller-name">🏪 {{ product.sellerName }}</span>
          </div>
        </div>

        <!-- 销量与库存 -->
        <div class="stats">
          <div class="stat-item">
            <span class="stat-label">已售</span>
            <span class="stat-value">{{ product.sold }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">库存</span>
            <span class="stat-value">{{ product.stock }}</span>
          </div>
        </div>

        <!-- 数量选择与操作 -->
        <div class="actions">
          <div class="qty-selector">
            <button @click="decreaseQty" :disabled="qty <= 1">−</button>
            <span>{{ qty }}</span>
            <button @click="increaseQty" :disabled="qty >= product.stock">+</button>
          </div>
          <button class="add-cart-btn" @click="addToCart">
            加入购物车
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 商品不存在 ===== -->
    <div class="not-found" v-if="!loading && !product">
      <p>商品不存在或已下架</p>
      <button class="back-home-btn" @click="goHome">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { request } from '../utils/request.js'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const images = ref([])
const currentImage = ref('')
const loading = ref(true)
const qty = ref(1)

/**
 * 加载商品详情
 * 1.根据路由参数中的商品 ID 调用 API
 * 2.加载商品详情和图片列表
 * 3.设置默认显示图片
 * <p>
 * @author ZuiM
 */
async function fetchProductDetail() {
  loading.value = true
  const productId = route.params.id
  if (!productId) {
    loading.value = false
    return
  }
  try {
    // 并行加载商品详情和图片
    const [detailRes, imagesRes] = await Promise.all([
      request(`/product/${productId}`),
      request(`/product/${productId}/images`)
    ])
    if (detailRes && detailRes.code === 200) {
      product.value = detailRes.data
      // 设置默认显示图片
      if (detailRes.data.mainImageUrl) {
        currentImage.value = detailRes.data.mainImageUrl
      }
    } else {
      console.error('获取商品详情失败:', detailRes?.mes)
    }
    if (imagesRes && imagesRes.code === 200) {
      images.value = imagesRes.data || []
      // 如果主图未设置，取第一张图片
      if (!currentImage.value && images.value.length > 0) {
        currentImage.value = images.value[0].imageUrl
      }
    }
  } catch (e) {
    console.error('获取商品详情异常:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 返回上一页
 * <p>
 * @author ZuiM
 */
function goBack() {
  router.back()
}

/**
 * 跳转首页
 * <p>
 * @author ZuiM
 */
function goHome() {
  router.push('/home')
}

/**
 * 增加数量
 * <p>
 * @author ZuiM
 */
function increaseQty() {
  if (qty.value < product.value.stock) {
    qty.value++
  }
}

/**
 * 减少数量
 * <p>
 * @author ZuiM
 */
function decreaseQty() {
  if (qty.value > 1) {
    qty.value--
  }
}

/**
 * 加入购物车
 * 1.获取当前购物车数据
 * 2.检查商品是否已存在
 * 3.存在则增加数量，否则添加新项
 * 4.保存到 localStorage
 * <p>
 * @author ZuiM
 */
function addToCart() {
  const cart = JSON.parse(localStorage.getItem('cart') || '[]')
  const existing = cart.find(item => item.id === product.value.id)
  if (existing) {
    existing.qty += qty.value
  } else {
    cart.push({
      id: product.value.id,
      productName: product.value.productName,
      price: product.value.price,
      mainImageUrl: product.value.mainImageUrl,
      qty: qty.value
    })
  }
  localStorage.setItem('cart', JSON.stringify(cart))
  // 轻提示反馈
  alert('已加入购物车')
}

onMounted(() => {
  fetchProductDetail()
})
</script>

<style scoped>
/* ===== Reset & Base ===== */
.detail-page {
  font-family: 'DM Sans', -apple-system, sans-serif;
  color: #212529;
  background: #ffffff;
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
}

/* ===== Navigation ===== */
.detail-nav {
  position: fixed; top: 0; left: 0; right: 0;
  height: 64px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid #dee2e6;
}
.back-btn {
  background: none; border: none;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.9rem; font-weight: 500; color: #2b6cb0;
  cursor: pointer; padding: 8px 0; transition: color 0.3s;
}
.back-btn:hover { color: #4a9eff; }
.nav-title { font-size: 0.95rem; font-weight: 600; color: #212529; }
.nav-placeholder { width: 60px; }

/* ===== Loading ===== */
.loading-container {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; min-height: 60vh; color: #868e96; gap: 16px;
}
.loading-spinner {
  width: 40px; height: 40px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Detail Content ===== */
.detail-content {
  max-width: 1100px; margin: 0 auto;
  padding: 96px 24px 60px;
  display: grid; grid-template-columns: 1fr 1fr; gap: 48px;
}

/* ===== Image Section ===== */
.image-section { position: sticky; top: 96px; align-self: start; }
.main-image {
  aspect-ratio: 1; border-radius: 20px; overflow: hidden;
  background: #f8f9fa; margin-bottom: 16px;
}
.main-image img {
  width: 100%; height: 100%; object-fit: cover;
  transition: opacity 0.3s;
}
.thumbnails {
  display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px;
}
.thumbnails img {
  width: 72px; height: 72px; border-radius: 12px; object-fit: cover;
  cursor: pointer; border: 2px solid transparent;
  transition: all 0.3s; flex-shrink: 0;
  background: #f8f9fa;
}
.thumbnails img:hover { border-color: #4a9eff; }
.thumbnails img.active { border-color: #2b6cb0; }

/* ===== Info Section ===== */
.info-section { padding-top: 8px; }
.product-header { margin-bottom: 24px; }
.product-name {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem; font-weight: 700; color: #212529;
  line-height: 1.2; margin-bottom: 16px;
}
.product-price {
  font-size: 2rem; font-weight: 700; color: #2b6cb0;
}

/* ===== Status Tags ===== */
.status-tags {
  display: flex; gap: 8px; margin-bottom: 24px;
}
.status-tag {
  padding: 4px 12px; border-radius: 50px;
  font-size: 0.75rem; font-weight: 600;
}
.status-tag.on-sale {
  background: #d3f9d8; color: #2b8a3e;
}
.status-tag.off-sale {
  background: #ffe3e3; color: #c92a2a;
}
.status-tag.category {
  background: #f1f3f5; color: #868e96;
}

/* ===== Description ===== */
.product-desc, .seller-info {
  margin-bottom: 24px;
}
.section-label {
  font-size: 0.85rem; font-weight: 600; color: #212529;
  margin-bottom: 8px; text-transform: uppercase;
  letter-spacing: 0.04em;
}
.product-desc p {
  font-size: 0.9rem; color: #495057; line-height: 1.7;
}
.seller-detail {
  display: flex; align-items: center; gap: 8px;
}
.seller-name {
  font-size: 0.9rem; font-weight: 500; color: #2b6cb0;
}

/* ===== Stats ===== */
.stats {
  display: flex; gap: 32px; margin-bottom: 32px;
  padding: 20px; background: #f8f9fa; border-radius: 16px;
}
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 0.75rem; color: #868e96; text-transform: uppercase; letter-spacing: 0.04em; }
.stat-value { font-size: 1.2rem; font-weight: 700; color: #212529; }

/* ===== Actions ===== */
.actions {
  display: flex; align-items: center; gap: 16px;
}
.qty-selector {
  display: flex; align-items: center; gap: 12px;
  border: 1px solid #dee2e6; border-radius: 12px; padding: 8px 12px;
}
.qty-selector button {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid #dee2e6; background: transparent;
  font-size: 1.1rem; cursor: pointer; display: flex;
  align-items: center; justify-content: center;
  transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.qty-selector button:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.qty-selector button:disabled { opacity: 0.3; cursor: not-allowed; }
.qty-selector span {
  font-size: 1rem; font-weight: 600; min-width: 30px; text-align: center;
}
.add-cart-btn {
  flex: 1; padding: 14px 32px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif;
  font-size: 0.9rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.add-cart-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(43,108,176,0.30);
}

/* ===== Not Found ===== */
.not-found {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; min-height: 60vh; gap: 16px;
  color: #868e96;
}
.back-home-btn {
  padding: 12px 24px; border: none; border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.back-home-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(43,108,176,0.30); }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .detail-content {
    grid-template-columns: 1fr; gap: 24px;
    padding: 80px 16px 40px;
  }
  .image-section { position: static; }
  .product-name { font-size: 1.4rem; }
  .product-price { font-size: 1.5rem; }
  .actions { flex-direction: column; }
  .qty-selector { width: 100%; justify-content: center; }
}
</style>