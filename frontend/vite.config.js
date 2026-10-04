import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 用相对路径，构建产物既能放在后端根路径，也能放在任意子路径
  base: './',
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发态把 /api 代理到后端，前端不需要处理跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    // 构建产物直接输出到后端静态资源目录，实现"只启动后端即可演示"
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
    // Element Plus 整包引入约 1MB（gzip 后约 380KB），本地演示可接受；
    // 拆分出独立的 vendor chunk，避免与业务代码混在一个文件里
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'axios'],
          'element-plus': ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})
