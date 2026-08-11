<template>
  <div class="seller-page">
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
        <button class="nav-btn" @click="goHome">返回商城</button>
        <button class="nav-btn nav-btn--logout" @click="handleLogout">退出</button>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="seller-wrapper">
      <div class="seller-card">
        <div class="seller-header">
          <div class="seller-icon">🏪</div>
          <h2 class="seller-title">商家商品管理</h2>
          <p class="seller-desc">管理您的商品，包括新增、编辑、上架/下架操作</p>
        </div>

        <!-- 新增商品按钮 -->
        <div class="toolbar">
          <button class="add-btn" @click="openAddDialog">+ 新增商品</button>
        </div>

        <!-- 加载中 -->
        <div class="loading-state" v-if="loading">
          <div class="loading-spinner"></div>
          <p>加载商品数据...</p>
        </div>

        <!-- 错误状态 -->
        <div class="empty-state" v-else-if="error">
          <div class="error-icon">⚠️</div>
          <p>商品加载失败</p>
          <button class="retry-btn" @click="fetchProducts">重新加载</button>
        </div>

        <!-- 空数据 -->
        <div class="empty-state" v-else-if="products.length === 0">
          <div class="empty-icon">📦</div>
          <p>暂无商品</p>
          <p class="empty-hint">点击上方按钮新增您的第一个商品</p>
        </div>

        <!-- 商品表格 -->
        <div class="table-wrap" v-else>
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
                <td>{{ prod.id }}</td>
                <td class="name-cell">{{ prod.productName }}</td>
                <td>¥{{ (prod.price || 0).toFixed(2) }}</td>
                <td>{{ prod.stock || 0 }}</td>
                <td>{{ prod.sold || 0 }}</td>
                <td>{{ prod.category || '-' }}</td>
                <td>
                  <span :class="['status-badge', prod.status === 1 ? 'status--active' : 'status--disabled']">
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

        <!-- 消息提示 -->
        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
      </div>
    </div>

    <!-- ===== 新增/编辑商品弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
        <div class="modal-card">
          <div class="modal-icon">{{ editingProduct ? '✏️' : '📦' }}</div>
          <h3 class="modal-title">{{ editingProduct ? '编辑商品' : '新增商品' }}</h3>

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
              <label>分类</label>
              <div class="modal-field">
                <input v-model="form.category" type="text" placeholder="请输入分类名称" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>商品描述</label>
              <div class="modal-field">
                <textarea v-model="form.description" placeholder="请输入商品描述" class="modal-textarea"></textarea>
              </div>
            </div>
          </div>

          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showDialog = false">取消</button>
            <button class="modal-btn modal-btn--blue" @click="submitForm" :disabled="formLoading">
              {{ editingProduct ? '保存' : '新增' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 底部 -->
    <div class="seller-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSellerProducts, addSellerProduct, updateSellerProduct, onshelfProduct, offshelfProduct } from '../api/index.js'

const router = useRouter()

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
  description: ''
})

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
  form.value = { productName: '', price: '', stock: '', category: '', description: '' }
  showDialog.value = true
}

/**
 * 打开编辑商品弹窗
 */
function openEditDialog(prod) {
  editingProduct.value = prod
  form.value = {
    productName: prod.productName || '',
    price: prod.price || '',
    stock: prod.stock || '',
    category: prod.category || '',
    description: prod.description || ''
  }
  showDialog.value = true
}

/**
 * 提交表单（新增或编辑）
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

  formLoading.value = true
  try {
    let res
    if (editingProduct.value) {
      // 编辑商品
      res = await updateSellerProduct(editingProduct.value.id, {
        productName: form.value.productName,
        price: Number(form.value.price),
        stock: Number(form.value.stock) || 0,
        category: form.value.category,
        description: form.value.description
      })
    } else {
      // 新增商品
      res = await addSellerProduct({
        productName: form.value.productName,
        price: Number(form.value.price),
        stock: Number(form.value.stock) || 0,
        category: form.value.category,
        description: form.value.description
      })
    }

    if (res && res.code === 200) {
      showMessage(editingProduct.value ? '商品已更新' : '商品已新增', true)
      showDialog.value = false
      fetchProducts()
    } else {
      showMessage(res?.mes || '操作失败', false)
    }
  } catch (e) {
    showMessage('网络错误，请检查后端服务', false)
  } finally {
    formLoading.value = false
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
    showMessage('网络错误', false)
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
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
/* ===== 全局 ===== */
.seller-page {
  min-height: 100vh; display: flex; flex-direction: column; align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif; position: relative; overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-circle {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35;
}
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
  padding: 8px 20px; border: 1px solid rgba(43,108,176,0.20); border-radius: 10px;
  background: rgba(255,255,255,0.60); font-family: 'DM Sans', sans-serif;
  font-size: 0.8rem; font-weight: 600; color: #2b6cb0; cursor: pointer; transition: all 0.3s;
}
.nav-btn:hover { background: #2b6cb0; color: white; border-color: #2b6cb0; }
.nav-btn--logout { color: #c92a2a; border-color: rgba(201,42,42,0.20); }
.nav-btn--logout:hover { background: #c92a2a; border-color: #c92a2a; color: white; }

/* ===== 主卡片 ===== */
.seller-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.seller-card {
  width: 1100px; max-width: 100%; padding: 48px 40px;
  background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.seller-header { text-align: center; margin-bottom: 32px; }
.seller-icon { font-size: 2rem; margin-bottom: 12px; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.1); opacity: 1; } }
.seller-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700; color: #212529; margin-bottom: 8px; }
.seller-desc { font-size: 0.9rem; color: #868e96; }

/* ===== 工具栏 ===== */
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 20px; }
.add-btn {
  padding: 12px 28px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.add-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(43,108,176,0.30); }

/* ===== 表格 ===== */
.table-wrap { overflow-x: auto; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); }
.data-table {
  width: 100%; border-collapse: collapse; font-size: 0.85rem; min-width: 700px;
}
.data-table th {
  text-align: left; padding: 12px 16px;
  background: rgba(43,108,176,0.04); color: #495057;
  font-weight: 600; font-size: 0.75rem; text-transform: uppercase;
  letter-spacing: 0.04em; border-bottom: 1px solid rgba(0,0,0,0.06);
  white-space: nowrap;
}
.data-table td {
  padding: 12px 16px; color: #212529; border-bottom: 1px solid rgba(0,0,0,0.04);
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: rgba(43,108,176,0.02); }
.name-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-cell { display: flex; gap: 6px; flex-wrap: wrap; }

/* ===== 状态标签 ===== */
.status-badge {
  display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 600;
}
.status--active { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.status--disabled { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 操作按钮 ===== */
.action-btn {
  padding: 5px 12px; border: 1px solid #dee2e6; border-radius: 6px;
  background: transparent; color: #495057; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
  white-space: nowrap;
}
.action-btn:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.action-btn--toggle { color: #2b6cb0; }
.action-btn--toggle:hover:not(:disabled) { border-color: #2b6cb0; }

/* ===== 分页 ===== */
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 24px;
}
.page-btn {
  padding: 8px 16px; border: 1px solid #dee2e6; border-radius: 8px;
  background: transparent; color: #495057; font-size: 0.85rem; cursor: pointer;
  transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.page-btn:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: #868e96; }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 60px 0; color: #868e96; }
.loading-spinner {
  width: 36px; height: 36px; margin: 0 auto 12px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-icon, .empty-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-hint { font-size: 0.85rem; color: #adb5bd; margin-top: 8px; }
.retry-btn {
  margin-top: 16px; padding: 10px 24px; border: 1px solid #2b6cb0; border-radius: 50px;
  background: transparent; color: #2b6cb0; font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.3s;
}
.retry-btn:hover { background: rgba(43,108,176,0.06); }

/* ===== 消息提示 ===== */
.msg { margin-top: 16px; text-align: center; font-size: 0.85rem; padding: 10px; border-radius: 10px; animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: #2b8a3e; background: rgba(43,138,62,0.06); border: 1px solid rgba(43,138,62,0.12); }
.msg--error { color: #c92a2a; background: rgba(201,42,42,0.06); border: 1px solid rgba(201,42,42,0.12); }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 200;
  animation: overlayIn 0.2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card {
  width: 460px; max-width: 90%; padding: 36px 32px;
  background: rgba(255,255,255,0.90); backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 20px; border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 20px 60px rgba(0,0,0,0.12); text-align: center;
  animation: modalIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
@keyframes modalIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.modal-icon { font-size: 2.4rem; margin-bottom: 12px; }
.modal-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.3rem; font-weight: 700; color: #212529; margin-bottom: 20px; }
.modal-form { text-align: left; margin-bottom: 20px; }
.modal-field-row { margin-bottom: 14px; }
.modal-field-row label {
  display: block; font-size: 0.78rem; font-weight: 600; color: #495057; margin-bottom: 6px;
}
.modal-field {
  display: flex; align-items: center; gap: 10px;
  padding: 0 14px; border: 1px solid #dee2e6; border-radius: 10px;
  background: rgba(255,255,255,0.60); transition: all 0.3s;
}
.modal-field:focus-within { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.12); }
.modal-field input, .modal-textarea {
  flex: 1; border: none; background: transparent; padding: 12px 0;
  font-size: 0.88rem; font-family: 'DM Sans', sans-serif; color: #212529; outline: none;
}
.modal-textarea {
  min-height: 80px; resize: vertical; padding: 12px 0; line-height: 1.5;
}
.modal-actions { display: flex; gap: 12px; }
.modal-btn {
  flex: 1; padding: 12px; border: none; border-radius: 10px;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.modal-btn--cancel { background: #f1f3f5; color: #495057; }
.modal-btn--cancel:hover { background: #e9ecef; }
.modal-btn--blue { background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: white; }
.modal-btn--blue:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.modal-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ===== 底部 ===== */
.seller-footer { position: relative; z-index: 1; padding: 24px 56px; text-align: center; }
.seller-footer span { font-size: 0.7rem; color: #adb5bd; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .seller-card { padding: 32px 20px; }
}
</style>