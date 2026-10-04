import { createRouter, createWebHashHistory } from 'vue-router'

// 采用 hash 路由：构建版是纯静态托管，刷新页面不会 404
const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '访问登录' } },
  { path: '/home', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/elders', name: 'elders', component: () => import('../views/ElderView.vue'), meta: { title: '老人档案' } },
  { path: '/plans', name: 'plans', component: () => import('../views/PlanView.vue'), meta: { title: '用药计划' } },
  { path: '/checkin', name: 'checkin', component: () => import('../views/CheckinView.vue'), meta: { title: '今日打卡' } },
  { path: '/ocr', name: 'ocr', component: () => import('../views/OcrView.vue'), meta: { title: '拍照识药' } },
  { path: '/safety', name: 'safety', component: () => import('../views/SafetyView.vue'), meta: { title: '用药安全' } }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
