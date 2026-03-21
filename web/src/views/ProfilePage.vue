<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">个人中心</h1>
        <div class="subtle">更新昵称、头像与个人简介（需要登录）</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
      </div>
    </div>

    <section class="glass card">
      <div v-if="!me" class="empty">未登录或无法获取用户信息</div>

      <div v-else class="grid">
        <div class="avatar">
          <div class="avatar-img">
            <img v-if="form.avatar" :src="form.avatar" alt="avatar" />
            <div v-else class="avatar-placeholder">{{ (me.nickname || me.username).slice(0, 1).toUpperCase() }}</div>
          </div>
          <div class="subtle">当前角色：{{ me.role || 'USER' }}</div>
        </div>

        <div class="form">
          <div class="row">
            <div class="field">
              <label>昵称</label>
              <input class="input" v-model.trim="form.nickname" placeholder="昵称" />
            </div>
            <div class="field">
              <label>头像 URL</label>
              <input class="input" v-model.trim="form.avatar" placeholder="https://..." />
            </div>
            <div class="field">
              <label>个人简介</label>
              <textarea class="textarea" v-model.trim="form.bio" rows="5" placeholder="介绍一下自己..." />
            </div>

            <button class="btn btn-primary" @click="save" :disabled="saving">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { apiGet, apiPut, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type User = {
  id: number
  username: string
  nickname?: string
  avatar?: string
  bio?: string
  role?: string
}

const loading = ref(false)
const saving = ref(false)
const me = ref<User | null>(null)

const form = reactive({
  nickname: '',
  avatar: '',
  bio: ''
})

async function load() {
  loading.value = true
  const res = await apiGet<ApiResult<User>>('/users/me')
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    me.value = null
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '未登录')
    me.value = null
    return
  }

  me.value = res.data
  form.nickname = res.data.nickname || ''
  form.avatar = res.data.avatar || ''
  form.bio = res.data.bio || ''
}

async function save() {
  saving.value = true
  const res = await apiPut<ApiResult<string>>('/users/profile', {
    nickname: form.nickname || null,
    avatar: form.avatar || null,
    bio: form.bio || null
  })
  saving.value = false

  if (!res) {
    showToast('error', '保存失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '保存失败', res.message || '保存失败')
    return
  }

  showToast('success', '保存成功', '资料已更新')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.empty { color: var(--muted); padding: 10px 0; }

.grid { display:grid; grid-template-columns: 220px 1fr; gap: 16px; align-items: start; }
.avatar { display:grid; gap: 10px; }
.avatar-img {
  width: 140px;
  height: 140px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(15,23,42,0.12);
  background: rgba(255,255,255,0.9);
  box-shadow: var(--shadow-sm);
}
.avatar-img img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  width: 100%; height: 100%;
  display:flex; align-items:center; justify-content:center;
  font-weight: 900; font-size: 48px;
  background: linear-gradient(135deg, rgba(37,99,235,0.14), rgba(29,78,216,0.10));
  color: rgba(15,23,42,0.8);
}

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
}
</style>
