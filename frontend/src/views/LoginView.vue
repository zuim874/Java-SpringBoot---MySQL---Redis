<template>
  <div class="login-page">
    <div class="login-box">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="input-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" required />
        </div>
        <div class="input-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <button type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p v-if="message" :class="['msg', success ? 'success' : 'error']">{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const loading = ref(false)
const message = ref('')
const success = ref(false)
const router = useRouter()

async function handleLogin() {
  loading.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('username', username.value)
    params.append('password', password.value)

    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    })
    const data = await res.json()
    if (data.code === 200) {
      success.value = true
      message.value = data.mes
      setTimeout(() => {
        router.push('/home')
      }, 800)
    } else {
      success.value = false
      message.value = data.mes || '登录失败'
    }
  } catch (err) {
    success.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  width: 360px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
}
h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
}
.input-group {
  margin-bottom: 18px;
}
.input-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}
.input-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.input-group input:focus {
  outline: none;
  border-color: #667eea;
}
button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}
button:hover:not(:disabled) {
  background: #5568d3;
}
button:disabled {
  background: #a0a0a0;
  cursor: not-allowed;
}
.msg {
  margin-top: 14px;
  text-align: center;
  font-size: 14px;
}
.success {
  color: #28a745;
}
.error {
  color: #dc3545;
}
</style>
