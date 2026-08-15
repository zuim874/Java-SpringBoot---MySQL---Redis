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
        <button class="nav-btn nav-btn--profile" @click="goProfile">个人中心</button>
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
          <h1 class="page-title">商家工作台</h1>
          <p class="page-sub">
            <span class="page-sub-shop">{{ shopName }}</span>
            <span v-if="isVip" class="vip-chip">★ 会员卖家旗舰店</span>
          </p>
        </div>
        <p class="page-tip">一站式管理 商品 · 订单 · 客服沟通 · 店铺信息</p>
      </header>

      <!-- 标签页导航 -->
      <nav class="tabs">
        <button v-for="t in tabs" :key="t.key"
                class="tab-btn" :class="{ active: activeTab === t.key }"
                @click="switchTab(t.key)">
          <span class="tab-icon">{{ t.icon }}</span>
          <span class="tab-label">{{ t.label }}</span>
          <span v-if="t.key === 'messages' && totalUnread > 0" class="tab-badge">{{ totalUnread }}</span>
        </button>
      </nav>

      <!-- ============ 商品管理 ============ -->
      <section v-if="activeTab === 'products'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">商品管理</h3>
          <button class="btn btn--primary" @click="openAddDialog">
            <span class="btn-icon">＋</span>新增商品
          </button>
        </div>

        <div class="loading-state" v-if="productsLoading">
          <div class="loading-spinner"></div>
          <p>加载商品...</p>
        </div>
        <div class="empty-state" v-else-if="products.length === 0">
          <p>暂无商品，点击右上角新增</p>
        </div>
        <div class="table-wrap" v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th><th>商品名</th><th>价格</th><th>库存</th>
                <th>销量</th><th>分类</th><th>状态</th><th>推荐</th><th>操作</th>
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
                <td>
                  <span v-if="prod.recommend === 1" class="status-badge status--recommend">★ 推荐位</span>
                  <span v-else class="cell-dim">普通</span>
                </td>
                <td class="action-cell">
                  <button class="action-btn" @click="openEditDialog(prod)">编辑</button>
                  <button class="action-btn action-btn--toggle" @click="toggleStatus(prod)" :disabled="actionLoading">
                    {{ prod.status === 1 ? '下架' : '上架' }}
                  </button>
                  <button v-if="isVip" class="action-btn action-btn--recommend"
                          @click="toggleRecommend(prod)" :disabled="actionLoading">
                    {{ prod.recommend === 1 ? '取消推荐' : '置顶推荐' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination" v-if="productTotalPages > 1">
          <button class="page-btn" :disabled="productPage <= 1" @click="productPage--; fetchProducts()">‹</button>
          <span class="page-info">{{ productPage }} / {{ productTotalPages }}</span>
          <button class="page-btn" :disabled="productPage >= productTotalPages" @click="productPage++; fetchProducts()">›</button>
        </div>
      </section>

      <!-- ============ 订单管理 ============ -->
      <section v-if="activeTab === 'orders'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">订单管理</h3>
          <div class="order-status-filter">
            <button v-for="s in orderStatusOptions" :key="s.value"
                    class="filter-tab" :class="{ active: orderStatusFilter === s.value }"
                    @click="orderStatusFilter = s.value; orderPage = 1; fetchOrders()">
              {{ s.label }}
            </button>
          </div>
        </div>

        <div class="loading-state" v-if="ordersLoading">
          <div class="loading-spinner"></div>
          <p>加载订单...</p>
        </div>
        <div class="empty-state" v-else-if="orders.length === 0">
          <p>暂无相关订单</p>
        </div>
        <div class="table-wrap" v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>订单号</th><th>金额</th><th>状态</th><th>收货人</th><th>时间</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orders" :key="order.id">
                <td class="cell-mono">{{ order.orderNo || order.id }}</td>
                <td class="price">¥{{ (order.totalAmount || 0).toFixed(2) }}</td>
                <td>
                  <span class="status-badge" :class="getOrderStatusClass(order.status)">
                    {{ getOrderStatusText(order.status) }}
                  </span>
                </td>
                <td>{{ order.receiverName || '-' }}</td>
                <td class="cell-dim">{{ formatDate(order.createTime) }}</td>
                <td class="action-cell">
                  <button class="action-btn action-btn--complete" @click="handleShip(order)"
                          :disabled="actionLoading || order.status !== 1">发货</button>
                  <button class="action-btn action-btn--detail" @click="viewOrderDetail(order)">详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination" v-if="orderTotalPages > 1">
          <button class="page-btn" :disabled="orderPage <= 1" @click="orderPage--; fetchOrders()">‹</button>
          <span class="page-info">{{ orderPage }} / {{ orderTotalPages }}</span>
          <button class="page-btn" :disabled="orderPage >= orderTotalPages" @click="orderPage++; fetchOrders()">›</button>
        </div>
      </section>

      <!-- ============ 消息中心 ============ -->
      <section v-if="activeTab === 'messages'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">消息中心</h3>
        </div>
        <div class="chat-panel">
          <!-- 会话列表 -->
          <div class="conv-list">
            <div class="conv-loading" v-if="convLoading">加载中...</div>
            <div class="conv-empty" v-else-if="conversations.length === 0">暂无会话</div>
            <div v-for="c in conversations" :key="c.id"
                 class="conv-item" :class="{ active: currentConv && currentConv.id === c.id }"
                 @click="openConv(c)">
              <span class="conv-avatar" v-if="c.avatar"><img :src="getCachedAvatar(c.avatar)" alt=""></span>
              <span class="conv-avatar conv-avatar--text" v-else>{{ (c.nickname || c.username || '?').charAt(0) }}</span>
              <div class="conv-info">
                <p class="conv-name">{{ c.nickname || c.username }}</p>
                <p class="conv-preview">{{ c.lastMessage || '暂无消息' }}</p>
              </div>
              <span v-if="c.unread > 0" class="conv-badge">{{ c.unread }}</span>
            </div>
          </div>
          <!-- 聊天窗口 -->
          <div class="conv-chat">
            <template v-if="currentConv">
              <div class="chat-msgs" ref="chatBodyRef">
                <div v-for="m in currentMessages" :key="m.id"
                     class="chat-msg" :class="{ 'chat-msg--mine': m.senderRole === 'SELLER' }">
                  <div class="chat-bubble">{{ m.content }}</div>
                  <span class="chat-time">{{ formatTime(m.createTime) }}</span>
                </div>
              </div>
              <div class="chat-input-bar">
                <input v-model="chatDraft" type="text" maxlength="500" placeholder="回复买家，Enter 发送"
                       @keyup.enter="sendReply" />
                <button class="chat-send-btn" :disabled="sendingReply || !chatDraft.trim()" @click="sendReply">
                  {{ sendingReply ? '发送中...' : '发送' }}
                </button>
              </div>
            </template>
            <div v-else class="conv-placeholder">
              <span>💬</span>
              <p>选择一个会话开始沟通</p>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 店铺信息 ============ -->
      <section v-if="activeTab === 'shop'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">店铺信息</h3>
        </div>
        <div v-if="isVip" class="vip-banner">
          <span class="vip-banner-icon">👑</span>
          <div>
            <p class="vip-banner-title">会员卖家专属福利</p>
            <p class="vip-banner-desc">① 商品可设置推荐位，商城首页置顶曝光；② 店铺展示「旗舰店」标识，提升买家信任；③ 优先触达优质买家流量。</p>
          </div>
        </div>
        <div class="shop-form">
          <div class="form-field">
            <label>店铺名称</label>
            <input v-model="shopForm.sellerName" type="text" placeholder="店铺名称" />
          </div>
          <div class="form-field">
            <label>店铺地址</label>
            <input v-model="shopForm.address" type="text" placeholder="店铺地址" />
          </div>
          <div class="form-field">
            <label>客服电话</label>
            <input v-model="shopForm.sellerContact" type="text" placeholder="客服电话" />
          </div>
          <div class="form-field">
            <label>店铺 Logo URL</label>
            <input v-model="shopForm.sellerAvatar" type="text" placeholder="店铺 Logo 图片地址" />
          </div>
          <button class="btn btn--primary" @click="saveShop" :disabled="actionLoading">保存修改</button>
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
              <div class="modal-field"><input v-model="form.productName" type="text" placeholder="请输入商品名称" /></div>
            </div>
            <div class="modal-field-row">
              <label>价格 (¥)</label>
              <div class="modal-field"><input v-model="form.price" type="number" step="0.01" placeholder="请输入价格" /></div>
            </div>
            <div class="modal-field-row">
              <label>库存</label>
              <div class="modal-field"><input v-model="form.stock" type="number" placeholder="请输入库存" /></div>
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
              <div class="modal-field"><textarea v-model="form.description" class="modal-textarea" placeholder="请输入商品描述"></textarea></div>
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

    <!-- ===== 订单详情弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showOrderDetail" class="modal-overlay" @click.self="showOrderDetail = false">
        <div class="modal-card modal-card--wide">
          <div class="modal-head">
            <span class="modal-icon">📋</span>
            <h3 class="modal-title">订单详情</h3>
          </div>
          <div class="order-detail" v-if="orderDetail">
            <div class="detail-row"><span>订单号</span><b>{{ orderDetail.order.orderNo || orderDetail.order.id }}</b></div>
            <div class="detail-row"><span>状态</span><b>{{ getOrderStatusText(orderDetail.order.status) }}</b></div>
            <div class="detail-row"><span>金额</span><b class="price">¥{{ (orderDetail.order.totalAmount || 0).toFixed(2) }}</b></div>
            <div class="detail-row"><span>收货人</span><b>{{ orderDetail.order.receiverName }} {{ orderDetail.order.receiverPhone }}</b></div>
            <div class="detail-row"><span>地址</span><b>{{ orderDetail.order.receiverAddress }}</b></div>
            <div class="detail-row"><span>备注</span><b>{{ orderDetail.order.remark || '-' }}</b></div>
            <div class="detail-items">
              <div v-for="it in orderDetail.items" :key="it.id" class="detail-item">
                <span>{{ it.productName }} ×{{ it.quantity }}</span>
                <span>¥{{ (it.subtotal || 0).toFixed(2) }}</span>
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showOrderDetail = false">关闭</button>
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  getSellerProducts, addSellerProduct, updateSellerProduct,
  onshelfProduct, offshelfProduct, sellerSetRecommend,
  getSellerOrders, getSellerOrderDetail, sellerShipOrder,
  getSellerShop, updateSellerShop,
  getSellerConversations, sendSellerMessage, getSellerMessages, markSellerConversationRead,
  getUserInfo, getCategoryList
} from '../api/index.js'
import { getCachedAvatar } from '../utils/avatarCache.js'
import { toastError } from '../utils/toast.js'

const router = useRouter()

// ===== 身份信息 =====
const nickname = ref(localStorage.getItem('nickname') || '')
const isVip = ref(false)

// ===== 标签页 =====
const activeTab = ref('products')
const tabs = [
  { key: 'products', label: '商品管理', icon: '📦' },
  { key: 'orders', label: '订单管理', icon: '📋' },
  { key: 'messages', label: '消息中心', icon: '💬' },
  { key: 'shop', label: '店铺信息', icon: '🏪' }
]

// ===== 通用 =====
const message = ref('')
const msgSuccess = ref(false)
const actionLoading = ref(false)

function showMessage(msg, success) {
  message.value = msg
  msgSuccess.value = success
  setTimeout(() => { message.value = '' }, 3000)
}

// ===== 商品管理 =====
const products = ref([])
const productsLoading = ref(false)
const productPage = ref(1)
const productTotalPages = ref(1)
const showDialog = ref(false)
const editingProduct = ref(null)
const formLoading = ref(false)
const form = ref({ productName: '', price: '', stock: '', category: '', description: '' })
// 预置分类（管理员维护，卖家只能从中选取，不可自定义）
const presetCategories = ref([])
// 分类ID → 名称 映射（用于回显/展示）
const categoryNameById = ref({})
// 已选分类ID列表（以英文逗号分隔存储到 form.category）
const selectedCategories = ref([])

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

function getProductCategoryNames(categoryIds) {
  if (!categoryIds) return []
  const names = []
  String(categoryIds).split(',').forEach(part => {
    const id = Number(part.trim())
    if (id && categoryNameById.value[id]) names.push(categoryNameById.value[id])
  })
  return names
}

function toggleCategory(cat) {
  const index = selectedCategories.value.indexOf(cat.id)
  if (index === -1) selectedCategories.value.push(cat.id)
  else selectedCategories.value.splice(index, 1)
}

function removeCategory(index) {
  selectedCategories.value.splice(index, 1)
}

async function fetchProducts() {
  productsLoading.value = true
  try {
    const res = await getSellerProducts(productPage.value, 10)
    if (res && res.code === 200 && res.data) {
      products.value = res.data.records || []
      productTotalPages.value = res.data.pages || 1
    }
  } catch {
    products.value = []
  } finally {
    productsLoading.value = false
  }
}

function openAddDialog() {
  editingProduct.value = null
  form.value = { productName: '', price: '', stock: '', category: '', description: '' }
  selectedCategories.value = []
  showDialog.value = true
}

function openEditDialog(prod) {
  editingProduct.value = prod
  form.value = {
    productName: prod.productName || '',
    price: prod.price || '',
    stock: prod.stock || '',
    category: prod.category || '',
    description: prod.description || ''
  }
  // 分类ID集合解析为已选分类ID数组（商品的 category 存分类ID）
  selectedCategories.value = String(prod.category || '').split(',').map(s => Number(s.trim())).filter(n => n > 0)
  showDialog.value = true
}

async function submitForm() {
  if (!form.value.productName.trim()) { showMessage('请输入商品名称', false); return }
  if (!form.value.price || Number(form.value.price) <= 0) { showMessage('请输入有效价格', false); return }
  if (selectedCategories.value.length === 0) { showMessage('请至少选择一个分类', false); return }
  formLoading.value = true
  try {
    const payload = {
      productName: form.value.productName,
      price: Number(form.value.price),
      stock: Number(form.value.stock) || 0,
      category: selectedCategories.value.join(','),
      description: form.value.description
    }
    const res = editingProduct.value
      ? await updateSellerProduct(editingProduct.value.id, payload)
      : await addSellerProduct(payload)
    if (res && res.code === 200) {
      showMessage(editingProduct.value ? '商品已更新' : '商品已新增', true)
      showDialog.value = false
      fetchProducts()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch {
    toastError('操作失败，网络异常')
  } finally {
    formLoading.value = false
  }
}

async function toggleStatus(prod) {
  actionLoading.value = true
  try {
    const res = prod.status === 1 ? await offshelfProduct(prod.id) : await onshelfProduct(prod.id)
    if (res && res.code === 200) {
      showMessage(prod.status === 1 ? '已下架' : '已上架', true)
      fetchProducts()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch {
    toastError('网络异常')
  } finally {
    actionLoading.value = false
  }
}

async function toggleRecommend(prod) {
  actionLoading.value = true
  try {
    const res = await sellerSetRecommend(prod.id, prod.recommend === 1 ? 0 : 1)
    if (res && res.code === 200) {
      showMessage(res.mes || '操作成功', true)
      fetchProducts()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch {
    toastError('网络异常')
  } finally {
    actionLoading.value = false
  }
}

// ===== 订单管理 =====
const orders = ref([])
const ordersLoading = ref(false)
const orderPage = ref(1)
const orderTotalPages = ref(1)
const orderStatusFilter = ref('')
const orderStatusOptions = [
  { value: '', label: '全部' },
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付' },
  { value: 2, label: '已发货' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已退款' }
]
const showOrderDetail = ref(false)
const orderDetail = ref(null)

async function fetchOrders() {
  ordersLoading.value = true
  try {
    const res = await getSellerOrders(orderPage.value, 10, orderStatusFilter.value)
    if (res && res.code === 200 && res.data) {
      orders.value = res.data.records || []
      orderTotalPages.value = res.data.pages || 1
    }
  } catch {
    orders.value = []
  } finally {
    ordersLoading.value = false
  }
}

async function handleShip(order) {
  if (!confirm(`确定要发货订单 ${order.orderNo || order.id} 吗？`)) return
  actionLoading.value = true
  try {
    const res = await sellerShipOrder(order.id)
    if (res && res.code === 200) {
      showMessage('发货成功', true)
      fetchOrders()
    } else {
      showMessage((res && res.mes) || '发货失败', false)
    }
  } catch {
    toastError('网络异常')
  } finally {
    actionLoading.value = false
  }
}

async function viewOrderDetail(order) {
  try {
    const res = await getSellerOrderDetail(order.id)
    if (res && res.code === 200) {
      orderDetail.value = res.data
      showOrderDetail.value = true
    } else {
      toastError((res && res.mes) || '订单详情获取失败')
    }
  } catch {
    toastError('网络异常')
  }
}

// ===== 消息中心 =====
const conversations = ref([])
const convLoading = ref(false)
const currentConv = ref(null)
const currentMessages = ref([])
const chatDraft = ref('')
const sendingReply = ref(false)
const chatBodyRef = ref(null)
const totalUnread = computed(() => conversations.value.reduce((s, c) => s + (c.unread || 0), 0))

async function fetchConversations() {
  convLoading.value = true
  try {
    const res = await getSellerConversations()
    if (res && res.code === 200) {
      conversations.value = res.data || []
    }
  } catch {
    conversations.value = []
  } finally {
    convLoading.value = false
  }
}

async function openConv(c) {
  currentConv.value = c
  currentMessages.value = []
  await markSellerConversationRead(c.id)
  c.unread = 0
  const res = await getSellerMessages(c.id)
  if (res && res.code === 200) {
    currentMessages.value = res.data || []
  }
  scrollChat()
}

async function sendReply() {
  const content = chatDraft.value.trim()
  if (!content || !currentConv.value) return
  sendingReply.value = true
  try {
    const res = await sendSellerMessage(currentConv.value.userId, content)
    if (res && res.code === 200) {
      chatDraft.value = ''
      currentMessages.value.push(res.data)
      currentConv.value.lastMessage = content
      scrollChat()
    } else {
      toastError((res && res.mes) || '发送失败')
    }
  } catch {
    toastError('发送失败，网络异常')
  } finally {
    sendingReply.value = false
  }
}

// 消息轮询
async function pollMessages() {
  if (!currentConv.value) return
  const lastId = currentMessages.value.length
    ? currentMessages.value[currentMessages.value.length - 1].id
    : 0
  try {
    const res = await getSellerMessages(currentConv.value.id, lastId || undefined)
    if (res && res.code === 200 && res.data && res.data.length > 0) {
      currentMessages.value.push(...res.data)
      await markSellerConversationRead(currentConv.value.id)
      scrollChat()
    }
  } catch {
    // 静默
  }
  // 同时刷新会话列表（未读数）
  const cres = await getSellerConversations()
  if (cres && cres.code === 200) {
    conversations.value = cres.data || []
    // 保留当前选中会话
    const cur = conversations.value.find(c => currentConv.value && c.id === currentConv.value.id)
    if (cur) currentConv.value = cur
  }
}

function scrollChat() {
  nextTick(() => {
    if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  })
}

// ===== 店铺信息 =====
const shopForm = ref({ sellerName: '', address: '', sellerContact: '', sellerAvatar: '' })
const shopName = ref('')

async function fetchShop() {
  try {
    const res = await getSellerShop()
    if (res && res.code === 200 && res.data) {
      const s = res.data.seller || {}
      shopForm.value = {
        sellerName: s.sellerName || '',
        address: s.address || '',
        sellerContact: s.sellerContact || '',
        sellerAvatar: s.sellerAvatar || ''
      }
      shopName.value = s.sellerName || ''
      isVip.value = !!res.data.isVip
    }
  } catch {
    // ignore
  }
}

async function saveShop() {
  if (!shopForm.value.sellerName.trim()) { showMessage('店铺名称不能为空', false); return }
  actionLoading.value = true
  try {
    const res = await updateSellerShop(shopForm.value)
    if (res && res.code === 200) {
      showMessage('店铺信息已更新', true)
      shopName.value = shopForm.value.sellerName
    } else {
      showMessage((res && res.mes) || '更新失败', false)
    }
  } catch {
    toastError('网络异常')
  } finally {
    actionLoading.value = false
  }
}

// ===== 工具 =====
function getOrderStatusText(status) {
  const map = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款' }
  return map[status] !== undefined ? map[status] : '未知'
}
function getOrderStatusClass(status) {
  const map = { 0: 'status--pending', 1: 'status--paid', 2: 'status--shipped', 3: 'status--completed', 4: 'status--cancelled', 5: 'status--refunded' }
  return map[status] || 'status--unknown'
}
function formatDate(t) {
  if (!t) return '-'
  try { return new Date(t).toLocaleString('zh-CN', { hour12: false }) } catch { return t }
}
function formatTime(t) {
  if (!t) return ''
  try { return new Date(t).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) } catch { return '' }
}

function goHome() { router.push('/home') }
function goProfile() { router.push('/profile') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

function switchTab(key) {
  activeTab.value = key
  if (key === 'orders') fetchOrders()
  if (key === 'messages') fetchConversations()
  if (key === 'shop') fetchShop()
}

let pollTimer = null
onMounted(() => {
  fetchProducts()
  fetchShop()
  loadCategories()
  pollTimer = setInterval(pollMessages, 4000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
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
.page-sub { display: flex; align-items: center; gap: 8px; margin-top: 6px; font-size: 0.9rem; color: var(--text-3); }
.page-sub-shop { color: var(--text-2); font-weight: 600; }
.page-tip { font-size: 0.82rem; color: var(--text-3); }
.vip-chip {
  padding: 3px 12px; border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff; font-size: 0.72rem; font-weight: 700;
}

/* ===== 标签页 ===== */
.tabs { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
.tab-btn {
  position: relative; display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 18px; border-radius: var(--radius);
  border: 1px solid var(--border); background: var(--surface);
  color: var(--text-2); font-size: 0.88rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
}
.tab-btn:hover { border-color: var(--primary); color: var(--primary); }
.tab-btn.active {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  border-color: transparent; color: #fff; box-shadow: var(--shadow-primary);
}
.tab-icon { font-size: 1rem; }
.tab-badge {
  position: absolute; top: -7px; right: -7px; min-width: 20px; height: 20px; padding: 0 6px;
  border-radius: 50px; background: var(--danger); color: #fff;
  font-size: 0.7rem; font-weight: 700; display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--bg);
}

/* ===== 内容面板 ===== */
.panel {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
  padding: 24px; margin-bottom: 20px;
  animation: panelIn 0.35s ease both;
}
@keyframes panelIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px; flex-wrap: wrap; margin-bottom: 18px;
}
.panel-title {
  font-family: var(--font-display); font-size: 1.1rem; font-weight: 700;
  color: var(--text); margin: 0;
}

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
.cell-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.8rem; color: var(--text-2); }
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
.status--pending { background: var(--warning-soft); color: var(--warning); }
.status--paid { background: var(--primary-soft); color: var(--primary); }
.status--shipped { background: var(--accent-soft); color: var(--accent); }
.status--completed { background: var(--success-soft); color: var(--success); }
.status--cancelled { background: var(--surface-3); color: var(--text-3); }
.status--refunded { background: var(--danger-soft); color: var(--danger); }
.status--unknown { background: var(--surface-3); color: var(--text-3); }
.status--recommend { background: linear-gradient(135deg, rgba(124,58,237,0.12), rgba(19,194,194,0.12)); color: #7c3aed; }

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
.action-btn--recommend:hover:not(:disabled) { border-color: #7c3aed; color: #7c3aed; background: rgba(124,58,237,0.06); }
.action-btn--complete:hover:not(:disabled) { border-color: var(--success); color: var(--success); background: var(--success-soft); }
.action-btn--detail:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); background: var(--accent-soft); }

/* ===== 筛选 ===== */
.order-status-filter { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-tab {
  padding: 6px 14px; border-radius: var(--radius-pill);
  border: 1px solid var(--border); background: var(--surface);
  color: var(--text-3); font-size: 0.78rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
}
.filter-tab:hover { border-color: var(--primary); color: var(--primary); }
.filter-tab.active { background: var(--primary); border-color: var(--primary); color: #fff; }

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

/* ===== 消息中心 ===== */
.chat-panel {
  display: flex; height: 500px; border: 1px solid var(--border);
  border-radius: var(--radius); overflow: hidden; background: var(--surface);
}
.conv-list {
  width: 280px; flex-shrink: 0; border-right: 1px solid var(--border);
  overflow-y: auto; background: var(--surface-2);
}
.conv-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  cursor: pointer; transition: background 0.2s ease;
  border-bottom: 1px solid var(--border); position: relative;
}
.conv-item:hover { background: var(--surface); }
.conv-item.active { background: var(--primary-softer); }
.conv-avatar {
  width: 38px; height: 38px; border-radius: 50%; overflow: hidden;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  flex-shrink: 0; display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 0.85rem;
}
.conv-avatar img { width: 100%; height: 100%; object-fit: cover; }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 0.85rem; font-weight: 600; color: var(--text); margin: 0; }
.conv-preview { font-size: 0.75rem; color: var(--text-3); margin: 2px 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-badge {
  position: absolute; top: 12px; right: 12px; min-width: 19px; height: 19px; padding: 0 6px;
  border-radius: 50px; background: var(--danger); color: #fff;
  font-size: 0.68rem; font-weight: 700; display: flex; align-items: center; justify-content: center;
}
.conv-loading, .conv-empty { text-align: center; padding: 40px 10px; color: var(--text-4); font-size: 0.82rem; }
.conv-chat { flex: 1; display: flex; flex-direction: column; background: var(--surface); }
.conv-placeholder {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: var(--text-3); font-size: 0.88rem; gap: 10px;
}
.conv-placeholder span { font-size: 2.4rem; }
.chat-msgs { flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 12px; }
.chat-msg { display: flex; flex-direction: column; max-width: 78%; }
.chat-msg--mine { align-self: flex-end; align-items: flex-end; }
.chat-bubble {
  padding: 10px 14px; border-radius: 14px; font-size: 0.85rem; line-height: 1.5;
  background: var(--surface-2); color: var(--text); border: 1px solid var(--border);
  border-top-left-radius: 4px; word-break: break-word;
}
.chat-msg--mine .chat-bubble {
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff; border: none; border-top-left-radius: 14px; border-top-right-radius: 4px;
}
.chat-time { font-size: 0.66rem; color: var(--text-4); margin-top: 4px; }
.chat-input-bar {
  display: flex; gap: 10px; padding: 12px 14px;
  border-top: 1px solid var(--border); background: var(--surface);
}
.chat-input-bar input {
  flex: 1; padding: 10px 14px; border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); font-size: 0.85rem; outline: none; transition: all 0.2s ease;
}
.chat-input-bar input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.chat-send-btn {
  padding: 10px 22px; border: none; border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff; font-size: 0.82rem; font-weight: 600; cursor: pointer; transition: all 0.2s ease;
}
.chat-send-btn:hover:not(:disabled) { box-shadow: var(--shadow-primary); transform: translateY(-1px); }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 店铺信息 ===== */
.vip-banner {
  display: flex; align-items: center; gap: 14px; padding: 16px 20px; margin-bottom: 20px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(19,194,194,0.10));
  border: 1px solid rgba(124,58,237,0.18);
}
.vip-banner-icon { font-size: 2rem; }
.vip-banner-title { font-weight: 700; color: #7c3aed; margin: 0 0 4px; }
.vip-banner-desc { font-size: 0.82rem; color: var(--text-2); margin: 0; }
.shop-form { max-width: 560px; }
.form-field { margin-bottom: 16px; }
.form-field label {
  display: block; font-size: 0.78rem; font-weight: 600; color: var(--text-2); margin-bottom: 6px;
}
.form-field input {
  width: 100%; padding: 11px 14px; border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); font-size: 0.88rem; outline: none; transition: all 0.2s ease;
  background: var(--surface-2); color: var(--text);
}
.form-field input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); background: var(--surface); }

/* ===== 订单详情 ===== */
.order-detail { text-align: left; }
.detail-row {
  display: flex; justify-content: space-between; gap: 12px; padding: 9px 0;
  border-bottom: 1px solid var(--border); font-size: 0.85rem;
}
.detail-row span { color: var(--text-3); flex-shrink: 0; }
.detail-row b { color: var(--text); text-align: right; font-weight: 600; }
.detail-items { margin-top: 12px; }
.detail-item {
  display: flex; justify-content: space-between; padding: 9px 12px;
  background: var(--surface-2); border: 1px solid var(--border);
  border-radius: var(--radius-sm); margin-bottom: 6px; font-size: 0.82rem;
}

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(23, 35, 61, 0.45);
  backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center;
  z-index: 200; padding: 20px; animation: overlayIn 0.2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card {
  width: 480px; max-width: 100%; padding: 26px;
  background: var(--surface); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg); animation: modalIn 0.3s ease;
}
.modal-card--wide { width: 560px; }
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

/* ===== 分类多选（仅可从预设分类中选取） ===== */
.category-tag-input {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  width: 100%; padding: 8px 10px;
  border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface-2); transition: all 0.2s ease;
}
.category-tag-input:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); background: var(--surface); }
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
  .chat-panel { flex-direction: column; height: auto; }
  .conv-list { width: 100%; border-right: none; border-bottom: 1px solid var(--border); max-height: 220px; }
  .conv-chat { min-height: 380px; }
}
</style>
