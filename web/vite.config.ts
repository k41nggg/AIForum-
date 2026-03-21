import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 通过 Vite 代理把前端 /api 请求转发到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5177,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
