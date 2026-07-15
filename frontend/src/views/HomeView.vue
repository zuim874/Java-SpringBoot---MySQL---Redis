<template>
  <div class="home-page">
    <div class="home-box">
      <h1>欢迎回来！</h1>
      <p>您已成功登录系统。</p>
      <button @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function handleLogout() {
  router.push('/login')
}
</script>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.home-box {
  text-align: center;
  padding: 60px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
h1 {
  color: #333;
  margin-bottom: 12px;
}
p {
  color: #666;
  margin-bottom: 24px;
}
button {
  padding: 10px 24px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}
button:hover {
  background: #5568d3;
}
</style>
