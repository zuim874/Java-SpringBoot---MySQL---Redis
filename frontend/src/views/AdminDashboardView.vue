<template>
  <div class="admin-page">
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
    <div class="admin-wrapper">
      <div class="admin-card">
        <div class="admin-header">
          <div class="admin-icon">⚙</div>
          <h2 class="admin-title">管理后台</h2>
          <p class="admin-desc">系统综合管理面板</p>
        </div>

        <!-- ===== 标签页导航 ===== -->
        <div class="tabs">
          <button v-for="tab in tabs" :key="tab.key"
                  class="tab-btn"
                  :class="{ active: activeTab === tab.key }"
                  @click="activeTab = tab.key">
            <span class="tab-icon">{{ tab.icon }}</span>
            <span class="tab-label">{{ tab.label }}</span>
          </button>
        </div>

        <!-- ===== 用户管理标签 ===== -->
        <div v-if="activeTab === 'users'" class="section">
          <h3 class="section-title">用户管理</h3>
          <!-- 加载中 -->
          <div class="table-loading" v-if="usersLoading">
            <div class="loading-spinner"></div>
            <p>加载用户数据...</p>
          </div>
          <!-- 用户表格 -->
          <div class="table-wrap" v-else-if="users.length > 0">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>昵称</th>
                  <th>邮箱</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in users" :key="user.id">
                  <td>{{ user.id }}</td>
                  <td>{{ user.username }}</td>
                  <td>{{ user.nickname }}</td>
                  <td>{{ user.email || '-' }}</td>
                  <td>
                    <span class="role-badge" :class="getRoleClass(user.userRole)">
                      {{ formatRole(user.userRole) }}
                    </span>
                  </td>
                  <td>
                    <span :class="['status-badge', user.status === 1 ? 'status--active' : 'status--disabled']">
                      {{ user.status === 1 ? '正常' : '已禁用' }}
                    </span>
                  </td>
                  <td>{{ formatDate(user.createTime) }}</td>
                  <td class="action-cell">
                    <button class="action-btn action-btn--toggle" @click="toggleUserStatus(user)"
                            :disabled="actionLoading" :title="user.status === 1 ? '禁用' : '启用'">
                      {{ user.status === 1 ? '禁用' : '启用' }}
                    </button>
                    <button class="action-btn action-btn--delete" @click="handleDeleteUser(user)"
                            :disabled="actionLoading" title="删除">删除</button>
                    <button class="action-btn action-btn--recover" @click="handleRecoverUser(user)"
                            :disabled="actionLoading" title="恢复">恢复</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="empty-state" v-else>
            <p>暂无用户数据</p>
          </div>
          <!-- 用户分页 -->
          <div class="pagination" v-if="userTotalPages > 1">
            <button class="page-btn" :disabled="userPage <= 1" @click="userPage--; fetchUsers()">‹</button>
            <span class="page-info">{{ userPage }} / {{ userTotalPages }}</span>
            <button class="page-btn" :disabled="userPage >= userTotalPages" @click="userPage++; fetchUsers()">›</button>
          </div>

          <!-- 用户余额充值 -->
          <div class="charge-section">
            <h4 class="charge-title">用户余额充值</h4>
            <div class="charge-row">
              <div class="charge-field">
                <label>用户 ID</label>
                <input v-model="chargeUserId" type="number" min="1" placeholder="请输入用户ID" class="charge-input" />
              </div>
              <div class="charge-field">
                <label>充值金额</label>
                <input v-model="chargeAmount" type="number" step="0.01" min="0.01" placeholder="请输入金额" class="charge-input" />
              </div>
              <button class="charge-btn" :disabled="actionLoading" @click="handleCharge">确认充值</button>
            </div>
          </div>
        </div>

        <!-- ===== 商品管理标签 ===== -->
        <div v-if="activeTab === 'products'" class="section">
          <h3 class="section-title">商品管理</h3>
          <div class="table-loading" v-if="productsLoading">
            <div class="loading-spinner"></div>
            <p>加载商品数据...</p>
          </div>
          <div class="table-wrap" v-else-if="products.length > 0">
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
                  <td>{{ prod.productName }}</td>
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
                    <button class="action-btn" @click="toggleProductStatus(prod)"
                            :disabled="actionLoading">
                      {{ prod.status === 1 ? '下架' : '上架' }}
                    </button>
                    <button class="action-btn action-btn--delete" @click="handleDeleteProduct(prod)"
                            :disabled="actionLoading">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="empty-state" v-else>
            <p>暂无商品数据</p>
          </div>
          <div class="pagination" v-if="productTotalPages > 1">
            <button class="page-btn" :disabled="productPage <= 1" @click="productPage--; fetchProducts()">‹</button>
            <span class="page-info">{{ productPage }} / {{ productTotalPages }}</span>
            <button class="page-btn" :disabled="productPage >= productTotalPages" @click="productPage++; fetchProducts()">›</button>
          </div>
        </div>

        <!-- ===== 订单管理标签 ===== -->
        <div v-if="activeTab === 'orders'" class="section">
          <h3 class="section-title">订单管理</h3>
          <div class="order-status-filter">
            <button v-for="s in orderStatusOptions" :key="s.value"
                    class="filter-tab" :class="{ active: orderStatusFilter === s.value }"
                    @click="orderStatusFilter = s.value; orderPage = 1; fetchOrders()">
              {{ s.label }}
            </button>
          </div>
          <div class="table-loading" v-if="ordersLoading">
            <div class="loading-spinner"></div>
            <p>加载订单数据...</p>
          </div>
          <div class="table-wrap" v-else-if="orders.length > 0">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>用户</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.id">
                  <td>{{ order.orderNo || order.id }}</td>
                  <td>{{ order.userId }}</td>
                  <td>¥{{ (order.totalAmount || 0).toFixed(2) }}</td>
                  <td>
                    <span class="order-status" :class="getOrderStatusClass(order.status)">
                      {{ getOrderStatusText(order.status) }}
                    </span>
                  </td>
                  <td>{{ formatDate(order.createTime) }}</td>
                  <td class="action-cell">
                    <button class="action-btn" @click="handleShipOrder(order)"
                            :disabled="actionLoading || order.status !== 1">发货</button>
                    <button class="action-btn action-btn--complete" @click="handleCompleteOrder(order)"
                            :disabled="actionLoading || order.status !== 2">完成</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="empty-state" v-else>
            <p>暂无订单数据</p>
          </div>
          <div class="pagination" v-if="orderTotalPages > 1">
            <button class="page-btn" :disabled="orderPage <= 1" @click="orderPage--; fetchOrders()">‹</button>
            <span class="page-info">{{ orderPage }} / {{ orderTotalPages }}</span>
            <button class="page-btn" :disabled="orderPage >= orderTotalPages" @click="orderPage++; fetchOrders()">›</button>
          </div>
        </div>

        <!-- ===== 卖家管理标签 ===== -->
        <div v-if="activeTab === 'sellers'" class="section">
          <h3 class="section-title">卖家管理</h3>
          <div class="table-loading" v-if="sellersLoading">
            <div class="loading-spinner"></div>
            <p>加载卖家数据...</p>
          </div>
          <div class="table-wrap" v-else-if="sellers.length > 0">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>名称</th>
                  <th>地址</th>
                  <th>联系方式</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="seller in sellers" :key="seller.id">
                  <td>{{ seller.id }}</td>
                  <td>{{ seller.sellerName }}</td>
                  <td>{{ seller.address || '-' }}</td>
                  <td>{{ seller.sellerContact || '-' }}</td>
                  <td class="action-cell">
                    <button class="action-btn" @click="editSeller(seller)">编辑</button>
                    <button class="action-btn action-btn--delete" @click="handleDeleteSeller(seller)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="empty-state" v-else>
            <p>暂无卖家数据</p>
          </div>
          <div class="pagination" v-if="sellerTotalPages > 1">
            <button class="page-btn" :disabled="sellerPage <= 1" @click="sellerPage--; fetchSellers()">‹</button>
            <span class="page-info">{{ sellerPage }} / {{ sellerTotalPages }}</span>
            <button class="page-btn" :disabled="sellerPage >= sellerTotalPages" @click="sellerPage++; fetchSellers()">›</button>
          </div>
          <button class="add-btn" @click="showSellerForm = true">新增卖家</button>
        </div>

        <!-- ===== 充值审核标签 ===== -->
        <div v-if="activeTab === 'recharge'" class="section">
          <h3 class="section-title">充值申请审核</h3>
          <div class="table-loading" v-if="rechargeLoading">
            <div class="loading-spinner"></div>
            <p>加载充值申请...</p>
          </div>
          <div class="table-wrap" v-else-if="rechargeRequests.length > 0">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>申请时间</th>
                  <th>备注</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="req in rechargeRequests" :key="req.id">
                  <td>{{ req.id }}</td>
                  <td>{{ req.username }}</td>
                  <td style="color:#2b6cb0;font-weight:700;">¥{{ req.amount.toFixed(2) }}</td>
                  <td>
                    <span :class="['status-badge',
                      req.status === 0 ? 'status--pending' :
                      req.status === 1 ? 'status--active' : 'status--disabled']">
                      {{ req.status === 0 ? '待审核' : req.status === 1 ? '已通过' : '已拒绝' }}
                    </span>
                  </td>
                  <td>{{ formatDate(req.createTime) }}</td>
                  <td>{{ req.remark || '-' }}</td>
                  <td class="action-cell">
                    <template v-if="req.status === 0">
                      <button class="action-btn action-btn--complete" @click="handleApproveRecharge(req.id)">通过</button>
                      <button class="action-btn action-btn--delete" @click="handleRejectRecharge(req.id)">拒绝</button>
                    </template>
                    <span v-else style="color:#adb5bd;font-size:0.75rem;">已处理</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="empty-state" v-else-if="!rechargeLoading">
            <p>暂无充值申请</p>
          </div>
          <div class="pagination" v-if="rechargeTotalPages > 1">
            <button class="page-btn" :disabled="rechargePage <= 1" @click="rechargePage--; fetchRechargeRequests()">‹</button>
            <span class="page-info">{{ rechargePage }} / {{ rechargeTotalPages }}</span>
            <button class="page-btn" :disabled="rechargePage >= rechargeTotalPages" @click="rechargePage++; fetchRechargeRequests()">›</button>
          </div>
        </div>

        <!-- 消息提示 -->
        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>
      </div>
    </div>

    <!-- ===== 新增/编辑卖家弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showSellerForm" class="modal-overlay" @click.self="showSellerForm = false">
        <div class="modal-card">
          <div class="modal-icon">🏪</div>
          <h3 class="modal-title">{{ editingSeller ? '编辑卖家' : '新增卖家' }}</h3>
          <div class="modal-form">
            <div class="modal-field-row">
              <label>卖家名称</label>
              <div class="modal-field">
                <input v-model="sellerForm.sellerName" type="text" placeholder="请输入卖家名称" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>地址</label>
              <div class="modal-field">
                <input v-model="sellerForm.address" type="text" placeholder="请输入地址" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>联系方式</label>
              <div class="modal-field">
                <input v-model="sellerForm.sellerContact" type="text" placeholder="请输入联系方式" />
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showSellerForm = false">取消</button>
            <button class="modal-btn modal-btn--blue" @click="submitSellerForm" :disabled="actionLoading">
              {{ editingSeller ? '保存' : '新增' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 底部版权 -->
    <div class="admin-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAdminUsers, toggleUserStatus as apiToggleUserStatus, deleteUser, recoverUser,
  getAdminProducts,
  getAdminOrders, shipOrder, completeOrder,
  getAdminSellers,
  onshelfProduct, offshelfProduct,
  chargeUserBalance,
  getAdminRechargeRequests, approveRechargeRequest, rejectRechargeRequest
} from '../api/index.js'

const router = useRouter()

// ===== 标签页 =====
const activeTab = ref('users')
const tabs = [
  { key: 'users', label: '用户管理', icon: '👥' },
  { key: 'products', label: '商品管理', icon: '📦' },
  { key: 'orders', label: '订单管理', icon: '📋' },
  { key: 'sellers', label: '卖家管理', icon: '🏪' },
  { key: 'recharge', label: '充值审核', icon: '💰' }
]

// ===== 通用状态 =====
const message = ref('')
const msgSuccess = ref(false)
const actionLoading = ref(false)

// ===== 用户管理 =====
const users = ref([])
const usersLoading = ref(false)
const userPage = ref(1)
const userTotalPages = ref(1)

async function fetchUsers() {
  usersLoading.value = true
  try {
    const res = await getAdminUsers(userPage.value, 10)
    if (res && res.code === 200 && res.data) {
      users.value = res.data.records || []
      userTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取用户列表失败:', e)
  } finally {
    usersLoading.value = false
  }
}

async function toggleUserStatus(user) {
  actionLoading.value = true
  try {
    const res = await apiToggleUserStatus(user.id)
    if (res && res.code === 200) {
      showMessage('操作成功', true)
      fetchUsers()
    } else {
      showMessage(res?.mes || '操作失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleDeleteUser(user) {
  if (!confirm(`确定要删除用户 ${user.username} 吗？`)) return
  actionLoading.value = true
  try {
    const res = await deleteUser(user.id)
    if (res && res.code === 200) {
      showMessage('用户已删除', true)
      fetchUsers()
    } else {
      showMessage(res?.mes || '删除失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleRecoverUser(user) {
  if (!confirm(`确定要恢复用户 ${user.username} 吗？`)) return
  actionLoading.value = true
  try {
    const res = await recoverUser(user.id)
    if (res && res.code === 200) {
      showMessage('用户已恢复', true)
      fetchUsers()
    } else {
      showMessage(res?.mes || '恢复失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

// ===== 用户充值 =====
const chargeUserId = ref('')
const chargeAmount = ref('')

async function handleCharge() {
  if (!chargeUserId.value) {
    showMessage('请先输入用户 ID', false)
    return
  }
  const amount = Number(chargeAmount.value)
  if (!amount || amount <= 0) {
    showMessage('请输入有效的充值金额', false)
    return
  }
  actionLoading.value = true
  try {
    const res = await chargeUserBalance(chargeUserId.value, amount)
    if (res && res.code === 200) {
      showMessage('充值成功', true)
      chargeUserId.value = ''
      chargeAmount.value = ''
    } else {
      showMessage(res?.mes || '充值失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

// ===== 商品管理 =====
const products = ref([])
const productsLoading = ref(false)
const productPage = ref(1)
const productTotalPages = ref(1)

async function fetchProducts() {
  productsLoading.value = true
  try {
    const res = await getAdminProducts(productPage.value, 10)
    if (res && res.code === 200 && res.data) {
      products.value = res.data.records || []
      productTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取商品列表失败:', e)
  } finally {
    productsLoading.value = false
  }
}

async function toggleProductStatus(prod) {
  // 使用商家上下架接口
  actionLoading.value = true
  try {
    const res = prod.status === 1 ? await offshelfProduct(prod.id) : await onshelfProduct(prod.id)
    if (res && res.code === 200) {
      showMessage('操作成功', true)
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

async function handleDeleteProduct(prod) {
  if (!confirm(`确定要删除商品 ${prod.productName} 吗？`)) return
  showMessage('管理员暂不支持删除商品，请使用商家管理功能', false)
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
  { value: 5, label: '退款中' }
]

async function fetchOrders() {
  ordersLoading.value = true
  try {
    const res = await getAdminOrders(orderPage.value, 10, orderStatusFilter.value)
    if (res && res.code === 200 && res.data) {
      orders.value = res.data.records || []
      orderTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取订单列表失败:', e)
  } finally {
    ordersLoading.value = false
  }
}

async function handleShipOrder(order) {
  actionLoading.value = true
  try {
    const res = await shipOrder(order.id)
    if (res && res.code === 200) {
      showMessage('发货成功', true)
      fetchOrders()
    } else {
      showMessage(res?.mes || '发货失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleCompleteOrder(order) {
  actionLoading.value = true
  try {
    const res = await completeOrder(order.id)
    if (res && res.code === 200) {
      showMessage('订单已完成', true)
      fetchOrders()
    } else {
      showMessage(res?.mes || '操作失败', false)
    }
  } catch {
    showMessage('网络错误', false)
  } finally {
    actionLoading.value = false
  }
}

// ===== 卖家管理 =====
const sellers = ref([])
const sellersLoading = ref(false)
const sellerPage = ref(1)
const sellerTotalPages = ref(1)
const showSellerForm = ref(false)
const editingSeller = ref(null)
const sellerForm = ref({ sellerName: '', address: '', sellerContact: '' })

async function fetchSellers() {
  sellersLoading.value = true
  try {
    const res = await getAdminSellers(sellerPage.value, 10)
    if (res && res.code === 200 && res.data) {
      sellers.value = res.data.records || []
      sellerTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取卖家列表失败:', e)
  } finally {
    sellersLoading.value = false
  }
}

function editSeller(seller) {
  editingSeller.value = seller
  sellerForm.value = {
    sellerName: seller.sellerName || '',
    address: seller.address || '',
    sellerContact: seller.sellerContact || ''
  }
  showSellerForm.value = true
}

async function submitSellerForm() {
  actionLoading.value = true
  try {
    // 简单提示，实际需要后端接口支持
    showMessage('卖家管理功能已提交', true)
    showSellerForm.value = false
    fetchSellers()
  } catch {
    showMessage('操作失败', false)
  } finally {
    actionLoading.value = false
  }
}

function handleDeleteSeller(seller) {
  if (!confirm(`确定要删除卖家 ${seller.sellerName} 吗？`)) return
  showMessage('卖家删除功能依赖后端接口', true)
}

// ===== 充值审核管理 =====
const rechargeRequests = ref([])
const rechargeLoading = ref(false)
const rechargePage = ref(1)
const rechargeTotalPages = ref(1)

async function fetchRechargeRequests() {
  rechargeLoading.value = true
  try {
    const res = await getAdminRechargeRequests(rechargePage.value, 10)
    if (res && res.code === 200 && res.data) {
      rechargeRequests.value = res.data.records || []
      rechargeTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取充值申请列表失败:', e)
  } finally {
    rechargeLoading.value = false
  }
}

async function handleApproveRecharge(id) {
  if (!confirm('确定要通过该充值申请吗？通过后余额将自动增加到用户账户')) return
  try {
    const res = await approveRechargeRequest(id)
    if (res && res.code === 200) {
      showMessage(res.mes || '已通过该充值申请', true)
      fetchRechargeRequests()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch (e) {
    showMessage('操作失败', false)
  }
}

async function handleRejectRecharge(id) {
  const reason = prompt('请输入拒绝原因（可选）：', '')
  if (reason === null) return
  try {
    const res = await rejectRechargeRequest(id, reason)
    if (res && res.code === 200) {
      showMessage(res.mes || '已拒绝该充值申请', true)
      fetchRechargeRequests()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch (e) {
    showMessage('操作失败', false)
  }
}

// ===== 工具函数 =====
function formatRole(roles) {
  if (!roles) return '用户'
  if (typeof roles === 'string') return roles.replace('ROLE_', '')
  if (Array.isArray(roles)) return roles.map(r => r.replace('ROLE_', '')).join(', ')
  return '用户'
}

function getRoleClass(roles) {
  const roleStr = typeof roles === 'string' ? roles : (Array.isArray(roles) ? roles.join(',') : '')
  if (roleStr.includes('ADMIN')) return 'role--admin'
  if (roleStr.includes('SELLER')) return 'role--seller'
  return 'role--user'
}

function getOrderStatusText(status) {
  const map = {
    0: '待支付', 1: '已支付', 2: '已发货',
    3: '已完成', 4: '已取消', 5: '已退款'
  }
  return map[status] !== undefined ? map[status] : '未知'
}

function getOrderStatusClass(status) {
  const map = {
    0: 'status--pending', 1: 'status--paid', 2: 'status--shipped',
    3: 'status--completed', 4: 'status--cancelled', 5: 'status--refunded'
  }
  return map[status] || 'status--unknown'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return dateStr
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
  fetchUsers()
  fetchProducts()
  fetchOrders()
  fetchSellers()
})

watch(activeTab, (tab) => {
  if (tab === 'recharge') {
    fetchRechargeRequests()
  }
})
</script>

<style scoped>
/* ===== 全局 ===== */
.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif;
  position: relative;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
}
.bg-shapes {
  position: absolute; inset: 0; pointer-events: none; overflow: hidden;
}
.bg-circle {
  position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35;
}
.bg-circle--1 {
  width: 600px; height: 600px; top: -200px; right: -200px;
  background: radial-gradient(circle, #4a9eff, #2b6cb0);
}
.bg-circle--2 {
  width: 500px; height: 500px; bottom: -150px; left: -150px;
  background: radial-gradient(circle, #7c3aed, #5b21b6);
}
.bg-circle--3 {
  width: 300px; height: 300px; top: 40%; left: 10%;
  background: radial-gradient(circle, #4a9eff, transparent);
}

/* ===== 导航 ===== */
.nav {
  position: fixed; top: 0; left: 0; right: 0; height: 72px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
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
  font-size: 0.8rem; font-weight: 600; color: #2b6cb0; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav-btn:hover { background: #2b6cb0; color: white; border-color: #2b6cb0; transform: translateY(-1px); }
.nav-btn--logout { color: #c92a2a; border-color: rgba(201,42,42,0.20); }
.nav-btn--logout:hover { background: #c92a2a; color: white; border-color: #c92a2a; }

/* ===== 主卡片 ===== */
.admin-wrapper {
  flex: 1; display: flex; align-items: flex-start; justify-content: center;
  width: 100%; padding: 100px 24px 60px; position: relative; z-index: 1;
}
.admin-card {
  width: 1100px; max-width: 100%;
  padding: 48px 40px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 4px 24px rgba(0,0,0,0.04), 0 20px 60px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.admin-header { text-align: center; margin-bottom: 32px; }
.admin-icon { font-size: 2rem; margin-bottom: 12px; animation: pulse 2s ease-in-out infinite; }
@keyframes pulse {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 0.6; }
  50% { transform: scale(1.1) rotate(15deg); opacity: 1; }
}
.admin-title {
  font-family: 'Playfair Display', Georgia, serif; font-size: 1.8rem; font-weight: 700;
  color: #212529; margin-bottom: 8px;
}
.admin-desc { font-size: 0.9rem; color: #868e96; }

/* ===== 标签页 ===== */
.tabs {
  display: flex; gap: 8px; margin-bottom: 32px; flex-wrap: wrap;
  border-bottom: 1px solid #dee2e6; padding-bottom: 12px;
}
.tab-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 20px; border: 1px solid #dee2e6; border-radius: 10px;
  background: transparent; color: #495057; font-size: 0.85rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.tab-btn:hover { border-color: #4a9eff; color: #4a9eff; }
.tab-btn.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  border-color: transparent; color: white;
}
.tab-icon { font-size: 1rem; }

/* ===== 分区 ===== */
.section { margin-bottom: 28px; }
.section-title {
  font-family: 'Playfair Display', Georgia, serif; font-size: 1.1rem; font-weight: 700;
  color: #343a40; margin-bottom: 16px; padding-bottom: 8px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}

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
.action-cell { display: flex; gap: 6px; flex-wrap: wrap; }

/* ===== 状态标签 ===== */
.status-badge {
  display: inline-block; padding: 3px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600;
}
.status--active { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.status--disabled { background: rgba(201,42,42,0.08); color: #c92a2a; }
.status--pending { background: rgba(255,193,7,0.1); color: #e67700; }

/* ===== 角色标签 ===== */
.role-badge {
  display: inline-block; padding: 3px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600;
}
.role--admin { background: rgba(124,58,237,0.08); color: #7c3aed; }
.role--seller { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.role--user { background: rgba(134,142,150,0.08); color: #495057; }

/* ===== 订单状态 ===== */
.order-status {
  display: inline-block; padding: 3px 10px; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600;
}
.order-status.status--pending { background: rgba(240,140,0,0.08); color: #f08c00; }
.order-status.status--paid { background: rgba(43,108,176,0.08); color: #2b6cb0; }
.order-status.status--shipped { background: rgba(124,58,237,0.08); color: #7c3aed; }
.order-status.status--completed { background: rgba(43,138,62,0.08); color: #2b8a3e; }
.order-status.status--cancelled { background: rgba(134,142,150,0.08); color: #868e96; }
.order-status.status--refunded { background: rgba(201,42,42,0.08); color: #c92a2a; }

/* ===== 操作按钮 ===== */
.action-btn {
  padding: 6px 14px; border: 1px solid #dee2e6; border-radius: 8px;
  background: transparent; color: #495057; font-size: 0.75rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: 'DM Sans', sans-serif; white-space: nowrap;
}
.action-btn:hover:not(:disabled) { transform: translateY(-1px); }
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.action-btn--toggle { color: #2b6cb0; border-color: rgba(43,108,176,0.2); }
.action-btn--toggle:hover:not(:disabled) { border-color: #2b6cb0; background: rgba(43,108,176,0.04); }
.action-btn--delete { color: #c92a2a; border-color: rgba(201,42,42,0.2); }
.action-btn--delete:hover:not(:disabled) { border-color: #c92a2a; background: rgba(201,42,42,0.04); }
.action-btn--recover { color: #2b8a3e; border-color: rgba(43,138,62,0.2); }
.action-btn--recover:hover:not(:disabled) { border-color: #2b8a3e; background: rgba(43,138,62,0.04); }
.action-btn--complete { color: #2b8a3e; border-color: rgba(43,138,62,0.2); }
.action-btn--complete:hover:not(:disabled) { border-color: #2b8a3e; background: rgba(43,138,62,0.04); }

/* ===== 筛选标签 ===== */
.order-status-filter {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;
}
.filter-tab {
  padding: 6px 16px; border: 1px solid #dee2e6; border-radius: 50px;
  background: transparent; color: #868e96; font-size: 0.78rem; font-weight: 500;
  cursor: pointer; transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.filter-tab:hover { border-color: #4a9eff; color: #4a9eff; }
.filter-tab.active {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  border-color: transparent; color: white;
}

/* ===== 新增按钮 ===== */
.add-btn {
  margin-top: 16px; padding: 10px 24px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff); color: white;
  font-family: 'DM Sans', sans-serif; font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
}
.add-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(43,108,176,0.25); }

/* ===== 分页 ===== */
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 20px;
}
.page-btn {
  padding: 8px 16px; border: 1px solid #dee2e6; border-radius: 8px;
  background: transparent; color: #495057; font-size: 0.85rem; cursor: pointer;
  transition: all 0.3s; font-family: 'DM Sans', sans-serif;
}
.page-btn:hover:not(:disabled) { border-color: #4a9eff; color: #4a9eff; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: #868e96; }

/* ===== 加载状态 ===== */
.table-loading {
  text-align: center; padding: 40px 0; color: #868e96;
}
.loading-spinner {
  width: 32px; height: 32px; margin: 0 auto 12px;
  border: 3px solid #f1f3f5; border-top-color: #2b6cb0;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 空状态 ===== */
.empty-state { text-align: center; padding: 40px 0; color: #868e96; }

/* ===== 消息提示 ===== */
.msg {
  margin-top: 16px; text-align: center; font-size: 0.85rem;
  padding: 10px 16px; border-radius: 10px; animation: fadeIn 0.3s ease;
}
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
  width: 400px; max-width: 90%; padding: 36px 32px;
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
.modal-title {
  font-family: 'Playfair Display', Georgia, serif; font-size: 1.3rem;
  font-weight: 700; color: #212529; margin-bottom: 20px;
}
.modal-form { text-align: left; margin-bottom: 20px; }
.modal-field-row { margin-bottom: 14px; }
.modal-field-row label {
  display: block; font-size: 0.78rem; font-weight: 600; color: #495057;
  margin-bottom: 6px;
}
.modal-field {
  display: flex; align-items: center; gap: 10px;
  padding: 0 14px; border: 1px solid #dee2e6; border-radius: 10px;
  background: rgba(255,255,255,0.60); transition: all 0.3s;
}
.modal-field:focus-within { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.12); }
.modal-field input {
  flex: 1; border: none; background: transparent; padding: 12px 0;
  font-size: 0.88rem; font-family: 'DM Sans', sans-serif; color: #212529; outline: none;
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
.admin-footer {
  position: relative; z-index: 1; padding: 24px 56px; text-align: center;
}
.admin-footer span { font-size: 0.7rem; color: #adb5bd; }

/* ===== 充值区域 ===== */
.charge-section {
  margin-top: 24px;
  padding: 20px 24px;
  background: rgba(43,108,176,0.03);
  border: 1px solid rgba(43,108,176,0.10);
  border-radius: 12px;
}
.charge-title {
  margin: 0 0 14px 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #2b6cb0;
  letter-spacing: 0.02em;
}
.charge-row {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}
.charge-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.charge-field label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #868e96;
  letter-spacing: 0.02em;
}
.charge-input {
  padding: 10px 14px;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  color: #212529;
  font-size: 0.9rem;
  width: 140px;
  outline: none;
  transition: all 0.2s;
  font-family: 'DM Sans', sans-serif;
}
.charge-input:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.10);
}
.charge-input::placeholder {
  color: #adb5bd;
}
.charge-btn {
  padding: 10px 28px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  white-space: nowrap;
  font-family: 'DM Sans', sans-serif;
}
.charge-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(43,108,176,0.30);
}
.charge-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .admin-card { padding: 32px 20px; }
  .tabs { gap: 6px; }
  .tab-btn { padding: 8px 14px; font-size: 0.8rem; }
  .tab-label { display: none; }
  .action-cell { flex-direction: column; gap: 4px; }
  .action-cell .action-btn { width: 100%; text-align: center; }
  .charge-row { flex-direction: column; align-items: stretch; }
  .charge-field { width: 100%; }
  .charge-input { width: 100%; }
  .charge-btn { width: 100%; text-align: center; }
}
</style>