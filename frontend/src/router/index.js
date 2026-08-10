import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from "../utils/token.js"
import { clearUserData } from "../utils/tokenChecker.js";
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import RecoverAccountView from '../views/RecoverAccountView.vue'
import AdminUserManageView from '../views/AdminUserManageView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'

// 路由规则数组
const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/home', name: 'Home', component: HomeView },
  { path: '/product/:id', name: 'ProductDetail', component: ProductDetailView },
  { path: '/profile', name: 'Profile', component: ProfileView },
  { path: '/recover', name: 'Recover', component: RecoverAccountView },
  { path: '/admin/users', name: 'AdminUsers', component: AdminUserManageView }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（检查登录状态和 Token 有效性）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 公开页面（不需要登录）
  const publicPages = ['Login', 'Register', 'Recover', 'Home', 'ProductDetail']
  const isPublicPage = publicPages.includes(to.name)
  const isRecoverPage = to.name === 'Recover'

  // --- 公开页面 ---
  if (isPublicPage) {
    // Recover 页面：无论是否登录、token 是否过期，一律放行
    // 场景：账号被删除后 token 未清，用户需要访问恢复页面
    if (isRecoverPage) {
      next()
      return
    }
    // Login / Register / Home / ProductDetail：已登录则跳转主页，未登录则正常访问
    if ((to.name === 'Login' || to.name === 'Register') && token && !isTokenExpired(token)) {
      next({ name: 'Home' })
    } else {
      next()
    }
    return
  }

  // --- 非公开页面（需要登录） ---
  if (!token) {
    next({ name: 'Login' })
    return
  }
  if (isTokenExpired(token)) {
    clearUserData()
    next({ name: 'Login' })
    return
  }

  next()
})

export default router