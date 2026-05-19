<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">帖子</h1>
        <div class="subtle">浏览已发布内容，登录后可发布并点赞</div>
      </div>
      <div class="page-actions">
        <RouterLink class="btn btn-primary" to="/posts/create">发布帖子</RouterLink>
        <button class="btn" @click="refresh" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="grid-1">
      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">发现内容</h2>
          <div class="filters">
            <input class="input" v-model.trim="keyword" placeholder="搜索标题/内容" />
            <select class="select" v-model="categoryId" style="max-width: 160px">
              <option value="">所有分类</option>
              <option v-for="c in categories" :key="c.id" :value="String(c.id)">
                {{ c.name }}
              </option>
            </select>
            <select class="select" v-model="sortBy" style="max-width: 140px">
              <option value="createTime">最新</option>
              <option value="viewCount">浏览</option>
              <option value="likeCount">点赞</option>
            </select>
            <button class="btn btn-primary" @click="refresh" :disabled="loading">查询</button>
          </div>
        </div>

        <div v-if="loading" class="subtle" style="padding: 10px 0">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty">暂无已发布帖子</div>

        <div class="post" v-for="p in posts" :key="p.id">
          <div class="post-title">{{ p.title }}</div>
          <div class="post-meta">
            <span class="pill">#{{ p.id }}</span>
            <span class="pill">作者 {{ p.userNickname || p.userId }}</span>
            <span class="pill">分类 {{ getCategoryName(p.categoryId) }}</span>
            <span class="pill">浏览 {{ p.viewCount }}</span>
            <span class="pill">点赞 {{ p.likeCount }}</span>
          </div>
          <div class="post-content">{{ p.content.length > 100 ? p.content.slice(0, 100) + '...' : p.content }}</div>
          <div class="post-actions">
            <button class="btn" @click="goDetail(p.id)">查看详情</button>
            <button class="btn" @click="like(p.id)">点赞</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPost, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type Post = {
  id: number
  userId: number
  userNickname?: string
  categoryId: number
  title: string
  content: string
  viewCount: number
  likeCount: number
  collectCount: number
  commentCount: number
  status: number
  createTime: string
}

type Page<T> = {
  records: T[]
}

type Category = {
  id: number
  name: string
}

const loading = ref(false)
const posts = ref<Post[]>([])

const keyword = ref('')
const sortBy = ref<'createTime' | 'viewCount' | 'likeCount'>('createTime')
const categories = ref<Category[]>([])

const router = useRouter()

const categoryId = ref('')

async function refresh() {
  loading.value = true

  const qs = new URLSearchParams({
    current: '1',
    size: '20',
    sortBy: sortBy.value,
    order: 'desc'
  })
  if (keyword.value) qs.set('keyword', keyword.value)
  if (categoryId.value) qs.set('categoryId', categoryId.value)

  const res = await apiGet<ApiResult<Page<Post>>>(`/posts?${qs.toString()}`)
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    posts.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '查询失败')
    posts.value = []
    return
  }

  posts.value = res.data.records || []
}

function getCategoryName(id: number) {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : `ID: ${id}`
}

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) {
    categories.value = res.data || []
  }
}

async function like(postId: number) {
  const res = await apiPost<ApiResult<null>>(`/posts/${postId}/like`, {})
  if (res?.code === 200) {
    showToast('success', '点赞成功')
    refresh()
  } else {
    showToast('error', '点赞失败', res?.message)
  }
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

onMounted(() => {
  refresh()
  loadCategories()
})
</script>

<style scoped>
.grid-1 {
  display: grid;
  gap: 24px;
}
.post {
  padding: 16px;
  border-bottom: 1px solid var(--border);
}
.post:last-child {
  border-bottom: none;
}
.post-title {
  font-size: 1.1rem;
  font-weight: bold;
  margin-bottom: 8px;
}
.post-meta {
  display: flex;
  gap: 8px;
  font-size: 0.9rem;
  color: var(--muted);
  margin-bottom: 8px;
}
.post-content {
  color: var(--text);
  line-height: 1.6;
  margin-bottom: 16px;
}
.post-actions {
  display: flex;
  gap: 8px;
}
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
