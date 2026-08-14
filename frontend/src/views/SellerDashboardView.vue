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
        <button class="nav-btn" @click="goHome">
          <span class="nav-btn-icon">🏪</span>
          <span class="nav-btn-text">返回商城</span>
        </button>
        <button class="nav-btn nav-btn--profile" @click="goProfile">个人中心</button>
        <button class="nav-btn nav-btn--outline" @click="handleLogout">
          <span class="nav-btn-icon">🚪</span>
          <span class="nav-btn-text">退出登录</span>
        </button>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="seller-wrapper">
      <div class="seller-card">
        <div class="seller-header">
          <div class="seller-icon">🏪</div>
          <h2 class="seller-title">商家工作台</h2>
          <p class="seller-desc">
            {{ shopName }}
            <span v-if="isVip" class="vip-chip">★ 会员卖家旗舰店</span>
          </p>
        </div>

        <!-- 标签页 -->
        <div class="tabs">
          <button v-for="t in tabs" :key="t.key"
                  class="tab-btn" :class="{ active: activeTab === t.key }"
                  @click="switchTab(t.key)">
            <span class="tab-icon">{{ t.icon }}</span>
            <span class="tab-label">{{ t.label }}</span>
            <span v-if="t.key === 'messages' && totalUnread > 0" class="tab-badge">{{ totalUnread }}</span>
          </button>
        </div>

        <!-- ============ 商品管理 ============ -->
        <div v-if="activeTab === 'products'" class="section">
          <div class="section-head">
            <h3 class="section-title">商品管理</h3>
            <button class="add-btn" @click="openAddDialog">+ 新增商品</button>
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
                  <td>
                    <span v-if="prod.recommend === 1" class="status-badge status--recommend">★推荐</span>
                    <span v-else style="color:#adb5bd;font-size:0.75rem;">普通</span>
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
        </div>

        <!-- ============ 订单管理 ============ -->
        <div v-if="activeTab === 'orders'" class="section">
          <h3 class="section-title">订单管理</h3>
          <div class="order-status-filter">
            <button v-for="s in orderStatusOptions" :key="s.value"
                    class="filter-tab" :class="{ active: orderStatusFilter === s.value }"
                    @click="orderStatusFilter = s.value; orderPage = 1; fetchOrders()">
              {{ s.label }}
            </button>
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
                  <td>{{ order.orderNo || order.id }}</td>
                  <td>¥{{ (order.totalAmount || 0).toFixed(2) }}</td>
                  <td>
                    <span class="order-status" :class="getOrderStatusClass(order.status)">
                      {{ getOrderStatusText(order.status) }}
                    </span>
                  </td>
                  <td>{{ order.receiverName || '-' }}</td>
                  <td>{{ formatDate(order.createTime) }}</td>
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
        </div>

        <!-- ============ 消息中心 ============ -->
        <div v-if="activeTab === 'messages'" class="section">
          <h3 class="section-title">消息中心</h3>
          <div class="chat-panel">
            <!-- 会话列表 -->
            <div class="conv-list">
              <div class="conv-loading" v-if="convLoading">加载中...</div>
              <div class="conv-empty" v-else-if="conversations.length === 0">暂无会话</div>
              <div v-for="c in conversations" :key="c.id"
                   class="conv-item" :class="{ active: currentConv && currentConv.id === c.id }"
                   @click="openConv(c)">
                <span class="conv-avatar" v-if="c.avatar"><img :src="c.avatar" alt=""></span>
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
        </div>

        <!-- ============ 店铺信息 ============ -->
        <div v-if="activeTab === 'shop'" class="section">
          <h3 class="section-title">店铺信息</h3>
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
            <button class="add-btn" @click="saveShop" :disabled="actionLoading">保存修改</button>
          </div>
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
              <label>分类</label>
              <div class="modal-field"><input v-model="form.category" type="text" placeholder="请输入分类" /></div>
            </div>
            <div class="modal-field-row">
              <label>商品描述</label>
              <div class="modal-field"><textarea v-model="form.description" class="modal-textarea" placeholder="请输入商品描述"></textarea></div>
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

    <!-- ===== 订单详情弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showOrderDetail" class="modal-overlay" @click.self="showOrderDetail = false">
        <div class="modal-card modal-card--wide">
          <div class="modal-icon">📋</div>
          <h3 class="modal-title">订单详情</h3>
          <div class="order-detail" v-if="orderDetail">
            <div class="detail-row"><span>订单号</span><b>{{ orderDetail.order.orderNo || orderDetail.order.id }}</b></div>
            <div class="detail-row"><span>状态</span><b>{{ getOrderStatusText(orderDetail.order.status) }}</b></div>
            <div class="detail-row"><span>金额</span><b>¥{{ (orderDetail.order.totalAmount || 0).toFixed(2) }}</b></div>
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
    <div class="seller-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
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
  getUserInfo
} from '../api/index.js'
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
  showDialog.value = true
}

async function submitForm() {
  if (!form.value.productName.trim()) { showMessage('请输入商品名称', false); return }
  if (!form.value.price || Number(form.value.price) <= 0) { showMessage('请输入有效价格', false); return }
  formLoading.value = true
  try {
    const payload = {
      productName: form.value.productName,
      price: Number(form.value.price),
      stock: Number(form.value.stock) || 0,
      category: form.value.category,
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
  pollTimer = setInterval(pollMessages, 4000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
/* ===== 全局 ===== */
.seller-page {
  min-height: 100vh; display: flex; flex-direction: column; align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif; position: relative; overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.bg-circle { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35; }
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
.nav-actions { display: flex; gap: 10px; }
.nav-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: #fff; font-family: 'DM Sans', sans-serif; font-size: 0.8rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.nav-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.nav-btn--profile { background: transparent; color: #495057; border: 1px solid #dee2e6; }
.nav-btn--profile:hover { border-color: #2b6cb0; color: #2b6cb0; box-shadow: none; }
.nav-btn--outline { background: transparent; color: #495057; border: 1px solid #dee2e6; }
.nav-btn--outline:hover { background: rgba(201,42,42,0.06); border-color: #c92a2a; color: #c92a2a; box-shadow: none; }

/* ===== 主卡片 ===== */
.seller-wrapper { flex: 1; display: flex; align-items: flex-start; justify-content: center; width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1; }
.seller-card { width: 1150px; max-width: 100%; padding: 44px 38px; background: rgba(255,255,255,0.75); backdrop-filter: blur(24px) saturate(1.4); border-radius: 24px; border: 1px solid rgba(255,255,255,0.50); box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06); animation: cardIn 0.6s both; }
@keyframes cardIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.seller-header { text-align: center; margin-bottom: 28px; }
.seller-icon { font-size: 2rem; margin-bottom: 10px; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.6; } 50% { transform: scale(1.1); opacity: 1; } }
.seller-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700; color: #212529; margin-bottom: 8px; }
.seller-desc { font-size: 0.9rem; color: #868e96; display: flex; align-items: center; justify-content: center; gap: 8px; }
.vip-chip { padding: 3px 12px; border-radius: 50px; background: linear-gradient(135deg, #7c3aed, #4a9eff); color: #fff; font-size: 0.72rem; font-weight: 700; }

/* ===== 标签页 ===== */
.tabs { display: flex; gap: 8px; margin-bottom: 28px; flex-wrap: wrap; border-bottom: 1px solid #dee2e6; padding-bottom: 12px; }
.tab-btn { display: flex; align-items: center; gap: 6px; padding: 10px 20px; border: 1px solid #dee2e6; border-radius: 10px; background: transparent; color: #495057; font-size: 0.85rem; font-weight: 500; cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif; position: relative; }
.tab-btn:hover { border-color: #4a9eff; color: #4a9eff; }
.tab-btn.active { background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); border-color: transparent; color: #fff; }
.tab-badge { min-width: 18px; height: 18px; padding: 0 5px; border-radius: 50px; background: #e03131; color: #fff; font-size: 0.68rem; font-weight: 700; display: flex; align-items: center; justify-content: center; }

/* ===== 分区 ===== */
.section { margin-bottom: 20px; }
.section-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.1rem; font-weight: 700; color: #343a40; margin: 0 0 16px; padding-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.06); }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-head .section-title { margin-bottom: 0; border-bottom: none; padding-bottom: 0; }

/* ===== 表格 ===== */
.table-wrap { overflow-x: auto; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); }
.data-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; min-width: 760px; }
.data-table th { text-align: left; padding: 12px 14px; background: rgba(43,108,176,0.04); color: #495057; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em; border-bottom: 1px solid rgba(0,0,0,0.06); white-space: nowrap; }
.data-table td { padding: 12px 14px; color: #212529; border-bottom: 1px solid rgba(0,0,0,0.04); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: rgba(43,108,176,0.02); }
.name-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-cell { display: flex; gap: 6px; flex-wrap: wrap; }

/* ===== 状态标签 ===== */
.status-badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
.status--active { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.status--disabled { background: rgba(201,42,42,0.08); color: #c92a2a; }
.status--recommend { background: rgba(124,58,237,0.1); color: #7c3aed; }
.order-status { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 600; }
.order-status.status--pending { background: rgba(240,140,0,0.08); color: #f08c00; }
.order-status.status--paid { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.order-status.status--shipped { background: rgba(124,58,237,0.08); color: #7c3aed; }
.order-status.status--completed { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.order-status.status--cancelled { background: rgba(134,142,150,0.08); color: #868e96; }
.order-status.status--refunded { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 按钮 ===== */
.action-btn { padding: 6px 14px; border: 1px solid #dee2e6; border-radius: 8px; background: transparent; color: #495057; font-size: 0.75rem; font-weight: 500; cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif; white-space: nowrap; }
.action-btn:hover:not(:disabled) { transform: translateY(-1px); }
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.action-btn--toggle { color: #2b6cb0; border-color: rgba(43,108,176,0.2); }
.action-btn--recommend { color: #7c3aed; border-color: rgba(124,58,237,0.2); }
.action-btn--complete { color: #2b8a3e; border-color: rgba(43,138,62,0.2); }
.action-btn--detail { color: #7c3aed; border-color: rgba(124,58,237,0.2); }
.add-btn { padding: 10px 24px; border: none; border-radius: 10px; background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); color: #fff; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.add-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 24px rgba(43,108,176,0.3); }

/* ===== 筛选 ===== */
.order-status-filter { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.filter-tab { padding: 6px 16px; border: 1px solid #dee2e6; border-radius: 50px; background: transparent; color: #868e96; font-size: 0.78rem; font-weight: 500; cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif; }
.filter-tab:hover { border-color: #4a9eff; color: #4a9eff; }
.filter-tab.active { background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed); border-color: transparent; color: #fff; }

/* ===== 分页 ===== */
.pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 20px; }
.page-btn { padding: 8px 16px; border: 1px solid #dee2e6; border-radius: 8px; background: transparent; color: #495057; font-size: 0.85rem; cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif; }
.page-btn:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: #868e96; }

/* ===== 加载 & 空状态 ===== */
.loading-state, .empty-state { text-align: center; padding: 40px 0; color: #868e96; }
.loading-spinner { width: 32px; height: 32px; margin: 0 auto 12px; border: 3px solid #f1f3f5; border-top-color: #2b6cb0; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 消息中心 ===== */
.chat-panel { display: flex; height: 480px; border: 1px solid rgba(0,0,0,0.05); border-radius: 14px; overflow: hidden; }
.conv-list { width: 260px; flex-shrink: 0; border-right: 1px solid #f1f3f5; overflow-y: auto; background: #fafbfc; }
.conv-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; cursor: pointer; transition: all 0.2s; border-bottom: 1px solid #f1f3f5; position: relative; }
.conv-item:hover { background: rgba(43,108,176,0.04); }
.conv-item.active { background: linear-gradient(135deg, rgba(43,108,176,0.08), rgba(124,58,237,0.08)); }
.conv-avatar { width: 36px; height: 36px; border-radius: 50%; overflow: hidden; background: linear-gradient(135deg, #2b6cb0, #7c3aed); flex-shrink: 0; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700; }
.conv-avatar img { width: 100%; height: 100%; object-fit: cover; }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 0.85rem; font-weight: 600; color: #212529; margin: 0; }
.conv-preview { font-size: 0.75rem; color: #adb5bd; margin: 2px 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-badge { position: absolute; top: 12px; right: 12px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 50px; background: #e03131; color: #fff; font-size: 0.68rem; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.conv-loading, .conv-empty { text-align: center; padding: 40px 10px; color: #adb5bd; font-size: 0.82rem; }
.conv-chat { flex: 1; display: flex; flex-direction: column; background: #fff; }
.conv-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #adb5bd; font-size: 0.88rem; gap: 10px; }
.conv-placeholder span { font-size: 2.4rem; }
.chat-msgs { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.chat-msg { display: flex; flex-direction: column; max-width: 80%; }
.chat-msg--mine { align-self: flex-end; align-items: flex-end; }
.chat-bubble { padding: 9px 13px; border-radius: 13px; font-size: 0.85rem; line-height: 1.45; background: #f1f3f5; color: #212529; border-top-left-radius: 4px; word-break: break-word; }
.chat-msg--mine .chat-bubble { background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: #fff; border-top-left-radius: 13px; border-top-right-radius: 4px; }
.chat-time { font-size: 0.66rem; color: #adb5bd; margin-top: 3px; }
.chat-input-bar { display: flex; gap: 10px; padding: 12px 14px; border-top: 1px solid #f1f3f5; }
.chat-input-bar input { flex: 1; padding: 10px 14px; border: 1px solid #dee2e6; border-radius: 10px; font-size: 0.85rem; font-family: 'DM Sans', sans-serif; outline: none; }
.chat-input-bar input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }
.chat-send-btn { padding: 10px 20px; border: none; border-radius: 10px; background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: #fff; font-family: 'DM Sans', sans-serif; font-size: 0.82rem; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.chat-send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.3); }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 店铺信息 ===== */
.vip-banner { display: flex; align-items: center; gap: 14px; padding: 16px 20px; margin-bottom: 20px; border-radius: 14px; background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(43,108,176,0.08)); border: 1px solid rgba(124,58,237,0.2); }
.vip-banner-icon { font-size: 2rem; }
.vip-banner-title { font-weight: 700; color: #7c3aed; margin: 0 0 4px; }
.vip-banner-desc { font-size: 0.82rem; color: #495057; margin: 0; }
.shop-form { max-width: 520px; }
.form-field { margin-bottom: 14px; }
.form-field label { display: block; font-size: 0.78rem; font-weight: 600; color: #495057; margin-bottom: 6px; }
.form-field input { width: 100%; padding: 11px 14px; border: 1px solid #dee2e6; border-radius: 10px; font-size: 0.88rem; font-family: 'DM Sans', sans-serif; outline: none; transition: border-color 0.2s; }
.form-field input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }

/* ===== 订单详情 ===== */
.order-detail { text-align: left; }
.detail-row { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f1f3f5; font-size: 0.85rem; }
.detail-row span { color: #868e96; flex-shrink: 0; }
.detail-row b { color: #212529; text-align: right; }
.detail-items { margin-top: 12px; }
.detail-item { display: flex; justify-content: space-between; padding: 8px 12px; background: #f8f9fa; border-radius: 8px; margin-bottom: 6px; font-size: 0.82rem; }

/* ===== 弹窗 ===== */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 200; animation: overlayIn 0.2s ease; }
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card { width: 460px; max-width: 90%; padding: 32px; background: rgba(255,255,255,0.92); backdrop-filter: blur(24px); border-radius: 20px; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 20px 60px rgba(0,0,0,0.12); text-align: center; animation: modalIn 0.3s; }
.modal-card--wide { width: 520px; }
@keyframes modalIn { from { opacity: 0; transform: translateY(16px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
.modal-icon { font-size: 2.2rem; margin-bottom: 10px; }
.modal-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.25rem; font-weight: 700; color: #212529; margin-bottom: 18px; }
.modal-form { text-align: left; margin-bottom: 18px; }
.modal-field-row { margin-bottom: 12px; }
.modal-field-row label { display: block; font-size: 0.78rem; font-weight: 600; color: #495057; margin-bottom: 6px; }
.modal-field { display: flex; align-items: center; padding: 0 14px; border: 1px solid #dee2e6; border-radius: 10px; background: rgba(255,255,255,0.6); }
.modal-field:focus-within { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.12); }
.modal-field input, .modal-textarea { flex: 1; border: none; background: transparent; padding: 11px 0; font-size: 0.88rem; font-family: 'DM Sans', sans-serif; color: #212529; outline: none; }
.modal-textarea { min-height: 76px; resize: vertical; line-height: 1.5; }
.modal-actions { display: flex; gap: 12px; }
.modal-btn { flex: 1; padding: 12px; border: none; border-radius: 10px; font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.modal-btn--cancel { background: #f1f3f5; color: #495057; }
.modal-btn--blue { background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: #fff; }
.modal-btn--blue:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }
.modal-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ===== 消息 ===== */
.msg { margin-top: 16px; text-align: center; font-size: 0.85rem; padding: 10px; border-radius: 10px; animation: fadeIn 0.3s; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: #2b8a3e; background: rgba(43,138,62,0.06); border: 1px solid rgba(43,138,62,0.12); }
.msg--error { color: #c92a2a; background: rgba(201,42,42,0.06); border: 1px solid rgba(201,42,42,0.12); }

/* ===== 底部 ===== */
.seller-footer { position: relative; z-index: 1; padding: 24px 56px; text-align: center; }
.seller-footer span { font-size: 0.7rem; color: #adb5bd; }

@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .seller-card { padding: 28px 18px; }
  .nav-btn-text { display: none; }
  .chat-panel { flex-direction: column; height: auto; }
  .conv-list { width: 100%; border-right: none; border-bottom: 1px solid #f1f3f5; max-height: 200px; }
  .conv-chat { min-height: 360px; }
}
</style>
