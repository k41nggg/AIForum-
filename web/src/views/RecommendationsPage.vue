<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">推荐</h1>
        <div class="subtle">为你量身定制的个性化内容</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="refresh" :disabled="loading">刷新</button>
      </div>
    </div>

    <section class="glass card">
      <div class="section-head">
        <h2 class="h2">推荐帖子</h2>
      </div>

      <div v-if="loading" class="subtle" style="padding: 10px 0">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">暂无推荐内容，多去看看帖子吧</div>

      <div class="post" v-for="p in posts" :key="p.id">
        <div class="post-title" @click="goDetail(p.id)">{{ p.title }}</div>
        <div class="post-meta">
          <span class="pill">作者 {{ authorLabel(p) }}</span>
          <span class="pill">分类 {{ resolveCategoryName(p.categoryId, categories) }}</span>
          <span class="pill">浏览 {{ p.viewCount }}</span>
          <span class="pill">点赞 {{ p.likeCount }}</span>
        </div>
        <PostPreviewContent :content="p.content" />
        <div class="post-actions">
          <button class="btn" @click="goDetail(p.id)">查看详情</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, type ApiResult } from '../lib/api'
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
}

type Category = {
  id: number
  name: string
}

const loading = ref(false)
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const router = useRouter()

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) categories.value = res.data || []
}

async function refresh() {
  loading.value = true
  try {
    const res = await apiGet<ApiResult<Post[]>>('/recommendations')
    if (!res) {
      showToast('error', '加载失败', '无法连接后端服务')
      posts.value = []
      return
    }

    if (res.code === 200) {
      posts.value = res.data || []
    } else {
      showToast('error', '加载失败', res.message || '加载失败')
    }
  } catch (e: any) {
    showToast('error', '加载失败', e.message || '请求失败')
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

onMounted(() => {
  loadCategories()
  refresh()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }
.empty { color: var(--muted); padding: 10px 0; }

.post { padding: 14px 0; border-top: 1px solid rgba(15, 23, 42, 0.10); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title {
  font-weight: 750;
  letter-spacing: 0.2px;
  margin-bottom: 8px;
  cursor: pointer;
}
.post-title:hover { color: var(--primary); }
.post-meta { display:flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.post :deep(.post-preview) { margin-bottom: 10px; }
.post-actions { margin-top: 12px; display:flex; gap: 10px; }
</style>
