<template>
  <div>
    <header class="topbar glass">
      <div class="topbar-inner container">
        <div class="brand" @click="$router.push('/')">
          <div class="logo">A</div>
          <div>
            <div class="brand-name">AIForum</div>
            <div class="subtle">一个简洁的论坛应用</div>
          </div>
        </div>

        <nav class="nav">
          <RouterLink class="nav-link" to="/">帖子</RouterLink>
          <RouterLink class="nav-link" to="/categories">分类</RouterLink>
          <RouterLink class="nav-link" to="/subscriptions">我的订阅</RouterLink>
          <RouterLink class="nav-link" to="/profile">个人中心</RouterLink>
          <RouterLink class="nav-link" to="/audit">审核</RouterLink>
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
    </header>

    <main class="container">
      <RouterView />
    </main>

    <div v-if="toastState.show" class="toast" role="status" aria-live="polite">
      <div class="toast-title">{{ toastTitle }}</div>
      <div class="toast-msg">{{ toastState.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, clearToken, type ApiResult } from './lib/api'
import { toast as toastState, showToast } from './lib/toast'

type User = {
  id: number
  username: string
  nickname?: string
  role?: string
}

const router = useRouter()
const me = ref<User | null>(null)

async function loadMe() {
  const res = await apiGet<ApiResult<User>>('/users/me')
  if (res?.code === 200) me.value = res.data
  else me.value = null
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

// 关键修复：确保全局函数 __AUTH_CHANGED__ 存在并被赋值，以便登录页可以调用它
// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(window as any).__AUTH_CHANGED__ = loadMe
</script>

<style scoped>
.topbar { position: sticky; top: 0; z-index: 20; border-radius: 0; border-left: none; border-right: none; }
.topbar-inner { display:flex; align-items:center; gap:16px; justify-content: space-between; padding-top: 14px; padding-bottom: 14px; }
.brand { display:flex; align-items:center; gap:12px; cursor:pointer; user-select:none; }
.logo {
  width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(37,99,235,0.95), rgba(29,78,216,0.95));
  color: white;
  display:flex; align-items:center; justify-content:center;
  font-weight: 900;
}
.brand-name { font-weight: 900; letter-spacing: 0.2px; }
.nav { display:flex; gap: 8px; align-items:center; flex: 1; justify-content:center; }
.nav-link { padding: 8px 12px; border-radius: 10px; color: var(--muted); }
.nav-link.router-link-active {
  color: var(--text);
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.22);
}
.auth { display:flex; gap:10px; align-items:center; }
.dot { width: 8px; height: 8px; border-radius: 999px; background: var(--success); display:inline-block; }
.user-pill { background: rgba(255,255,255,0.95); }
.role { opacity: 0.8; font-size: 11px; padding-left: 6px; }
</style>
