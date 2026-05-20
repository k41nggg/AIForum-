<template>
  <div class="app-layout">
    <aside class="sidebar glass">
      <div class="sidebar-inner">
        <div class="brand" @click="$router.push('/')">
          <div class="logo">A</div>
          <div>
            <div class="brand-name">AIForum</div>
            <div class="subtle">一个简洁的论坛应用</div>
          </div>
        </div>

        <nav class="nav">
          <div class="nav-section">
            <div class="nav-title">用户</div>
            <RouterLink class="nav-link" to="/posts">帖子</RouterLink>
            <RouterLink v-if="me" class="nav-link" to="/recommendations">推荐</RouterLink>
            <RouterLink v-if="me" class="nav-link nav-link-messages" to="/messages">
              消息
              <span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </RouterLink>
            <RouterLink v-if="me" class="nav-link" to="/profile">个人中心</RouterLink>
          </div>

          <div v-if="isAdmin" class="nav-section">
            <div class="nav-title">管理员</div>
            <RouterLink class="nav-link" to="/audit">内容审核</RouterLink>
            <RouterLink class="nav-link" to="/categories">分类管理</RouterLink>
          </div>
        </nav>

        <div class="auth">
          <template v-if="me">
            <span class="pill user-pill">
              <span class="dot" />
              {{ me.nickname || me.username }}
              <span class="role" v-if="me.role">{{ me.role }}</span>
            </span>
            <button class="btn" @click="logout">退出</button>
          </template>
          <template v-else>
            <RouterLink class="btn" to="/login">登录</RouterLink>
            <RouterLink class="btn btn-primary" to="/register">注册</RouterLink>
          </template>
        </div>
      </div>
    </aside>

    <div class="main-content container">
      <RouterView />
    </div>

    <div v-if="toastState.show" class="toast" role="status" aria-live="polite">
      <div class="toast-title">{{ toastTitle }}</div>
      <div class="toast-msg">{{ toastState.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, clearToken, messages, type ApiResult } from './lib/api'
import { toast as toastState, showToast } from './lib/toast'

type User = {
  id: number
  username: string
  nickname?: string
  role?: string
}

const router = useRouter()
const me = ref<User | null>(null)
const unreadCount = ref(0)
let unreadTimer: ReturnType<typeof setInterval> | null = null

const isAdmin = computed(() => (me.value?.role || '').toUpperCase() === 'ADMIN')

async function loadUnread() {
  if (!me.value) {
    unreadCount.value = 0
    return
  }
  const res = await messages.unreadCount()
  if (res?.code === 200 && res.data) unreadCount.value = res.data.count ?? 0
}

async function loadMe() {
  const res = await apiGet<ApiResult<User>>('/users/me')
  if (res?.code === 200) {
    me.value = res.data
    await loadUnread()
    if (unreadTimer) clearInterval(unreadTimer)
    unreadTimer = setInterval(loadUnread, 60000)
  } else {
    me.value = null
    unreadCount.value = 0
    if (unreadTimer) {
      clearInterval(unreadTimer)
      unreadTimer = null
    }
  }
}

function logout() {
  clearToken()
  me.value = null
  showToast('success', '已退出', '你的登录状态已清除')
  router.push('/login')
}

const toastTitle = computed(() => {
  if (toastState.value.type === 'success') return '成功'
  if (toastState.value.type === 'error') return '出错了'
  return '提示'
})

onMounted(loadMe)
onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})

// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(window as any).__AUTH_CHANGED__ = loadMe
// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(window as any).__MESSAGES_CHANGED__ = loadUnread
</script>

<style scoped>
.sidebar {
  width: 240px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  border-radius: 0;
}
.sidebar-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px 16px;
  flex: 1;
}
.brand { display:flex; align-items:center; gap:12px; cursor:pointer; user-select:none; }
.logo {
  width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(37,99,235,0.95), rgba(29,78,216,0.95));
  color: white;
  display:flex; align-items:center; justify-content:center;
  font-weight: 900;
  flex-shrink: 0;
}
.brand-name { font-weight: 900; letter-spacing: 0.2px; }
.nav {
  display:flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  justify-content: flex-start;
  margin-top: 20px;
}
.nav-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.nav-title {
  margin-top: 14px;
  margin-bottom: 2px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: rgba(15, 23, 42, 0.55);
  text-transform: uppercase;
}
.nav-link { padding: 10px 12px; border-radius: 10px; color: var(--muted); }
.nav-link.router-link-active {
  color: var(--text);
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.22);
}
.nav-link-messages { position: relative; }
.nav-badge {
  margin-left: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.auth {
  display:flex;
  flex-direction: column;
  gap:10px;
  align-items: stretch;
}
.dot { width: 8px; height: 8px; border-radius: 999px; background: var(--success); display:inline-block; }
.user-pill { background: rgba(255,255,255,0.95); }
.role { opacity: 0.8; font-size: 11px; padding-left: 6px; }
</style>
