<template>
  <div class="auth-wrap">
    <div class="glass card auth-card">
      <div class="head">
        <h1 class="h1">创建账号</h1>
        <div class="subtle">注册后即可登录使用完整功能</div>
      </div>

      <div class="row">
        <div class="field">
          <label>用户名</label>
          <input class="input" v-model.trim="form.username" placeholder="用户名" autocomplete="username" />
        </div>

        <div class="field">
          <label>密码</label>
          <input class="input" v-model.trim="form.password" type="password" placeholder="密码" autocomplete="new-password" />
        </div>

        <div class="field">
          <label>邮箱（可选）</label>
          <input class="input" v-model.trim="form.email" placeholder="xxx@example.com" autocomplete="email" />
        </div>

        <div class="field">
          <label>昵称（可选）</label>
          <input class="input" v-model.trim="form.nickname" placeholder="昵称" />
        </div>

        <button class="btn btn-primary" @click="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="subtle">
          已有账号？<RouterLink class="link" to="/login">去登录</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiPost, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: '',
  nickname: ''
})

async function submit() {
  if (!form.username || !form.password) {
    showToast('error', '注册失败', '用户名和密码不能为空')
    return
  }

  loading.value = true

  const res = await apiPost<ApiResult<unknown>>('/auth/register', {
    username: form.username,
    password: form.password,
    email: form.email || null,
    nickname: form.nickname || null
  })

  loading.value = false

  if (!res) {
    showToast('error', '网络错误', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '注册失败', res.message || '注册失败')
    return
  }

  showToast('success', '注册成功', '请使用新账号登录')
  router.push('/login')
}
</script>

<style scoped>
.auth-wrap { min-height: calc(100vh - 92px); display:flex; align-items:center; justify-content:center; }
.auth-card { width: 460px; }
.head { display:grid; gap: 6px; margin-bottom: 14px; }
.link { text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 520px) { .auth-card { width: 100%; } }
</style>
