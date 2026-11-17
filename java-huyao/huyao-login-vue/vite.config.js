import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：启用 Vue 插件
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0', // 同时监听 IPv4/IPv6，避免 127.0.0.1 打不开
    port: 5173,
    open: true, // 启动时自动打开浏览器
    proxy: {
      // 将 /api 开头的请求代理到后端 Spring Boot（8081 端口）
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
