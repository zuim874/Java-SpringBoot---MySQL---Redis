import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    },
    // 允许所有主机访问（开发环境）
    allowedHosts: ['frp-bar.com', '.frp-bar.com'],
  },
  https: false
})
