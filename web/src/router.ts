import { createRouter, createWebHistory } from 'vue-router'
import PostsPage from './views/PostsPage.vue'
import PostDetailPage from './views/PostDetailPage.vue'
import LoginPage from './views/LoginPage.vue'
import RegisterPage from './views/RegisterPage.vue'
import CategoriesPage from './views/CategoriesPage.vue'
import AuditPage from './views/AuditPage.vue'
import ProfilePage from './views/ProfilePage.vue'
import SubscriptionsPage from './views/SubscriptionsPage.vue'
import { getToken } from './lib/api'
import { showToast } from './lib/toast'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PostsPage },
    { path: '/posts/:id', component: PostDetailPage },
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/categories', component: CategoriesPage },
    { path: '/subscriptions', component: SubscriptionsPage, meta: { requiresAuth: true } },
    { path: '/profile', component: ProfilePage, meta: { requiresAuth: true } },
    { path: '/audit', component: AuditPage, meta: { requiresAuth: true } }
  ]
})

router.beforeEach((to) => {
  const requiresAuth = Boolean(to.meta?.requiresAuth)
  if (requiresAuth && !getToken()) {
    showToast('error', '需要登录', '请先登录后再继续')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(window as any).__AUTH_CHANGED__ = () => {
  router.replace(router.currentRoute.value.fullPath)
}
