<template>
  <div class="auth-wrap">
    <div class="glass card auth-card">
      <div class="head">
        <h1 class="h1">欢迎回来</h1>
        <div class="subtle">登录后可以发布帖子、点赞与订阅分类</div>
      </div>

      <div class="row">
        <div class="field">
          <label>用户名</label>
          <input class="input" v-model.trim="username" placeholder="admin" autocomplete="username" />
        </div>

        <div class="field">
          <label>密码</label>
          <input
            class="input"
            v-model.trim="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="submit"
          />
        </div>

        <button class="btn btn-primary" @click="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="subtle">
          还没有账号？<RouterLink class="link" to="/register">立即注册</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { apiPost, setToken, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type LoginResp = { token: string }

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    showToast('error', '登录失败', '请输入用户名和密码')
    return
  }

  loading.value = true
  const res = await apiPost<ApiResult<LoginResp>>('/auth/login', {
    username: username.value,
    password: password.value
  })
  loading.value = false

  if (!res) {
    showToast('error', '网络错误', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '登录失败', res.message || '用户名或密码错误')
    return
  }

  setToken(res.data.token)

  // 1. 立即触发全局登录态更新
  if ((window as any).__AUTH_CHANGED__) {
    await (window as any).__AUTH_CHANGED__()
  }

  showToast('success', '登录成功', '欢迎回来')

  // 2. 跳转到重定向页面或首页
  const redirect = (route.query.redirect as string) || '/'
  router.push(redirect)
}
</script>

<style scoped>
.auth-wrap { min-height: calc(100vh - 92px); display:flex; align-items:center; justify-content:center; }
.auth-card { width: 460px; }
.head { display:grid; gap: 6px; margin-bottom: 14px; }
.link { text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 520px) { .auth-card { width: 100%; } }
</style>
