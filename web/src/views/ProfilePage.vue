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

    <section class="glass card">
      <h2 class="h2">我的帖子</h2>
      <div v-if="postsLoading" class="subtle">加载中...</div>
      <div v-else-if="myPosts.length === 0" class="empty">你还没有发布过帖子</div>
      <div v-else>
        <div class="post" v-for="p in myPosts" :key="p.id">
          <div class="post-title" @click="goDetail(p.id)">{{ p.title }}</div>
          <div class="post-meta-row">
            <div class="post-meta">
              <span class="pill" :class="getStatusClass(p.status)">{{ getStatusText(p.status) }}</span>
              <span class="pill">#{{ p.id }}</span>
              <span class="pill">浏览 {{ p.viewCount }}</span>
              <span class="pill">点赞 {{ p.likeCount }}</span>
              <span class="pill">{{ new Date(p.createTime).toLocaleString() }}</span>
            </div>
            <button class="btn btn-danger btn-compact" @click="removeMyPost(p.id)" :disabled="deletingId === p.id">
              {{ deletingId === p.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiDelete, apiGet, apiPut, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type User = {
  id: number
  username: string
  nickname?: string
  avatar?: string
  bio?: string
  role?: string
}

type Post = {
  id: number
  title: string
  status: string
  viewCount: number
  likeCount: number
  createTime: string
}

type Page<T> = {
  records: T[]
}

const loading = ref(false)
const saving = ref(false)
const me = ref<User | null>(null)
const postsLoading = ref(false)
const myPosts = ref<Post[]>([])
const deletingId = ref<number | null>(null)
const router = useRouter()

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

async function loadMyPosts() {
  postsLoading.value = true
  const res = await apiGet<ApiResult<Page<Post>>>('/posts/my')
  postsLoading.value = false
  if (res?.code === 200) {
    myPosts.value = res.data.records || []
  }
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

async function removeMyPost(id: number) {
  deletingId.value = id
  const res = await apiDelete<ApiResult<null>>(`/posts/${id}`)
  deletingId.value = null

  if (!res) {
    showToast('error', '删除失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '删除失败', res.message || '删除失败')
    return
  }

  showToast('success', '删除成功', '帖子已删除')
  await loadMyPosts()
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

function getStatusText(status: string) {
  switch (status) {
    case 'AUDIT_PENDING': return '待审核'
    case 'PUBLISHED': return '已发布'
    case 'DELETED': return '已删除'
    default: return status
  }
}

function getStatusClass(status: string) {
  switch (status) {
    case 'AUDIT_PENDING': return 'status-pending'
    case 'PUBLISHED': return 'status-published'
    case 'DELETED': return 'status-deleted'
    default: return ''
  }
}

onMounted(() => {
  load()
  loadMyPosts()
})
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

.form { flex: 1; }
.row { display: flex; flex-direction: column; gap: 16px; }

.post { padding: 14px 0; border-top: 1px solid var(--border); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; letter-spacing: 0.2px; margin-bottom: 8px; cursor: pointer; }
.post-title:hover { color: var(--primary); }
.post-meta { display:flex; gap: 8px; flex-wrap: wrap; }
.post-meta-row {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}
.post-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.btn-danger {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.08);
  color: rgba(185, 28, 28, 0.95);
}
.btn-danger:hover {
  background: rgba(239, 68, 68, 0.12);
}
.btn-compact {
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.2;
  min-height: 28px;
  border-radius: 10px;
  white-space: nowrap;
}

.status-pending { background-color: #f59e0b; color: white; }
.status-published { background-color: #10b981; color: white; }
.status-deleted { background-color: #ef4444; color: white; }

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .post-meta-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
