<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">帖子</h1>
        <div class="subtle">浏览已发布内容，登录后可发布并点赞</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="refresh" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="grid-2">
      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">发布帖子</h2>
          <span class="pill">发布后进入「待审核」</span>
        </div>

        <div class="row">
          <div class="field">
            <label>分类</label>
            <select class="select" v-model.number="createForm.categoryId">
              <option v-for="c in categories" :key="c.id" :value="c.id">
                {{ c.name }}
              </option>
            </select>
          </div>

          <div class="field">
            <label>标题</label>
            <input class="input" v-model.trim="createForm.title" placeholder="写一个清晰的标题" />
          </div>

          <div class="field">
            <label>内容</label>
            <textarea class="textarea" v-model.trim="createForm.content" rows="7" placeholder="分享你的想法..." />
          </div>

          <button class="btn btn-primary" @click="createPost" :disabled="creating">
            {{ creating ? '提交中...' : '发布' }}
          </button>

          <div class="subtle">提示：只有管理员审核通过后，帖子才会出现在右侧列表。</div>
        </div>
      </section>

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
            <button class="btn" @click="refresh" :disabled="loading">查询</button>
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
import { onMounted, reactive, ref } from 'vue'
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
const creating = ref(false)
const posts = ref<Post[]>([])

const keyword = ref('')
const sortBy = ref<'createTime' | 'viewCount' | 'likeCount'>('createTime')
const categories = ref<Category[]>([])

const createForm = reactive({
  categoryId: null as number | null,
  title: '',
  content: ''
})

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
  const res = await apiGet<ApiResult<Category>>('/categories/tree')
  if (res?.code === 200) {
    categories.value = res.data || []
    if (categories.value.length > 0 && !createForm.categoryId) {
      createForm.categoryId = categories.value[0].id
    }
  }
}

async function createPost() {
  if (!createForm.categoryId || !createForm.title || !createForm.content) {
    showToast('error', '发布失败', '分类、标题、内容不能为空')
    return
  }

  creating.value = true

  const res = await apiPost<ApiResult<Post>>('/posts', {
    categoryId: createForm.categoryId,
    title: createForm.title,
    content: createForm.content
  })

  creating.value = false

  if (!res) {
    showToast('error', '发布失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '发布失败', res.message || '发布失败')
    return
  }

  showToast('success', '发布成功', `postId=${res.data.id}，等待管理员审核`)
  createForm.title = ''
  createForm.content = ''
}

async function like(id: number) {
  const res = await apiPost<ApiResult<null>>(`/posts/${id}/like`)
  if (!res) {
    showToast('error', '点赞失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '点赞失败', res.message || '点赞失败')
    return
  }

  showToast('success', '点赞成功', '感谢你的支持')
  await refresh()
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
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }
.filters { display:flex; gap: 10px; align-items:center; flex-wrap: wrap; }
.empty { color: var(--muted); padding: 10px 0; }

.post { padding: 14px 0; border-top: 1px solid rgba(15, 23, 42, 0.10); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; letter-spacing: 0.2px; margin-bottom: 8px; }
.post-meta { display:flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.post-content { color: rgba(15, 23, 42, 0.85); white-space: pre-wrap; line-height: 1.55; }
.post-actions { margin-top: 12px; display:flex; gap: 10px; }

@media (max-width: 980px) {
  .filters { flex-direction: column; align-items: stretch; }
}
</style>
