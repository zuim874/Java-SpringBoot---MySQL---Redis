import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/home', name: 'Home', component: HomeView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 新增：路由守卫
router.beforeEach((to, from, next) => {
  // 获取 Token
  const token = localStorage.getItem('token')

  // 如果要访问的页面需要登录（如 /home），但没有 Token
  if (to.name !== 'Login' && !token) {
    // 跳转到登录页
    next({ name: 'Login' })
  }
  // 如果已经登录，但要去登录页
  else if (to.name === 'Login' && token) {
    // 直接跳转到主页
    next({ name: 'Home' })
  }
  // 其他情况，正常跳转
  else {
    next()
  }
})

export default router