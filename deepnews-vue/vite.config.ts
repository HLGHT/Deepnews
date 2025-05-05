import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'


// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      // 代理所有以 /api 开头的请求
      '/api': {
        target: 'http://localhost:8080', // 后端服务器地址
        changeOrigin: true, // 需要虚拟托管站点
      },
      // 单独为 SSE 流配置代理
      '/deep': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true // 如果需要 WebSocket 支持
      }
    }
  },
  // 生产环境配置
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})