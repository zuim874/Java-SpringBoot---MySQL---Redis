<template>
  <div class="seller-page">
    <!-- 顶部导航 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-actions">
        <button class="nav-btn" @click="goHome">
          <span class="nav-btn-icon">🏪</span>
          <span class="nav-btn-text">返回商城</span>
        </button>
        <button class="nav-btn nav-btn--profile" @click="goSellerDashboard">商家工作台</button>
        <button class="nav-btn nav-btn--logout" @click="handleLogout">
          <span class="nav-btn-icon">🚪</span>
          <span class="nav-btn-text">退出登录</span>
        </button>
      </div>
    </nav>

    <!-- 主内容 -->
    <main class="seller-body">
      <!-- 页头 -->
      <header class="page-head">
        <div>
          <h1 class="page-title">商家商品管理</h1>
          <p class="page-sub">管理您的商品，包括新增、编辑、上架/下架操作</p>
        </div>
        <button class="btn btn--primary" @click="openAddDialog">
          <span class="btn-icon">＋</span>新增商品
        </button>
      </header>

      <!-- 加载中 -->
      <div class="loading-state" v-if="loading">
        <div class="loading-spinner"></div>
        <p>加载商品数据...</p>
      </div>

      <!-- 错误状态 -->
      <div class="empty-state" v-else-if="error">
        <div class="state-icon">⚠️</div>
        <p>商品加载失败</p>
        <button class="retry-btn" @click="fetchProducts">重新加载</button>
      </div>

      <!-- 空数据 -->
      <div class="empty-state" v-else-if="products.length === 0">
        <div class="state-icon">📦</div>
        <p>暂无商品</p>
        <p class="empty-hint">点击右上角「新增商品」创建您的第一个商品</p>
      </div>

      <!-- 商品表格 -->
      <section v-else class="panel">
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>商品名</th>
                <th>价格</th>
                <th>库存</th>
                <th>销量</th>
                <th>分类</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prod in products" :key="prod.id">
                <td class="cell-dim">{{ prod.id }}</td>
                <td class="name-cell">{{ prod.productName }}</td>
                <td class="price">¥{{ (prod.price || 0).toFixed(2) }}</td>
                <td>{{ prod.stock || 0 }}</td>
                <td>{{ prod.sold || 0 }}</td>
                <td>
                  <template v-if="getProductCategoryNames(prod.category).length">
                    <span class="cat-chip" v-for="(name, i) in getProductCategoryNames(prod.category)" :key="i">{{ name }}</span>
                  </template>
                  <span v-else class="cat-chip cat-chip--empty">-</span>
                </td>
                <td>
                  <span class="status-badge" :class="prod.status === 1 ? 'status--ok' : 'status--off'">
                    {{ prod.status === 1 ? '上架' : '下架' }}
                  </span>
                </td>
                <td class="action-cell">
                  <button class="action-btn" @click="openEditDialog(prod)">编辑</button>
                  <button class="action-btn action-btn--toggle" @click="toggleStatus(prod)"
                          :disabled="actionLoading">
                    {{ prod.status === 1 ? '下架' : '上架' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">›</button>
        </div>
      </section>

      <!-- 消息提示 -->
      <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
    </main>

    <!-- ===== 新增/编辑商品弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
        <div class="modal-card">
          <div class="modal-head">
            <span class="modal-icon">{{ editingProduct ? '✏️' : '📦' }}</span>
            <h3 class="modal-title">{{ editingProduct ? '编辑商品' : '新增商品' }}</h3>
          </div>
          <div class="modal-form">
            <div class="modal-field-row">
              <label>商品名称</label>
              <div class="modal-field">
                <input v-model="form.productName" type="text" placeholder="请输入商品名称" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>价格 (¥)</label>
              <div class="modal-field">
                <input v-model="form.price" type="number" step="0.01" placeholder="请输入价格" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>库存</label>
              <div class="modal-field">
                <input v-model="form.stock" type="number" placeholder="请输入库存数量" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>分类（可多选，分类由平台管理员维护）</label>
              <div class="modal-field">
                <div class="category-tag-input">
                  <span v-for="(cat, ci) in selectedCategories" :key="ci" class="cat-tag">
                    {{ categoryNameById[cat] || ('#' + cat) }}
                    <button type="button" class="cat-tag-del" @click="removeCategory(ci)">×</button>
                  </span>
                  <span v-if="selectedCategories.length === 0" class="category-empty-hint">请从下方选择分类</span>
                </div>
              </div>
              <div class="category-presets">
                <span v-for="cat in presetCategories" :key="cat.id"
                      class="cat-preset-chip"
                      :class="{ active: selectedCategories.includes(cat.id) }"
                      @click="toggleCategory(cat)">{{ cat.name }}</span>
              </div>
            </div>
            <div class="modal-field-row">
              <label>商品描述</label>
              <div class="modal-field">
                <textarea v-model="form.description" placeholder="请输入商品描述" class="modal-textarea"></textarea>
              </div>
            </div>
            <!-- ===== 图片上传区域 ===== -->
            <div class="modal-field-row">
              <label>商品主图</label>
              <div class="image-upload-row">
                <div class="upload-box upload-box--main" @click="triggerMainUpload">
                  <input ref="mainImageInput" type="file" accept="image/*" hidden @change="onMainImageChange" />
                  <span v-if="mainImagePreview" class="upload-preview-wrap">
                    <img :src="mainImagePreview" alt="主图预览" class="upload-preview" />
                    <button type="button" class="upload-preview-del" @click.stop="clearMainImage">×</button>
                  </span>
                  <span v-else-if="form.mainImageUrl" class="upload-preview-wrap">
                    <img :src="form.mainImageUrl" alt="当前主图" class="upload-preview" />
                  </span>
                  <span v-else class="upload-placeholder">
                    <span class="upload-icon">📷</span>
                    <span class="upload-text">点击上传主图</span>
                  </span>
                </div>
                <div class="main-image-info">
                  <p class="image-info-text">将作为首页展示的商品主图</p>
                  <p class="image-info-text" v-if="form.mainImageUrl">已上传：{{ form.mainImageUrl }}</p>
                </div>
              </div>
            </div>
            <div class="modal-field-row">
              <label>商品细节图（可多张）</label>
              <div class="detail-image-grid">
                <div v-for="(img, di) in detailImages" :key="di" class="detail-image-item">
                  <img :src="img.preview || img.url" alt="细节图" class="detail-image-preview" />
                  <button type="button" class="upload-preview-del" @click="removeDetailImage(di)">×</button>
                </div>
                <div class="upload-box upload-box--detail" @click="triggerDetailUpload">
                  <input ref="detailImageInput" type="file" accept="image/*" multiple hidden @change="onDetailImageChange" />
                  <span class="upload-placeholder">
                    <span class="upload-icon">🖼️</span>
                    <span class="upload-text">添加图片</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showDialog = false">取消</button>
            <button class="modal-btn modal-btn--primary" @click="submitForm" :disabled="formLoading">
              {{ editingProduct ? '保存' : '新增' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 底部 -->
    <footer class="page-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, getSellerProducts, addSellerProduct, updateSellerProduct, onshelfProduct, offshelfProduct, getCategoryList, getProductDetail, uploadSellerProductImage, deleteSellerProductImage } from '../api/index.js'

const router = useRouter()

const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'

const nickname = ref(localStorage.getItem('nickname') || '')
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

function goProfile() {
  router.push('/profile')
}

// ===== 商品列表状态 =====
const products = ref([])
const loading = ref(true)
const error = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 10
const actionLoading = ref(false)

// ===== 弹窗状态 =====
const showDialog = ref(false)
const editingProduct = ref(null)
const formLoading = ref(false)
const form = ref({
  productName: '',
  price: '',
  stock: '',
  category: '',
  description: '',
  mainImageUrl: ''
})

// 预置分类（管理员维护，拉取后展示为可多选的分类卡片）
const presetCategories = ref([])
// 分类ID → 名称 映射（用于回显/展示）
const categoryNameById = ref({})

// 已选分类ID列表（以英文逗号分隔存储到 form.category）
const selectedCategories = ref([])

// 图片上传状态
const mainImageInput = ref(null)
const detailImageInput = ref(null)
const mainImagePreview = ref('')      // 主图本地预览 URL
const mainImageFile = ref(null)       // 主图待上传文件
const detailImages = ref([])          // 细节图列表（含已上传的 url 和待上传的 file）
const detailImageFiles = ref([])      // 细节图待上传文件列表
const oldMainImageId = ref(null)      // 编辑时已存在的主图记录ID（换主图时删除）

/**
 * 加载分类预设（管理员维护的启用分类，卖家只能从中选取，不可自定义）
 */
async function loadCategories() {
  try {
    const res = await getCategoryList()
    if (res && res.code === 200 && Array.isArray(res.data)) {
      presetCategories.value = res.data.filter(c => c && c.id != null)
      const map = {}
      presetCategories.value.forEach(c => { map[c.id] = c.name })
      categoryNameById.value = map
    }
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

/**
 * 商品分类ID集合 → 名称列表（表格展示用）
 * @param {string} categoryIds - 逗号分隔的分类ID（如 "1,2"）
 * @returns {string[]} 分类名称列表
 */
function getProductCategoryNames(categoryIds) {
  if (!categoryIds) return []
  const names = []
  String(categoryIds).split(',').forEach(part => {
    const id = Number(part.trim())
    if (id && categoryNameById.value[id]) names.push(categoryNameById.value[id])
  })
  return names
}

// ===== 消息提示 =====
const message = ref('')
const msgSuccess = ref(false)

/**
 * 获取商家商品列表
 */
async function fetchProducts() {
  loading.value = true
  error.value = false
  try {
    const res = await getSellerProducts(currentPage.value, pageSize)
    if (res && res.code === 200 && res.data) {
      products.value = res.data.records || []
      totalPages.value = res.data.pages || 1
    } else {
      products.value = []
      totalPages.value = 1
    }
  } catch (e) {
    console.error('获取商家商品列表失败:', e)
    error.value = true
    products.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 分页跳转
 */
function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchProducts()
}

/**
 * 打开新增商品弹窗
 */
function openAddDialog() {
  editingProduct.value = null
  form.value = { productName: '', price: '', stock: '', category: '', description: '', mainImageUrl: '' }
  selectedCategories.value = []
  mainImagePreview.value = ''
  mainImageFile.value = null
  detailImages.value = []
  detailImageFiles.value = []
  oldMainImageId.value = null
  showDialog.value = true
}

/**
 * 打开编辑商品弹窗（回显图片）
 */
async function openEditDialog(prod) {
  editingProduct.value = prod
  form.value = {
    productName: prod.productName || '',
    price: prod.price || '',
    stock: prod.stock || '',
    category: prod.category || '',
    description: prod.description || '',
    mainImageUrl: prod.mainImageUrl || ''
  }
  // 分类ID集合解析为已选分类ID数组（商品的 category 存分类ID）
  selectedCategories.value = String(prod.category || '').split(',').map(s => Number(s.trim())).filter(n => n > 0)
  mainImagePreview.value = ''
  mainImageFile.value = null
  detailImages.value = []
  detailImageFiles.value = []
  oldMainImageId.value = null
  showDialog.value = true

  // 加载已有图片回显
  try {
    const res = await getProductDetail(prod.id)
    if (res && res.code === 200 && res.data && Array.isArray(res.data.images)) {
      res.data.images.forEach(img => {
        if (img.isMain === 1) {
          form.value.mainImageUrl = img.imageUrl
          oldMainImageId.value = img.id
        } else {
          detailImages.value.push({ url: img.imageUrl, id: img.id })
        }
      })
    }
  } catch (e) {
    console.error('加载商品图片失败:', e)
  }
}

/**
 * 提交表单（新增或编辑）
 * 1.保存商品基本信息（分类为逗号分隔的多分类串）
 * 2.更换主图时先删除旧主图记录
 * 3.上传新主图（isMain=1）
 * 4.上传新增细节图（isMain=0）
 */
async function submitForm() {
  // 表单验证
  if (!form.value.productName.trim()) {
    showMessage('请输入商品名称', false)
    return
  }
  if (!form.value.price || Number(form.value.price) <= 0) {
    showMessage('请输入有效的价格', false)
    return
  }
  if (selectedCategories.value.length === 0) {
    showMessage('请至少选择一个分类', false)
    return
  }

  formLoading.value = true
  try {
    const categoryStr = selectedCategories.value.join(',')
    let productId = editingProduct.value?.id
    let res

    if (editingProduct.value) {
      // 编辑商品
      res = await updateSellerProduct(editingProduct.value.id, {
        productName: form.value.productName,
        price: Number(form.value.price),
        stock: Number(form.value.stock) || 0,
        category: categoryStr,
        description: form.value.description
      })
    } else {
      // 新增商品（后端返回新商品ID）
      res = await addSellerProduct({
        productName: form.value.productName,
        price: Number(form.value.price),
        stock: Number(form.value.stock) || 0,
        category: categoryStr,
        description: form.value.description
      })
      if (res && res.code === 200 && res.data && res.data.id) {
        productId = res.data.id
      }
    }

    if (!res || res.code !== 200) {
      showMessage(res?.mes || '操作失败', false)
      return
    }

    // 编辑场景：更换了主图，先删除旧主图记录（避免重复主图）
    if (mainImageFile.value && oldMainImageId.value) {
      try {
        await deleteSellerProductImage(oldMainImageId.value, productId)
        oldMainImageId.value = null
      } catch (e) {
        console.error('删除旧主图失败:', e)
      }
    }

    // 上传主图
    if (mainImageFile.value) {
      const imgRes = await uploadSellerProductImage(productId, mainImageFile.value, 1, 0)
      if (!imgRes || imgRes.code !== 200) {
        showMessage('主图上传失败，商品信息已保存', false)
        showDialog.value = false
        fetchProducts()
        return
      }
    }

    // 上传新增细节图（按顺序排序）
    for (let i = 0; i < detailImageFiles.value.length; i++) {
      const imgRes = await uploadSellerProductImage(productId, detailImageFiles.value[i], 0, i)
      if (!imgRes || imgRes.code !== 200) {
        showMessage('细节图上传失败，请稍后重试', false)
        break
      }
    }

    showMessage(editingProduct.value ? '商品已更新' : '商品已新增', true)
    showDialog.value = false
    fetchProducts()
  } catch (e) {
    showMessage('网络错误，请检查后端服务', false)
  } finally {
    formLoading.value = false
  }
}

// ===== 分类操作（仅可从预设分类中多选，不可自定义输入） =====

/**
 * 切换预设分类（选中/取消，按分类ID）
 * @param {object} cat - 分类对象 { id, name }
 */
function toggleCategory(cat) {
  const index = selectedCategories.value.indexOf(cat.id)
  if (index === -1) {
    selectedCategories.value.push(cat.id)
  } else {
    selectedCategories.value.splice(index, 1)
  }
}

/**
 * 删除已选分类
 */
function removeCategory(index) {
  selectedCategories.value.splice(index, 1)
}

// ===== 主图上传 =====

/**
 * 触发主图文件选择
 */
function triggerMainUpload() {
  mainImageInput.value?.click()
}

/**
 * 主图选择回调（生成本地预览）
 */
function onMainImageChange(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showMessage('请选择图片文件', false)
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    showMessage('图片大小不能超过 10MB', false)
    return
  }
  // 释放旧预览，绑定新文件
  if (mainImagePreview.value) URL.revokeObjectURL(mainImagePreview.value)
  mainImageFile.value = file
  mainImagePreview.value = URL.createObjectURL(file)
  e.target.value = ''
}

/**
 * 清除主图选择
 */
function clearMainImage() {
  if (mainImagePreview.value) URL.revokeObjectURL(mainImagePreview.value)
  mainImageFile.value = null
  mainImagePreview.value = ''
}

// ===== 细节图上传 =====

/**
 * 触发细节图文件选择（可多选）
 */
function triggerDetailUpload() {
  detailImageInput.value?.click()
}

/**
 * 细节图选择回调（生成本地预览）
 */
function onDetailImageChange(e) {
  const files = Array.from(e.target.files || [])
  files.forEach(file => {
    if (!file.type.startsWith('image/')) {
      showMessage('请选择图片文件', false)
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      showMessage('图片大小不能超过 10MB', false)
      return
    }
    detailImageFiles.value.push(file)
    detailImages.value.push({ file, preview: URL.createObjectURL(file) })
  })
  e.target.value = ''
}

/**
 * 删除细节图
 * 1.本地待上传：直接移除
 * 2.已上传：调用后端接口删除
 */
async function removeDetailImage(index) {
  const img = detailImages.value[index]
  if (img.preview) URL.revokeObjectURL(img.preview)
  detailImages.value.splice(index, 1)
  if (img.file) {
    // 本地待上传文件，仅从列表移除
    const fi = detailImageFiles.value.indexOf(img.file)
    if (fi !== -1) detailImageFiles.value.splice(fi, 1)
  } else if (img.id && editingProduct.value) {
    // 已上传图片，调用接口删除
    try {
      await deleteSellerProductImage(img.id, editingProduct.value.id)
    } catch (e) {
      console.error('删除细节图失败:', e)
    }
  }
}

/**
 * 上架/下架商品
 */
async function toggleStatus(prod) {
  actionLoading.value = true
  try {
    const res = prod.status === 1
      ? await offshelfProduct(prod.id)
      : await onshelfProduct(prod.id)

    if (res && res.code === 200) {
      showMessage(prod.status === 1 ? '已下架' : '已上架', true)
      fetchProducts()
    } else {
      showMessage(res?.mes || '操作失败', false)
    }
  } catch {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

function showMessage(msg, success) {
  message.value = msg
  msgSuccess.value = success
  setTimeout(() => { message.value = '' }, 3000)
}

function goHome() { router.push('/home') }
function goSellerDashboard() { router.push('/seller/dashboard') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  document.addEventListener('click', closePopups)
  fetchProducts()
  fetchUserProfile()
  loadCategories()
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})
</script>

<style scoped>
/* ===== 页面骨架 ===== */
.seller-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  font-family: var(--font-sans);
  color: var(--text);
}

/* ===== 顶部导航 ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0; height: 68px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 clamp(20px, 4vw, 48px);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border-bottom: 1px solid var(--border);
}
.nav-logo {
  font-family: var(--font-display); font-size: 1.35rem; font-weight: 700; letter-spacing: -0.02em;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.nav-actions { display: flex; align-items: center; gap: 10px; }
.nav-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: var(--radius-pill);
  border: 1px solid var(--border-strong);
  background: var(--surface); color: var(--text-2);
  font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
}
.nav-btn:hover { border-color: var(--primary); color: var(--primary); box-shadow: var(--shadow-sm); }
.nav-btn--profile:hover { border-color: var(--primary); color: var(--primary); }
.nav-btn--logout:hover { border-color: var(--danger); color: var(--danger); background: var(--danger-soft); }
.nav-btn-icon { font-size: 0.95rem; }

/* ===== 主体布局 ===== */
.seller-body {
  flex: 1; width: 100%; max-width: var(--container); margin: 0 auto;
  padding: 96px 24px 56px;
}
.page-head {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 16px;
  flex-wrap: wrap; margin-bottom: 24px;
}
.page-title {
  font-family: var(--font-display); font-size: 1.7rem; font-weight: 700;
  color: var(--text); letter-spacing: -0.01em; line-height: 1.2;
}
.page-sub { margin-top: 6px; font-size: 0.9rem; color: var(--text-3); }

/* ===== 按钮 ===== */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 20px; border: none; border-radius: var(--radius);
  font-size: 0.85rem; font-weight: 600; cursor: pointer; font-family: var(--font-sans);
  transition: all 0.2s ease;
}
.btn--primary { background: linear-gradient(135deg, var(--primary), var(--primary-2)); color: #fff; }
.btn--primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-icon { font-size: 0.9rem; }

/* ===== 内容面板 ===== */
.panel {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
  padding: 24px; margin-bottom: 20px;
  animation: panelIn 0.35s ease both;
}
@keyframes panelIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }

/* ===== 表格 ===== */
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius); }
.data-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; min-width: 820px; }
.data-table thead th {
  text-align: left; padding: 12px 16px; background: var(--surface-3);
  color: var(--text-2); font-weight: 600; font-size: 0.74rem;
  letter-spacing: 0.04em; white-space: nowrap; border-bottom: 1px solid var(--border);
}
.data-table tbody td {
  padding: 13px 16px; color: var(--text); border-bottom: 1px solid var(--border);
  vertical-align: middle;
}
.data-table tbody tr:nth-child(even) td { background: var(--surface-2); }
.data-table tbody tr:hover td { background: var(--primary-softer); }
.data-table tbody tr:last-child td { border-bottom: none; }
.name-cell { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.cell-dim { color: var(--text-3); font-size: 0.8rem; }
.cat-chip {
  display: inline-block; padding: 2px 10px; border-radius: var(--radius-pill);
  background: var(--surface-3); color: var(--text-2); font-size: 0.72rem; font-weight: 600;
}
.cat-chip + .cat-chip { margin-left: 4px; }
.cat-chip--empty { color: var(--text-4); }
.price { color: var(--price); font-weight: 700; white-space: nowrap; }
.action-cell { display: flex; gap: 6px; flex-wrap: wrap; }

/* ===== 状态标签 ===== */
.status-badge {
  display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px;
  border-radius: var(--radius-pill); font-size: 0.72rem; font-weight: 600; white-space: nowrap;
}
.status--ok { background: var(--success-soft); color: var(--success); }
.status--off { background: var(--danger-soft); color: var(--danger); }

/* ===== 操作按钮 ===== */
.action-btn {
  padding: 5px 12px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-strong); background: var(--surface);
  color: var(--text-2); font-size: 0.75rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease; white-space: nowrap;
}
.action-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); background: var(--primary-softer); }
.action-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.action-btn--toggle:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); background: var(--primary-softer); }

/* ===== 分页 ===== */
.pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 20px; }
.page-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); border: 1px solid var(--border-strong);
  background: var(--surface); color: var(--text-2); font-size: 0.9rem; cursor: pointer;
  transition: all 0.2s ease;
}
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: var(--text-3); }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 48px 16px; color: var(--text-3); font-size: 0.88rem; }
.loading-spinner {
  width: 34px; height: 34px; margin: 0 auto 12px;
  border: 3px solid var(--border); border-top-color: var(--primary);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.state-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-hint { font-size: 0.82rem; color: var(--text-4); margin-top: 8px; }
.retry-btn {
  margin-top: 16px; padding: 9px 22px; border: 1px solid var(--primary); border-radius: var(--radius-pill);
  background: var(--surface); color: var(--primary); font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
}
.retry-btn:hover { background: var(--primary-soft); }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(23, 35, 61, 0.45);
  backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center;
  z-index: 200; padding: 20px; animation: overlayIn 0.2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card {
  width: 560px; max-width: 100%; padding: 26px;
  /* 内容过长时卡片内部滚动，避免按钮被挤出视口 */
  max-height: calc(100vh - 40px); overflow-y: auto;
  background: var(--surface); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg); animation: modalIn 0.3s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(14px) scale(0.98); } to { opacity: 1; transform: none; } }
.modal-head {
  display: flex; align-items: center; gap: 12px; margin-bottom: 20px;
  padding-bottom: 14px; border-bottom: 1px solid var(--border);
}
.modal-icon {
  width: 42px; height: 42px; border-radius: var(--radius); background: var(--primary-soft);
  display: flex; align-items: center; justify-content: center; font-size: 1.3rem; flex-shrink: 0;
}
.modal-title {
  font-family: var(--font-display); font-size: 1.2rem; font-weight: 700; color: var(--text); margin: 0;
}
.modal-form { margin-bottom: 20px; }
.modal-field-row { margin-bottom: 14px; }
.modal-field-row label {
  display: block; font-size: 0.78rem; font-weight: 600; color: var(--text-2); margin-bottom: 6px;
}
.modal-field {
  display: flex; align-items: center; padding: 0 14px;
  border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface-2); transition: all 0.2s ease;
}
.modal-field:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); background: var(--surface); }
.modal-field input, .modal-textarea {
  flex: 1; border: none; background: transparent; padding: 11px 0;
  font-size: 0.88rem; color: var(--text); outline: none;
}
.modal-textarea { min-height: 80px; resize: vertical; line-height: 1.5; }
.modal-actions { display: flex; gap: 12px; }
.modal-btn {
  flex: 1; padding: 12px; border: none; border-radius: var(--radius-sm);
  font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s ease;
}
.modal-btn--cancel { background: var(--surface-3); color: var(--text-2); }
.modal-btn--cancel:hover { background: var(--bg-deep); }
.modal-btn--primary { background: linear-gradient(135deg, var(--primary), var(--primary-2)); color: #fff; }
.modal-btn--primary:hover:not(:disabled) { box-shadow: var(--shadow-primary); transform: translateY(-1px); }
.modal-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ===== 分类多选 / 自定义输入 ===== */
.category-tag-input {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  width: 100%; padding: 8px 10px;
  border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface-2); transition: all 0.2s ease;
}
.category-tag-input:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); background: var(--surface); }
.category-tag-input input {
  flex: 1; min-width: 160px; border: none; background: transparent;
  padding: 4px 0; font-size: 0.88rem; color: var(--text); outline: none;
}
.cat-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 8px; border-radius: var(--radius-pill);
  background: var(--primary-soft); color: var(--primary);
  font-size: 0.74rem; font-weight: 600;
}
.cat-tag-del {
  display: inline-flex; align-items: center; justify-content: center;
  width: 14px; height: 14px; border: none; border-radius: 50%;
  background: transparent; color: var(--primary); font-size: 0.85rem;
  line-height: 1; cursor: pointer; transition: all 0.2s ease;
}
.cat-tag-del:hover { background: var(--danger-soft); color: var(--danger); }
.category-empty-hint { font-size: 0.78rem; color: var(--text-4); }
.category-presets { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.cat-preset-chip {
  padding: 4px 12px; border-radius: var(--radius-pill);
  border: 1px solid var(--border-strong); background: var(--surface);
  color: var(--text-2); font-size: 0.75rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease; user-select: none;
}
.cat-preset-chip:hover { border-color: var(--primary); color: var(--primary); }
.cat-preset-chip.active { background: var(--primary); border-color: var(--primary); color: #fff; }

/* ===== 图片上传 ===== */
.image-upload-row { display: flex; align-items: flex-start; gap: 14px; }
.upload-box {
  position: relative; display: flex; align-items: center; justify-content: center;
  border: 1.5px dashed var(--border-strong); border-radius: var(--radius);
  background: var(--surface-2); cursor: pointer; overflow: hidden;
  transition: all 0.2s ease;
}
.upload-box:hover { border-color: var(--primary); background: var(--primary-softer); }
.upload-box--main { width: 120px; height: 120px; flex-shrink: 0; }
.upload-box--detail { width: 88px; height: 88px; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--text-3); }
.upload-icon { font-size: 1.6rem; }
.upload-text { font-size: 0.72rem; font-weight: 600; }
.upload-preview-wrap { position: relative; width: 100%; height: 100%; }
.upload-preview { width: 100%; height: 100%; object-fit: cover; }
.upload-preview-del {
  position: absolute; top: 6px; right: 6px;
  display: flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; border: none; border-radius: 50%;
  background: rgba(23, 35, 61, 0.55); color: #fff; font-size: 0.9rem;
  line-height: 1; cursor: pointer; transition: all 0.2s ease;
}
.upload-preview-del:hover { background: var(--danger); }
.main-image-info { display: flex; flex-direction: column; gap: 4px; padding-top: 4px; }
.image-info-text { font-size: 0.76rem; color: var(--text-3); line-height: 1.4; word-break: break-all; }
.detail-image-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.detail-image-item { position: relative; width: 88px; height: 88px; }
.detail-image-preview { width: 100%; height: 100%; object-fit: cover; border-radius: var(--radius-sm); }

/* ===== 消息提示 ===== */
.msg {
  margin-top: 18px; text-align: center; font-size: 0.85rem; padding: 12px;
  border-radius: var(--radius-sm); animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47, 158, 68, 0.18); }
.msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224, 49, 49, 0.18); }

/* ===== 底部 ===== */
.page-footer { padding: 8px 0 28px; text-align: center; }
.page-footer span { font-size: 0.72rem; color: var(--text-4); }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .seller-body { padding: 84px 16px 40px; }
  .nav-btn-text { display: none; }
  .page-head { flex-direction: column; align-items: flex-start; }
}
</style>