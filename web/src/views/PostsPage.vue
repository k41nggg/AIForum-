<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">帖子</h1>
        <div class="subtle">浏览已发布内容，登录后可发布并点赞</div>
      </div>
      <!-- 顶部页眉不再放按钮，按钮移动到查询区域右侧 -->
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

            <div class="filters-actions">
              <RouterLink class="btn btn-primary" to="/posts/create">发布帖子</RouterLink>
              <button class="btn" @click="refresh" :disabled="loading">刷新</button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="subtle" style="padding: 10px 0">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty">暂无已发布帖子</div>

        <div class="post" v-for="p in posts" :key="p.id">
          <div class="post-title">{{ p.title }}</div>
          <div class="post-meta">
            <span class="pill">作者 {{ authorLabel(p) }}</span>
            <span class="pill">分类 {{ resolveCategoryName(p.categoryId, categories) }}</span>
            <div class="post-stats">
              <PostStatPill kind="view" :count="p.viewCount" />
              <PostStatPill kind="like" :count="p.likeCount" :active="isPostLiked(p.id)" />
              <PostStatPill kind="collect" :count="p.collectCount" :active="isPostCollected(p.id)" />
            </div>
          </div>
          <PostPreviewContent :content="p.content" />
          <div class="post-actions">
            <button class="btn" @click="goDetail(p.id)">查看详情</button>
            <PostLikeButton
              :liked="isPostLiked(p.id)"
              :count="p.likeCount"
              :loading="likingId === p.id"
              @click="like(p.id)"
            />
            <PostCollectButton
              :collected="isPostCollected(p.id)"
              :count="p.collectCount"
              :loading="collectingId === p.id"
              @click="toggleCollect(p.id)"
            />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, getToken, postCollect, postLike, type ApiResult } from '../lib/api'
import PostCollectButton from '../components/PostCollectButton.vue'
import PostLikeButton from '../components/PostLikeButton.vue'
import PostStatPill from '../components/PostStatPill.vue'
import PostPreviewContent from '../components/PostPreviewContent.vue'
import { authorLabel, getCategoryName as resolveCategoryName } from '../lib/postDisplay'
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
const likedPostIds = ref<Set<number>>(new Set())
const collectedPostIds = ref<Set<number>>(new Set())
const likingId = ref<number | null>(null)
const collectingId = ref<number | null>(null)

function isPostLiked(postId: number) {
  return likedPostIds.value.has(postId)
}

function isPostCollected(postId: number) {
  return collectedPostIds.value.has(postId)
}

async function loadUserActionIds() {
  likedPostIds.value = new Set()
  collectedPostIds.value = new Set()
  if (!getToken()) return
  const [likedRes, collectedRes] = await Promise.all([postLike.likedIds(), postCollect.collectedIds()])
  if (likedRes?.code === 200 && Array.isArray(likedRes.data)) {
    likedPostIds.value = new Set(likedRes.data)
  }
  if (collectedRes?.code === 200 && Array.isArray(collectedRes.data)) {
    collectedPostIds.value = new Set(collectedRes.data)
  }
}

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
  await loadUserActionIds()
}

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) {
    categories.value = res.data || []
  }
}

async function like(postId: number) {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能点赞')
    return
  }
  if (isPostLiked(postId)) return

  likingId.value = postId
  const res = await postLike.like(postId)
  likingId.value = null

  if (res?.code === 200) {
    likedPostIds.value = new Set([...likedPostIds.value, postId])
    const p = posts.value.find((x) => x.id === postId)
    if (p) p.likeCount += 1
    showToast('success', '点赞成功', '')
    return
  }
  if (res?.message === '已点赞') {
    likedPostIds.value = new Set([...likedPostIds.value, postId])
    return
  }
  showToast('error', '点赞失败', res?.message || '点赞失败')
}

async function toggleCollect(postId: number) {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能收藏')
    return
  }

  const wasCollected = isPostCollected(postId)
  collectingId.value = postId
  const res = wasCollected ? await postCollect.uncollect(postId) : await postCollect.collect(postId)
  collectingId.value = null

  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '操作失败')
    return
  }

  const p = posts.value.find((x) => x.id === postId)
  if (wasCollected) {
    const next = new Set(collectedPostIds.value)
    next.delete(postId)
    collectedPostIds.value = next
    if (p && p.collectCount > 0) p.collectCount -= 1
    showToast('success', '已取消收藏', '')
  } else {
    collectedPostIds.value = new Set([...collectedPostIds.value, postId])
    if (p) p.collectCount += 1
    showToast('success', '已收藏', '')
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
  flex-wrap: wrap;
  align-items: center;
  font-size: 0.9rem;
  color: var(--muted);
  margin-bottom: 8px;
}
.post-stats {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.post :deep(.post-preview) {
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
.filters-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-left: auto;
}
.page-head {
  margin-bottom: 14px;
}

@media (max-width: 980px) {
  .filters-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .filters-actions .btn {
    flex: 1;
  }
}
</style>
