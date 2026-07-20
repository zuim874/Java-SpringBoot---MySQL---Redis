import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from "../utils/token.js"
import { clearUserData } from "../utils/tokenChecker.js";
// import { 函数名 } from "address";  => 导入其他文件的函数
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
// import 导入 页面组件

// 路由规则数组：定义浏览器地址烂路径 和 页面组件 的 对应关系
const routes = [
  { path: '/', redirect: '/login' },  // 根路径'/' 重定向至'/login'
  { path: '/login', name: 'Login', component: LoginView },  // 浏览器地址：'/login'，匹配成功后，页面渲染LoginView，Login是路由命名
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/home', name: 'Home', component: HomeView }
]

/**
 * const：JavaScript声明变量的关键字，不允许重新赋值，但是对象内部属性可以修改
 */

// VueRouter提供的createRouter()函数，创建一个 路由实例对象
// router：整个项目的路由管理器，负责：页面跳转、地址监听、路由守卫、匹配路径渲染组件
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * router.beforeEach:全局前置路由守卫
 * 每次页面跳转之前，都会自动执行这个函数
 * 用来做登录鉴权、权限拦截、Token 校验
 */

// 路由守卫(切换页面时触发token校验)
router.beforeEach((to, from, next) => {
  // 获取 Token
  const token = localStorage.getItem('token')

  // 如果要访问的页面需要登录（如 /home），但没有 Token
  if (to.name !== 'Login' && to.name !== 'Register' && !token) {
    // 跳转到登录页
    next({ name: 'Login' })
  }
  // 如果token过期，清空登录状态
  else if (to.name !== 'Login' && to.name !== 'Register' && isTokenExpired(token)) {
    clearUserData()
    next({ name: 'Login'})
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

// 导出路由示例
export default router