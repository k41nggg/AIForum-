<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">推荐</h1>
        <div class="subtle">基于点赞、收藏与订阅的 AI 个性化推荐</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary" @click="aiRefresh" :disabled="refreshing">
          {{ refreshButtonText }}
        </button>
      </div>
    </div>

    <section v-if="cached && posts.length > 0 && displaySummary" class="glass card meta-card">
      <div class="summary-title">AI 总结</div>
      <p class="summary">{{ displaySummary }}</p>
      <p v-if="updatedLabel" class="subtle meta-line">{{ updatedLabel }}</p>
      <p v-if="actionCount != null && actionCount < 3" class="hint-bar">
        你的点赞/收藏较少，多互动后推荐会更准
      </p>
    </section>

    <section class="glass card">
      <div class="section-head">
        <h2 class="h2">推荐帖子</h2>
        <span v-if="posts.length" class="pill">{{ posts.length }} 篇</span>
      </div>

      <div v-if="loading" class="subtle state">加载中...</div>
      <div v-else-if="!cached && posts.length === 0" class="empty-state">
        <div class="empty-icon">✨</div>
        <p>{{ hint || '点击上方「AI 刷新推荐」生成你的专属列表' }}</p>
      </div>
      <div v-else-if="posts.length === 0" class="empty-state">
        <p>暂无推荐结果，请稍后重试 AI 刷新</p>
      </div>

      <div v-else>
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
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, recommendations, type ApiResult, type RecommendationResult } from '../lib/api'
import PostPreviewContent from '../components/PostPreviewContent.vue'
import { authorLabel, getCategoryName as resolveCategoryName, normalizePostAuthor } from '../lib/postDisplay'
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

type Category = { id: number; name: string }

const loading = ref(false)
const refreshing = ref(false)
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const cached = ref(false)
const summary = ref('')
const hint = ref('')
const actionCount = ref<number | null>(null)
const updatedAt = ref<string | null>(null)
const router = useRouter()

const updatedLabel = computed(() => {
  if (!updatedAt.value) return ''
  const d = new Date(updatedAt.value)
  if (Number.isNaN(d.getTime())) return ''
  return `更新于 ${d.toLocaleString()}`
})

/** 仅展示后端 AI 生成的兴趣总结 */
const displaySummary = computed(() => summary.value?.trim() || '')

const refreshButtonText = computed(() => (refreshing.value ? 'AI 分析中...' : 'AI 刷新推荐'))

function applyData(data: RecommendationResult | undefined) {
  posts.value = ((data?.posts || []) as Post[]).map(normalizePostAuthor)
  cached.value = Boolean(data?.cached)
  summary.value = data?.summary || ''
  hint.value = data?.hint || ''
  actionCount.value = data?.actionCount ?? null
  updatedAt.value = data?.updatedAt || null
}

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) categories.value = res.data || []
}

async function loadCached() {
  loading.value = true
  const res = await recommendations.get()
  loading.value = false
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '加载失败')
    return
  }
  applyData(res.data)
}

async function aiRefresh() {
  refreshing.value = true
  const res = await recommendations.refresh()
  refreshing.value = false

  if (!res) {
    showToast('error', '刷新失败', '无法连接后端服务')
    return
  }

  if (res.code !== 200) {
    showToast('error', '刷新失败', res.message || 'AI 推荐失败')
    return
  }

  applyData(res.data)
  showToast('success', '推荐已更新', 'AI 已根据你的兴趣生成新列表')
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

onMounted(() => {
  loadCategories()
  loadCached()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-actions { display: flex; gap: 10px; }
.meta-card { padding: 14px 16px; }
.summary-title {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgba(37, 99, 235, 0.85);
  margin-bottom: 8px;
}
.summary { margin: 0 0 6px; line-height: 1.65; font-size: 15px; color: rgba(15, 23, 42, 0.9); }
.meta-line { margin: 0; font-size: 13px; }
.hint-bar {
  margin: 10px 0 0;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.12);
  color: rgba(146, 64, 14, 0.95);
  font-size: 13px;
}
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.state, .empty-state { color: var(--muted); padding: 24px 0; text-align: center; }
.empty-icon { font-size: 28px; margin-bottom: 8px; opacity: 0.7; }
.post { padding: 14px 0; border-top: 1px solid rgba(15, 23, 42, 0.1); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; margin-bottom: 8px; cursor: pointer; }
.post-title:hover { color: var(--primary); }
.post-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.post :deep(.post-preview) { margin-bottom: 10px; }
.post-actions { margin-top: 12px; }
</style>
