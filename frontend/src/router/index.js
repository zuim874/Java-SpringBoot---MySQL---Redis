/**
 * 路由配置文件
 * 所有页面路由集中管理，包含登录检查和角色权限守卫
 * 关键：权限守卫必须从后端 API 获取用户角色，而非仅依赖前端本地存储
 */
import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from "../utils/token.js"
import { clearUserData } from "../utils/tokenChecker.js"
import { fetchUserRoles, ROLES, clearRoles } from "../utils/auth.js"
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import RecoverAccountView from '../views/RecoverAccountView.vue'
import AdminUserManageView from '../views/AdminUserManageView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import OrderListView from '../views/OrderListView.vue'
import OrderDetailView from '../views/OrderDetailView.vue'
import SellerProductManageView from '../views/SellerProductManageView.vue'
import MyCouponsView from '../views/MyCouponsView.vue'
import SellerDashboardView from '../views/SellerDashboardView.vue'

// 路由规则数组
const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/home', name: 'Home', component: HomeView },
  // 商品详情（公开页面）
  { path: '/product/:id', name: 'ProductDetail', component: ProductDetailView },
  // 个人中心（需登录）
  { path: '/profile', name: 'Profile', component: ProfileView },
  // 找回账号（公开页面）
  { path: '/recover', name: 'Recover', component: RecoverAccountView },
  // 旧版管理员用户管理（保留兼容）
  { path: '/admin/users', name: 'AdminUsers', component: AdminUserManageView },
  // ===== 新路由 =====
  // 管理员综合面板（需管理员角色）
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: AdminDashboardView,
    meta: { requiresAuth: true, requiredRoles: [ROLES.ADMIN] }
  },
  // 用户订单列表（需登录）
  {
    path: '/orders',
    name: 'OrderList',
    component: OrderListView,
    meta: { requiresAuth: true }
  },
  // 用户订单详情（需登录）
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: OrderDetailView,
    meta: { requiresAuth: true }
  },
  // 商家商品管理（需商家或管理员角色）
  {
    path: '/seller/products',
    name: 'SellerProducts',
    component: SellerProductManageView,
    meta: { requiresAuth: true, requiredRoles: [ROLES.SELLER, ROLES.ADMIN] }
  },
  // 商家综合面板（订单管理+店铺+消息）
  {
    path: '/seller/dashboard',
    name: 'SellerDashboard',
    component: SellerDashboardView,
    meta: { requiresAuth: true, requiredRoles: [ROLES.SELLER, ROLES.ADMIN] }
  },
  // 我的优惠券（需登录）
  {
    path: '/coupons',
    name: 'MyCoupons',
    component: MyCouponsView,
    meta: { requiresAuth: true }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 公开页面列表（不需要登录即可访问）
const publicPages = ['Login', 'Register', 'Recover', 'Home', 'ProductDetail']

/**
 * 路由守卫
 * 1. 检查登录状态和 Token 有效性
 * 2. 检查页面需要的角色权限（从后端 API 获取最新角色）
 */
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')

  // ===== 公开页面处理 =====
  const isPublicPage = publicPages.includes(to.name)
  const isRecoverPage = to.name === 'Recover'

  if (isPublicPage) {
    if (isRecoverPage) {
      next()
      return
    }
    // Login / Register：已登录则跳转主页
    if ((to.name === 'Login' || to.name === 'Register') && token && !isTokenExpired(token)) {
      next({ name: 'Home' })
    } else {
      next()
    }
    return
  }

  // ===== 非公开页面（需要登录） =====
  if (!token) {
    next({ name: 'Login' })
    return
  }
  if (isTokenExpired(token)) {
    clearUserData()
    clearRoles()
    next({ name: 'Login' })
    return
  }

  // ===== 角色权限检查 =====
  // 如果页面需要特定角色，从后端 API 获取最新角色信息进行校验
  const requiredRoles = to.meta.requiredRoles
  if (requiredRoles && requiredRoles.length > 0) {
    try {
      const userRoles = await fetchUserRoles()
      // 检查用户是否拥有所需的任一角色
      const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role))
      if (!hasRequiredRole) {
        // 无权限，跳转到首页并提示
        console.warn('权限不足：需要角色', requiredRoles, '，当前角色', userRoles)
        next({ name: 'Home' })
        return
      }
    } catch (e) {
      console.error('角色检查失败:', e)
      // 角色检查失败时，不允许访问需要权限的页面
      next({ name: 'Home' })
      return
    }
  }

  next()
})

export default router