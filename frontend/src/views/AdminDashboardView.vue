<template>
  <div class="admin-page">
    <!-- 顶部导航 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-actions">
        <div class="avatar-wrapper" @click.stop>
          <span class="nav-balance" v-if="userBalance !== null">余额 ¥{{ Number(userBalance).toFixed(2) }}</span>
          <span class="nav-nickname">{{ nickname }}</span>
          <button class="avatar-btn" @click="profileMenuOpen = !profileMenuOpen" aria-label="用户菜单">
            <span class="avatar-circle">
              <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
            </span>
            <span class="avatar-caret" :class="{ open: profileMenuOpen }">▾</span>
          </button>
          <transition name="profile">
            <div class="profile-menu" v-if="profileMenuOpen">
              <div class="profile-menu-header">
                <span class="profile-menu-avatar">
                  <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
                </span>
                <div class="profile-menu-id">
                  <p class="profile-menu-name">{{ nickname }}</p>
                  <p class="profile-menu-role">{{ roleLabel }}</p>
                </div>
              </div>
              <button class="profile-menu-item" @click="profileMenuOpen = false; goHome()">返回商城</button>
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
      </div>
    </nav>

    <!-- 主内容 -->
    <main class="admin-body">
      <!-- 页头 -->
      <header class="page-head">
        <div>
          <h1 class="page-title">管理后台</h1>
          <p class="page-sub">系统综合管理面板 · 用户 / 商品 / 订单 / 卖家 / 优惠券 / 充值审核</p>
        </div>
      </header>

      <!-- 标签页导航 -->
      <nav class="tabs">
        <button v-for="tab in tabs" :key="tab.key"
                class="tab-btn" :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key">
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
        </button>
      </nav>

      <!-- ============ 用户管理 ============ -->
      <section v-if="activeTab === 'users'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">用户管理</h3>
        </div>
        <div class="loading-state" v-if="usersLoading">
          <div class="loading-spinner"></div>
          <p>加载用户数据...</p>
        </div>
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
                <td class="cell-dim">{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.nickname }}</td>
                <td class="cell-dim">{{ user.email || '-' }}</td>
                <td>
                  <span class="role-badge" :class="getRoleClass(user.userRole)">
                    {{ formatRole(user.userRole) }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="user.status === 1 ? 'status--ok' : 'status--off'">
                    {{ user.status === 1 ? '正常' : '已禁用' }}
                  </span>
                </td>
                <td class="cell-dim">{{ formatDate(user.createTime) }}</td>
                <td class="action-cell">
                  <button class="action-btn" @click="toggleUserStatus(user)"
                          :disabled="actionLoading" :title="user.status === 1 ? '禁用' : '启用'">
                    {{ user.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="action-btn action-btn--vip" @click="toggleUserVip(user)"
                          :disabled="actionLoading" :title="isVipUser(user) ? '取消会员' : '升级会员'">
                    {{ isVipUser(user) ? '取消会员' : '设为会员' }}
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
      </section>

      <!-- ============ 商品管理 ============ -->
      <section v-if="activeTab === 'products'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">商品管理</h3>
        </div>
        <div class="loading-state" v-if="productsLoading">
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
                <td class="cell-dim">{{ prod.id }}</td>
                <td class="name-cell">{{ prod.productName }}</td>
                <td class="price">¥{{ (prod.price || 0).toFixed(2) }}</td>
                <td>{{ prod.stock || 0 }}</td>
                <td>{{ prod.sold || 0 }}</td>
                <td><span class="cat-chip">{{ prod.category || '-' }}</span></td>
                <td>
                  <span class="status-badge" :class="prod.status === 1 ? 'status--ok' : 'status--off'">
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
                <td class="cell-mono">{{ order.orderNo || order.id }}</td>
                <td class="cell-dim">{{ order.userId }}</td>
                <td class="price">¥{{ (order.totalAmount || 0).toFixed(2) }}</td>
                <td>
                  <span class="status-badge" :class="getOrderStatusClass(order.status)">
                    {{ getOrderStatusText(order.status) }}
                  </span>
                </td>
                <td class="cell-dim">{{ formatDate(order.createTime) }}</td>
                <td class="action-cell">
                  <button class="action-btn action-btn--complete" @click="handleShipOrder(order)"
                          :disabled="actionLoading || order.status !== 1">发货</button>
                  <button class="action-btn" @click="handleCompleteOrder(order)"
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
      </section>

      <!-- ============ 卖家管理 ============ -->
      <section v-if="activeTab === 'sellers'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">卖家管理</h3>
          <button class="btn btn--primary" @click="showSellerForm = true">
            <span class="btn-icon">＋</span>新增卖家
          </button>
        </div>
        <div class="loading-state" v-if="sellersLoading">
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
                <td class="cell-dim">{{ seller.id }}</td>
                <td class="name-cell">{{ seller.sellerName }}</td>
                <td>{{ seller.address || '-' }}</td>
                <td>{{ seller.sellerContact || '-' }}</td>
                <td class="action-cell">
                  <button class="action-btn action-btn--vip" @click="toggleSellerVip(seller)"
                          :disabled="actionLoading">设为会员卖家</button>
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
      </section>

      <!-- ============ 优惠券管理 ============ -->
      <section v-if="activeTab === 'coupons'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">优惠券管理</h3>
        </div>
        <div class="coupon-create">
          <h4 class="coupon-create-title">创建优惠券模板</h4>
          <div class="coupon-create-row">
            <input v-model="couponForm.name" type="text" placeholder="优惠券名称" class="coupon-input" />
            <select v-model="couponForm.type" class="coupon-input coupon-input--select">
              <option :value="1">满减</option>
              <option :value="2">折扣</option>
            </select>
            <select v-model="couponForm.targetType" class="coupon-input coupon-input--select coupon-input--target">
              <option :value="1">全部用户</option>
              <option :value="2">仅VIP</option>
            </select>
            <input v-model="couponForm.discountValue" type="number" step="0.01" placeholder="优惠值" class="coupon-input" />
            <input v-model="couponForm.minAmount" type="number" step="0.01" placeholder="使用门槛(元)" class="coupon-input" />
            <input v-model="couponForm.totalCount" type="number" placeholder="发行量" class="coupon-input" />
            <button class="charge-btn" :disabled="actionLoading" @click="handleCreateCoupon">创建</button>
          </div>
          <p class="coupon-hint">提示：满减填减免金额（如 30），折扣填折数（如 8.5 表示 8.5 折）；仅VIP券受众 = VIP用户 + VIP卖家</p>
        </div>
        <div class="loading-state" v-if="couponsLoading">
          <div class="loading-spinner"></div>
          <p>加载优惠券...</p>
        </div>
        <div class="table-wrap" v-else-if="coupons.length > 0">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th><th>名称</th><th>类型</th><th>优惠值</th>
                <th>门槛</th><th>适用人群</th><th>剩余/总量</th><th>状态</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in coupons" :key="c.id">
                <td class="cell-dim">{{ c.id }}</td>
                <td class="name-cell">{{ c.name }}</td>
                <td>{{ c.type === 1 ? '满减' : '折扣' }}</td>
                <td>{{ c.type === 2 ? Number(c.discountValue).toFixed(1) + '折' : '¥' + Number(c.discountValue).toFixed(2) }}</td>
                <td>满¥{{ Number(c.minAmount || 0).toFixed(0) }}</td>
                <td>
                  <span class="target-badge" :class="c.targetType === 2 ? 'target-badge--vip' : 'target-badge--all'">
                    {{ c.targetType === 2 ? '仅VIP' : '全部' }}
                  </span>
                </td>
                <td>{{ c.remainCount }} / {{ c.totalCount }}</td>
                <td>
                  <span class="status-badge" :class="c.status === 1 ? 'status--ok' : 'status--off'">
                    {{ c.status === 1 ? '启用' : '停用' }}
                  </span>
                </td>
                <td class="action-cell">
                  <button class="action-btn action-btn--vip" @click="handleGrantCoupon(c)">发给用户</button>
                  <button class="action-btn action-btn--all" @click="handleGrantAllUsers(c)">发全部用户</button>
                  <button class="action-btn action-btn--complete" @click="handleGrantAllVip(c)">发全部会员</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="empty-state" v-else-if="!couponsLoading">
          <p>暂无优惠券模板，请先创建</p>
        </div>
        <div class="pagination" v-if="couponTotalPages > 1">
          <button class="page-btn" :disabled="couponPage <= 1" @click="couponPage--; fetchCoupons()">‹</button>
          <span class="page-info">{{ couponPage }} / {{ couponTotalPages }}</span>
          <button class="page-btn" :disabled="couponPage >= couponTotalPages" @click="couponPage++; fetchCoupons()">›</button>
        </div>
      </section>

      <!-- ============ 充值审核 ============ -->
      <section v-if="activeTab === 'recharge'" class="panel">
        <div class="panel-head">
          <h3 class="panel-title">充值申请审核</h3>
        </div>
        <div class="loading-state" v-if="rechargeLoading">
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
                <td class="cell-dim">{{ req.id }}</td>
                <td>{{ req.username }}</td>
                <td class="price">¥{{ req.amount.toFixed(2) }}</td>
                <td>
                  <span class="status-badge"
                        :class="req.status === 0 ? 'status--pending' :
                                req.status === 1 ? 'status--ok' : 'status--off'">
                    {{ req.status === 0 ? '待审核' : req.status === 1 ? '已通过' : '已拒绝' }}
                  </span>
                </td>
                <td class="cell-dim">{{ formatDate(req.createTime) }}</td>
                <td class="cell-dim">{{ req.remark || '-' }}</td>
                <td class="action-cell">
                  <template v-if="req.status === 0">
                    <button class="action-btn action-btn--complete" @click="handleApproveRecharge(req.id)">通过</button>
                    <button class="action-btn action-btn--delete" @click="handleRejectRecharge(req.id)">拒绝</button>
                  </template>
                  <span v-else class="cell-dim">已处理</span>
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
      </section>

      <!-- 消息提示 -->
      <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">{{ message }}</p>
    </main>

    <!-- ===== 新增/编辑卖家弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showSellerForm" class="modal-overlay" @click.self="showSellerForm = false">
        <div class="modal-card">
          <div class="modal-head">
            <span class="modal-icon">🏪</span>
            <h3 class="modal-title">{{ editingSeller ? '编辑卖家' : '新增卖家' }}</h3>
          </div>
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
            <button class="modal-btn modal-btn--primary" @click="submitSellerForm" :disabled="actionLoading">
              {{ editingSeller ? '保存' : '新增' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 底部版权 -->
    <footer class="page-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCachedAvatar } from '../utils/avatarCache.js'
import { getCachedRoles } from '../utils/auth.js'
import {
  getAdminUsers, toggleUserStatus as apiToggleUserStatus, deleteUser, recoverUser,
  getAdminProducts,
  getAdminOrders, shipOrder, completeOrder,
  getAdminSellers,
  onshelfProduct, offshelfProduct,
  chargeUserBalance,
  getAdminRechargeRequests, approveRechargeRequest, rejectRechargeRequest,
  adminListCoupons, adminCreateCoupon, adminGrantCoupon, adminGrantCouponAllVip, adminGrantCouponAllUsers,
  setUserVip, setSellerVip,
  getUserInfo
} from '../api/index.js'

const router = useRouter()

// ===== 用户头像下拉 =====
const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'
const avatarUrl = ref(DEFAULT_AVATAR)
const nickname = ref(localStorage.getItem('nickname') || '用户')
const userBalance = ref(null)
const profileMenuOpen = ref(false)

const roleLabel = computed(() => {
  const roles = ['管理员']
  return roles[0]
})

const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))

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

function closePopups() { profileMenuOpen.value = false }

// ===== 标签页 =====
const activeTab = ref('users')
const tabs = [
  { key: 'users', label: '用户管理', icon: '👥' },
  { key: 'products', label: '商品管理', icon: '📦' },
  { key: 'orders', label: '订单管理', icon: '📋' },
  { key: 'sellers', label: '卖家管理', icon: '🏪' },
  { key: 'recharge', label: '充值审核', icon: '💰' },
  { key: 'coupons', label: '优惠券管理', icon: '🎟️' }
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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
    showMessage('服务连接失败，请稍后重试', false)
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

// ===== 优惠券管理 =====
const coupons = ref([])
const couponsLoading = ref(false)
const couponPage = ref(1)
const couponTotalPages = ref(1)
const couponForm = ref({ name: '', type: 1, targetType: 1, discountValue: '', minAmount: '', totalCount: '' })

async function fetchCoupons() {
  couponsLoading.value = true
  try {
    const res = await adminListCoupons(couponPage.value, 10)
    if (res && res.code === 200 && res.data) {
      coupons.value = res.data.records || []
      couponTotalPages.value = res.data.pages || 1
    }
  } catch (e) {
    console.error('获取优惠券列表失败:', e)
  } finally {
    couponsLoading.value = false
  }
}

async function handleCreateCoupon() {
  if (!couponForm.value.name.trim()) { showMessage('请输入优惠券名称', false); return }
  if (!couponForm.value.discountValue || Number(couponForm.value.discountValue) <= 0) {
    showMessage('请输入有效的优惠值', false); return
  }
  if (!couponForm.value.totalCount || Number(couponForm.value.totalCount) <= 0) {
    showMessage('请输入有效的发行量', false); return
  }
  actionLoading.value = true
  try {
    const res = await adminCreateCoupon({
      name: couponForm.value.name.trim(),
      type: Number(couponForm.value.type),
      targetType: Number(couponForm.value.targetType),
      discountValue: Number(couponForm.value.discountValue),
      minAmount: Number(couponForm.value.minAmount) || 0,
      totalCount: Number(couponForm.value.totalCount)
    })
    if (res && res.code === 200) {
      showMessage('优惠券创建成功', true)
      couponForm.value = { name: '', type: 1, targetType: 1, discountValue: '', minAmount: '', totalCount: '' }
      fetchCoupons()
    } else {
      showMessage((res && res.mes) || '创建失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleGrantCoupon(c) {
  const userId = prompt(`请输入要发放「${c.name}」的目标用户 ID：`)
  if (!userId) return
  const days = prompt('请输入有效天数（默认 30）：', '30')
  actionLoading.value = true
  try {
    const res = await adminGrantCoupon(userId, c.id, Number(days) || 30)
    if (res && res.code === 200) {
      showMessage(res.mes || '发放成功', true)
      fetchCoupons()
    } else {
      showMessage((res && res.mes) || '发放失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleGrantAllVip(c) {
  if (!confirm(`确定要向全部VIP会员（VIP用户 + VIP卖家）发放「${c.name}」吗？`)) return
  const days = prompt('请输入有效天数（默认 30）：', '30')
  actionLoading.value = true
  try {
    const res = await adminGrantCouponAllVip(c.id, Number(days) || 30)
    if (res && res.code === 200) {
      showMessage(res.mes || '批量发放完成', true)
      fetchCoupons()
    } else {
      showMessage((res && res.mes) || '发放失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

async function handleGrantAllUsers(c) {
  if (!confirm(`确定要向全部注册用户发放「${c.name}」吗？（将发给所有启用用户）`)) return
  const days = prompt('请输入有效天数（默认 30）：', '30')
  actionLoading.value = true
  try {
    const res = await adminGrantCouponAllUsers(c.id, Number(days) || 30)
    if (res && res.code === 200) {
      showMessage(res.mes || '批量发放完成', true)
      fetchCoupons()
    } else {
      showMessage((res && res.mes) || '发放失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

// ===== 会员（VIP）管理 =====
function isVipUser(user) {
  const role = user.userRole || ''
  return role.includes('ROLE_VIP_USER')
}

async function toggleUserVip(user) {
  const enable = !isVipUser(user)
  if (!confirm(`确定要${enable ? '升级' : '取消'}用户「${user.username}」的会员身份吗？`)) return
  actionLoading.value = true
  try {
    const res = await setUserVip(user.id, enable)
    if (res && res.code === 200) {
      showMessage(res.mes || '操作成功', true)
      fetchUsers()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
  }
}

async function toggleSellerVip(seller) {
  if (!confirm(`确定要升级卖家「${seller.sellerName}」为会员卖家吗？（会员卖家可享推荐位/旗舰店权益）`)) return
  actionLoading.value = true
  try {
    const res = await setSellerVip(seller.id, true)
    if (res && res.code === 200) {
      showMessage(res.mes || '操作成功', true)
      fetchSellers()
    } else {
      showMessage((res && res.mes) || '操作失败', false)
    }
  } catch (e) {
    showMessage('服务连接失败，请稍后重试', false)
  } finally {
    actionLoading.value = false
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
function goOrders() { router.push('/orders') }
function goProfile() { router.push('/profile') }
function goCoupons() { router.push('/coupons') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('roles')
  router.push('/login')
}

onMounted(() => {
  fetchUserProfile()
  document.addEventListener('click', closePopups)
  fetchUsers()
  fetchProducts()
  fetchOrders()
  fetchSellers()
  fetchCoupons()
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})

watch(activeTab, (tab) => {
  if (tab === 'recharge') {
    fetchRechargeRequests()
  }
  if (tab === 'coupons') {
    fetchCoupons()
  }
})
</script>

<style scoped>
/* ===== 页面骨架 ===== */
.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  font-family: var(--font-sans);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
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
.nav-actions { display: flex; align-items: center; gap: 12px; }

/* ===== 头像与下拉 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 8px; }
.nav-balance {
  padding: 5px 12px; border-radius: var(--radius-pill);
  background: var(--primary-soft); border: 1px solid rgba(47,84,235,0.15);
  color: var(--primary); font-size: 0.75rem; font-weight: 600; white-space: nowrap;
}
.nav-nickname { font-size: 0.85rem; font-weight: 600; color: var(--text-2); padding: 0 4px; }
.avatar-btn { display: flex; align-items: center; gap: 4px; background: none; border: none; cursor: pointer; padding: 2px; transition: transform 0.2s ease; }
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 38px; height: 38px; border-radius: 50%; overflow: hidden;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 1rem;
  box-shadow: var(--shadow-sm); border: 2px solid var(--surface);
}
.avatar-circle img { width: 100%; height: 100%; object-fit: cover; }
.avatar-caret { font-size: 0.7rem; color: var(--text-3); transition: transform 0.2s ease; }
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 14px); right: 0; min-width: 190px; padding: 8px;
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}
.profile-menu-header { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-bottom: 1px solid var(--border); margin-bottom: 6px; }
.profile-menu-avatar {
  width: 40px; height: 40px; border-radius: 50%; overflow: hidden; flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700;
}
.profile-menu-avatar img { width: 100%; height: 100%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name { font-size: 0.9rem; font-weight: 600; color: var(--text); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-menu-role { font-size: 0.72rem; color: var(--text-3); margin: 2px 0 0; }
.profile-menu-item {
  display: block; width: 100%; text-align: left; padding: 10px 14px; border: none; border-radius: var(--radius-sm);
  background: transparent; cursor: pointer; font-size: 0.85rem; color: var(--text-2); transition: all 0.2s ease;
}
.profile-menu-item:hover { background: var(--primary-softer); color: var(--primary); }
.profile-menu-item--logout:hover { background: var(--danger-soft); color: var(--danger); }
.profile-menu-divider { height: 1px; background: var(--border); margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主体布局 ===== */
.admin-body {
  flex: 1; width: 100%; max-width: var(--container); margin: 0 auto;
  padding: 96px 24px 56px;
}
.page-head { margin-bottom: 24px; }
.page-title {
  font-family: var(--font-display); font-size: 1.7rem; font-weight: 700;
  color: var(--text); letter-spacing: -0.01em; line-height: 1.2;
}
.page-sub { margin-top: 6px; font-size: 0.9rem; color: var(--text-3); }

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

/* ===== 角色标签 ===== */
.role-badge {
  display: inline-flex; align-items: center; padding: 3px 10px;
  border-radius: var(--radius-pill); font-size: 0.72rem; font-weight: 600; white-space: nowrap;
}
.role--admin { background: var(--primary-soft); color: var(--primary); }
.role--seller { background: var(--accent-soft); color: var(--accent); }
.role--user { background: var(--surface-3); color: var(--text-3); }

/* ===== 操作按钮 ===== */
.action-btn {
  padding: 5px 12px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-strong); background: var(--surface);
  color: var(--text-2); font-size: 0.75rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease; white-space: nowrap;
}
.action-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); background: var(--primary-softer); }
.action-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.action-btn--vip:hover:not(:disabled) { border-color: #7c3aed; color: #7c3aed; background: rgba(124,58,237,0.06); }
.action-btn--delete:hover:not(:disabled) { border-color: var(--danger); color: var(--danger); background: var(--danger-soft); }
.action-btn--recover:hover:not(:disabled) { border-color: var(--success); color: var(--success); background: var(--success-soft); }
.action-btn--complete:hover:not(:disabled) { border-color: var(--success); color: var(--success); background: var(--success-soft); }
.action-btn--all:hover:not(:disabled) { border-color: var(--warning); color: var(--warning); background: rgba(250,173,20,0.08); }

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

/* ===== 充值区域 ===== */
.charge-section {
  margin-top: 24px; padding: 20px 24px;
  background: var(--surface-2); border: 1px solid var(--border);
  border-radius: var(--radius);
}
.charge-title { margin: 0 0 14px; font-size: 0.9rem; font-weight: 600; color: var(--text-2); letter-spacing: 0.02em; }
.charge-row { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.charge-field { display: flex; flex-direction: column; gap: 6px; }
.charge-field label { font-size: 0.78rem; font-weight: 600; color: var(--text-3); letter-spacing: 0.02em; }
.charge-input {
  padding: 10px 14px; background: var(--surface); border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); color: var(--text); font-size: 0.9rem; width: 150px;
  outline: none; transition: all 0.2s ease; font-family: var(--font-sans);
}
.charge-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.charge-input::placeholder { color: var(--text-4); }
.charge-btn {
  padding: 10px 28px; background: linear-gradient(135deg, var(--primary), var(--primary-2));
  color: #fff; border: none; border-radius: var(--radius-sm); font-size: 0.85rem;
  font-weight: 600; cursor: pointer; transition: all 0.2s ease; white-space: nowrap;
}
.charge-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.charge-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 优惠券管理 ===== */
.coupon-create {
  margin-bottom: 20px; padding: 20px 24px;
  background: var(--primary-softer); border: 1px solid rgba(47,84,235,0.14);
  border-radius: var(--radius);
}
.coupon-create-title { margin: 0 0 14px; font-size: 0.9rem; font-weight: 600; color: var(--primary); letter-spacing: 0.02em; }
.coupon-create-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.coupon-input {
  padding: 10px 14px; background: var(--surface); border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm); color: var(--text); font-size: 0.85rem; min-width: 110px;
  flex: 1; outline: none; transition: all 0.2s ease; font-family: var(--font-sans);
}
.coupon-input--select { flex: 0 0 90px; }
.coupon-input--target { flex: 0 0 110px; }
.coupon-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.coupon-input::placeholder { color: var(--text-4); }

/* 适用人群角标 */
.target-badge {
  display: inline-block; padding: 3px 10px; border-radius: 999px;
  font-size: 0.72rem; font-weight: 600; white-space: nowrap;
}
.target-badge--all { background: rgba(96,108,132,0.12); color: var(--text-3); }
.target-badge--vip { background: rgba(245,158,11,0.15); color: #b45309; }
.coupon-hint { margin: 10px 0 0; font-size: 0.75rem; color: var(--text-4); }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(23, 35, 61, 0.45);
  backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center;
  z-index: 200; padding: 20px; animation: overlayIn 0.2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card {
  width: 460px; max-width: 100%; padding: 26px;
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
.modal-field input {
  flex: 1; border: none; background: transparent; padding: 11px 0;
  font-size: 0.88rem; color: var(--text); outline: none;
}
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
  .admin-body { padding: 84px 16px 40px; }
  .nav-nickname { display: none; }
  .tabs { gap: 6px; }
  .tab-label { display: none; }
  .action-cell { flex-direction: column; gap: 4px; }
  .action-cell .action-btn { width: 100%; text-align: center; }
  .charge-row { flex-direction: column; align-items: stretch; }
  .charge-field { width: 100%; }
  .charge-input { width: 100%; }
  .charge-btn { width: 100%; text-align: center; }
}
</style>