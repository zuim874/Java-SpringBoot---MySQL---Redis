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
 * @returns {Promise<{code, mes, data: {records, total, pages, current}}>}
 */
export function getProductPage(page = 1, size = 12, category = '') {
  const params = { page, size }
  if (category) params.category = category
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
  if (status) params.status = status
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
  if (status) params.status = status
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
 * 获取所有卖家列表
 * @returns {Promise}
 */
export function getAdminSellers() {
  return api.get('/admin/sellers')
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

export default api