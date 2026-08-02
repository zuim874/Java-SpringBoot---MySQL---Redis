import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from "../utils/token.js"
import { clearUserData } from "../utils/tokenChecker.js";
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import RecoverAccountView from '../views/RecoverAccountView.vue'
import AdminUserManageView from '../views/AdminUserManageView.vue'

// 路由规则数组
const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/home', name: 'Home', component: HomeView },
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

  // 不需要登录的页面
  const publicPages = ['Login', 'Register', 'Recover']

  // 如果要访问的页面需要登录，但没有 Token
  if (!publicPages.includes(to.name) && !token) {
    next({ name: 'Login' })
  }
  // 如果 Token 已过期，清空登录状态
  else if (!publicPages.includes(to.name) && isTokenExpired(token)) {
    clearUserData()
    next({ name: 'Login' })
  }
  // 如果已经登录，但要去登录页/注册页，跳转到主页
  else if (publicPages.includes(to.name) && token) {
    next({ name: 'Home' })
  }
  // 其他情况，正常跳转
  else {
    next()
  }
})

export default router