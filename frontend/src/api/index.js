/**
 * API 服务模块
 * 集中管理所有后端 API 调用
 * 使用 axios 实例，自动携带 Token，统一错误处理
 */
import axios from 'axios'

// 创建 axios 实例，baseURL 为 /api（Vite 代理转发到后端）
const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ===== 请求拦截器：自动携带 Token =====
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 如果是 FormData，让浏览器自动设置 Content-Type（含 boundary）
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  error => Promise.reject(error)
)

// ===== 响应拦截器：统一处理 401 等错误 =====
api.interceptors.response.use(
  response => {
    const data = response.data
    // 后端返回的 code 为 401 表示未登录或 Token 过期
    if (data && data.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('nickname')
      localStorage.removeItem('role')
      window.location.href = '/login'
      return Promise.reject(new Error(data.mes || '登录已过期'))
    }
    return data
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('nickname')
        localStorage.removeItem('role')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// ==================== 商品相关 API ====================

/**
 * 获取商品分页列表
 * @param {number} page - 页码（从1开始）
 * @param {number} size - 每页数量
 * @param {string} category - 分类名称（可选）
 * @param {string} keyword - 搜索关键词（可选，模糊匹配商品名/描述）
 * @returns {Promise<{code, mes, data: {records, total, pages, current}}>}
 */
export function getProductPage(page = 1, size = 12, category = '', keyword = '') {
  const params = { page, size }
  if (category) params.category = category
  if (keyword) params.keyword = keyword
  return api.get('/product/page', { params })
}

/**
 * 获取商品分类列表
 * @returns {Promise<{code, mes, data: string[]}>}
 */
export function getCategories() {
  return api.get('/product/categories')
}

/**
 * 获取商品详情
 * @param {number|string} id - 商品ID
 * @returns {Promise<{code, mes, data}>}
 */
export function getProductDetail(id) {
  return api.get(`/product/detail/${id}`)
}

// ==================== 订单相关 API ====================

/**
 * 创建订单
 * @param {object} orderData - 订单数据
 * @returns {Promise}
 */
export function createOrder(orderData) {
  return api.post('/order/create', orderData)
}

/**
 * 获取当前用户的订单列表
 * @param {number} page - 页码
 * @param {number} size - 每页数量
 * @param {string} status - 订单状态筛选（可选）
 * @returns {Promise}
 */
export function getUserOrders(page = 1, size = 10, status = '') {
  const params = { page, size }
  // 注意：status 为 0（待支付）时不能省略，需用显式判空而非 if(status)
  if (status !== '' && status !== null && status !== undefined) {
    params.status = status
  }
  return api.get('/order/list', { params })
}

/**
 * 获取订单详情
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function getOrderDetail(id) {
  return api.get(`/order/detail/${id}`)
}

/**
 * 支付订单
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function payOrder(id) {
  return api.put(`/order/pay/${id}`)
}

/**
 * 取消订单
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function cancelOrder(id) {
  return api.put(`/order/cancel/${id}`)
}

/**
 * 申请退款
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function refundOrder(id) {
  return api.put(`/order/refund/${id}`)
}

// ==================== 管理员订单 API ====================

/**
 * 管理员获取所有订单列表
 * @param {number} page
 * @param {number} size
 * @param {string} status
 * @returns {Promise}
 */
export function getAdminOrders(page = 1, size = 10, status = '') {
  const params = { page, size }
  // 注意：status 为 0（待支付）时不能省略，需用显式判空而非 if(status)
  if (status !== '' && status !== null && status !== undefined) {
    params.status = status
  }
  return api.get('/order/admin/list', { params })
}

/**
 * 管理员发货
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function shipOrder(id) {
  return api.put(`/order/admin/ship/${id}`)
}

/**
 * 管理员完成订单
 * @param {number|string} id - 订单ID
 * @returns {Promise}
 */
export function completeOrder(id) {
  return api.put(`/order/admin/complete/${id}`)
}

// ==================== 管理员用户 API ====================

/**
 * 获取所有用户列表
 * @param {number} page
 * @param {number} size
 * @returns {Promise}
 */
export function getAdminUsers(page = 1, size = 10) {
  return api.get('/admin/users', { params: { page, size } })
}

/**
 * 启用/禁用用户
 * @param {number|string} id - 用户ID
 * @returns {Promise}
 */
export function toggleUserStatus(id) {
  return api.put(`/admin/user/status/${id}`)
}

/**
 * 删除用户
 * @param {number|string} id - 用户ID
 * @returns {Promise}
 */
export function deleteUser(id) {
  return api.delete(`/admin/user/${id}`)
}

/**
 * 恢复用户
 * @param {number|string} id - 用户ID
 * @returns {Promise}
 */
export function recoverUser(id) {
  return api.put(`/admin/user/recover/${id}`)
}

/**
 * 管理员为用户充值（模拟货币，增加余额）
 * @param {number|string} userId - 用户ID
 * @param {number} amount - 充值金额（元）
 * @returns {Promise}
 */
export function chargeUserBalance(userId, amount) {
  return api.post('/admin/user/charge', null, { params: { userId, amount } })
}

/**
 * 用户申请充值（给自己充值，等待管理员处理）
 * @param {number} amount - 申请充值金额
 * @returns {Promise}
 */
export function applyRecharge(amount) {
  return api.post('/user/recharge/apply', null, { params: { amount } })
}
// ==================== 用户地址 API ====================

/**
 * 获取当前用户的所有收货地址
 * @returns {Promise}
 */
export function getUserAddresses() {
  return api.get('/user/address/list')
}

/**
 * 获取默认地址
 * @returns {Promise}
 */
export function getDefaultAddress() {
  return api.get('/user/address/default')
}

/**
 * 新增收货地址
 * @param {object} address - 地址信息 { receiverName, receiverPhone, receiverAddress, isDefault }
 * @returns {Promise}
 */
export function addAddress(address) {
  return api.post('/user/address/add', address)
}

/**
 * 更新收货地址
 * @param {object} address - 地址信息 { id, receiverName, receiverPhone, receiverAddress, isDefault }
 * @returns {Promise}
 */
export function updateAddress(address) {
  return api.put('/user/address/update', address)
}

/**
 * 删除收货地址
 * @param {number} id - 地址ID
 * @returns {Promise}
 */
export function deleteAddress(id) {
  return api.delete(`/user/address/delete/${id}`)
}

/**
 * 设置默认地址
 * @param {number} id - 地址ID
 * @returns {Promise}
 */
export function setDefaultAddress(id) {
  return api.put(`/user/address/default/${id}`)
}
// ==================== 用户充值申请 API ====================

/**
 * 用户提交充值申请
 * @param {number} amount - 充值金额
 * @returns {Promise}
 */
export function submitRecharge(amount) {
  return api.post('/user/recharge/apply', null, { params: { amount } })
}

/**
 * 查询当前用户的充值申请记录
 * @returns {Promise}
 */
export function getRechargeRecords() {
  return api.get('/user/recharge/records')
}

/**
 * 管理员获取充值申请列表（分页）
 * @param {number} page - 页码
 * @param {number} size - 每页条数
 * @returns {Promise}
 */
export function getAdminRechargeRequests(page, size) {
  return api.get('/admin/recharge-requests', { params: { page, size } })
}

/**
 * 管理员审核通过充值申请
 * @param {number} id - 申请ID
 * @returns {Promise}
 */
export function approveRechargeRequest(id) {
  return api.post(`/admin/recharge-requests/${id}/approve`)
}

/**
 * 管理员拒绝充值申请
 * @param {number} id - 申请ID
 * @param {string} reason - 拒绝原因（可选）
 * @returns {Promise}
 */
export function rejectRechargeRequest(id, reason) {
  return api.post(`/admin/recharge-requests/${id}/reject`, null, { params: { reason } })
}
// ==================== 管理员商品 API ====================

/**
 * 管理员获取所有商品列表
 * @param {number} page
 * @param {number} size
 * @returns {Promise}
 */
export function getAdminProducts(page = 1, size = 10) {
  return api.get('/admin/products', { params: { page, size } })
}

// ==================== 管理员卖家 API ====================

/**
 * 获取所有卖家列表（分页）
 * @param {number} page
 * @param {number} size
 * @returns {Promise}
 */
export function getAdminSellers(page = 1, size = 10) {
  return api.get('/admin/sellers', { params: { page, size } })
}

// ==================== 商家商品 API ====================

/**
 * 商家获取自己的商品列表
 * @param {number} page
 * @param {number} size
 * @returns {Promise}
 */
export function getSellerProducts(page = 1, size = 10) {
  return api.get('/product/seller/products', { params: { page, size } })
}

/**
 * 商家新增商品
 * @param {object} productData - 商品数据
 * @returns {Promise}
 */
export function addSellerProduct(productData) {
  return api.post('/product/seller/add', productData)
}

/**
 * 商家更新商品
 * @param {number|string} id - 商品ID
 * @param {object} productData - 商品数据
 * @returns {Promise}
 */
export function updateSellerProduct(id, productData) {
  return api.put(`/product/seller/update/${id}`, productData)
}

/**
 * 商家上架商品
 * @param {number|string} id - 商品ID
 * @returns {Promise}
 */
export function onshelfProduct(id) {
  return api.put(`/product/seller/onshelf/${id}`)
}

/**
 * 商家下架商品
 * @param {number|string} id - 商品ID
 * @returns {Promise}
 */
export function offshelfProduct(id) {
  return api.put(`/product/seller/offshelf/${id}`)
}

// ==================== 用户相关 API ====================

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
export function getUserInfo() {
  return api.get('/user/me')
}

// ==================== 优惠券 API ====================

/**
 * 获取当前用户可用优惠券
 * @returns {Promise}
 */
export function getAvailableCoupons() {
  return api.get('/coupon/user/available')
}

/**
 * 获取当前用户全部优惠券（含已用/已过期）
 * @returns {Promise}
 */
export function getAllCoupons() {
  return api.get('/coupon/user/all')
}

/**
 * 管理员创建优惠券模板
 * @param {object} coupon - { name, type, discountValue, minAmount, totalCount }
 * @returns {Promise}
 */
export function adminCreateCoupon(coupon) {
  return api.post('/coupon/admin/create', coupon)
}

/**
 * 管理员分页查询优惠券模板
 * @param {number} page
 * @param {number} size
 * @returns {Promise}
 */
export function adminListCoupons(page = 1, size = 10) {
  return api.get('/coupon/admin/list', { params: { page, size } })
}

/**
 * 管理员向指定用户发放优惠券
 * @param {number} userId
 * @param {number} couponId
 * @param {number} expireDays
 * @returns {Promise}
 */
export function adminGrantCoupon(userId, couponId, expireDays = 30) {
  return api.post('/coupon/admin/grant', null, { params: { userId, couponId, expireDays } })
}

/**
 * 管理员向全部会员买家批量发放优惠券
 * @param {number} couponId
 * @param {number} expireDays
 * @returns {Promise}
 */
export function adminGrantCouponAllVip(couponId, expireDays = 30) {
  return api.post('/coupon/admin/grant-all-vip', null, { params: { couponId, expireDays } })
}

// ==================== 买家聊天 API ====================

/**
 * 买家获取或创建与卖家的会话
 * @param {number} sellerId
 * @returns {Promise}
 */
export function userOpenChat(sellerId) {
  return api.post('/chat/user/open', null, { params: { sellerId } })
}

/**
 * 获取买家的会话列表
 * @returns {Promise}
 */
export function getUserConversations() {
  return api.get('/chat/user/conversations')
}

/**
 * 买家发送消息
 * @param {number} sellerId
 * @param {string} content
 * @returns {Promise}
 */
export function sendUserMessage(sellerId, content) {
  return api.post('/chat/user/send', null, { params: { sellerId, content } })
}

/**
 * 买家拉取会话消息（支持增量）
 * @param {number} conversationId
 * @param {number} afterId - 增量起点（可选）
 * @returns {Promise}
 */
export function getUserMessages(conversationId, afterId) {
  const params = { conversationId }
  if (afterId) params.afterId = afterId
  return api.get('/chat/user/messages', { params })
}

/**
 * 买家标记会话已读
 * @param {number} conversationId
 * @returns {Promise}
 */
export function markUserConversationRead(conversationId) {
  return api.post('/chat/user/read', null, { params: { conversationId } })
}

// ==================== 卖家聊天 API ====================

/**
 * 获取卖家的会话列表
 * @returns {Promise}
 */
export function getSellerConversations() {
  return api.get('/chat/seller/conversations')
}

/**
 * 卖家发送消息
 * @param {number} userId
 * @param {string} content
 * @returns {Promise}
 */
export function sendSellerMessage(userId, content) {
  return api.post('/chat/seller/send', null, { params: { userId, content } })
}

/**
 * 卖家拉取会话消息（支持增量）
 * @param {number} conversationId
 * @param {number} afterId
 * @returns {Promise}
 */
export function getSellerMessages(conversationId, afterId) {
  const params = { conversationId }
  if (afterId) params.afterId = afterId
  return api.get('/chat/seller/messages', { params })
}

/**
 * 卖家标记会话已读
 * @param {number} conversationId
 * @returns {Promise}
 */
export function markSellerConversationRead(conversationId) {
  return api.post('/chat/seller/read', null, { params: { conversationId } })
}

// ==================== 卖家订单与店铺 API ====================

/**
 * 获取卖家店铺信息
 * @returns {Promise}
 */
export function getSellerShop() {
  return api.get('/seller/shop')
}

/**
 * 更新卖家店铺信息
 * @param {object} seller - { sellerName, address, sellerContact, sellerAvatar }
 * @returns {Promise}
 */
export function updateSellerShop(seller) {
  return api.put('/seller/shop/update', seller)
}

/**
 * 卖家分页查询订单
 * @param {number} page
 * @param {number} size
 * @param {string} status - 订单状态筛选（可选）
 * @returns {Promise}
 */
export function getSellerOrders(page = 1, size = 10, status = '') {
  const params = { page, size }
  if (status !== '' && status !== null && status !== undefined) {
    params.status = status
  }
  return api.get('/seller/orders', { params })
}

/**
 * 卖家查询订单详情
 * @param {number} id
 * @returns {Promise}
 */
export function getSellerOrderDetail(id) {
  return api.get(`/seller/order/${id}`)
}

/**
 * 卖家发货
 * @param {number} id
 * @returns {Promise}
 */
export function sellerShipOrder(id) {
  return api.put(`/seller/order/ship/${id}`)
}

/**
 * 卖家设置商品推荐位（会员卖家权益）
 * @param {number} id
 * @param {number} recommend - 1推荐 0取消
 * @returns {Promise}
 */
export function sellerSetRecommend(id, recommend) {
  return api.put(`/product/seller/recommend/${id}`, null, { params: { recommend } })
}

// ==================== 管理员 VIP 管理 API ====================

/**
 * 管理员升级/降级买家为会员买家
 * @param {number} id - 用户ID
 * @param {boolean} enable
 * @returns {Promise}
 */
export function setUserVip(id, enable) {
  return api.put(`/admin/user/vip/${id}`, null, { params: { enable } })
}

/**
 * 管理员升级/降级卖家为会员卖家
 * @param {number} id - 卖家ID
 * @param {boolean} enable
 * @returns {Promise}
 */
export function setSellerVip(id, enable) {
  return api.put(`/admin/seller/vip/${id}`, null, { params: { enable } })
}

export default api