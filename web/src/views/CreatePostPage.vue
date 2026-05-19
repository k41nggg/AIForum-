<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">发布帖子</h1>
        <div class="subtle">分享你的想法，发布后将进入审核</div>
      </div>
      <div class="page-actions">
        <RouterLink class="btn" to="/posts">返回列表</RouterLink>
      </div>
    </div>

    <div class="grid-1">
      <section class="glass card">
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
            <label>AI 推荐分类</label>
            <div class="ai-row">
              <button class="btn" @click="recommendCategory" :disabled="recommending">
                {{ recommending ? '分析中...' : '推荐分类' }}
              </button>
              <div v-if="aiRecommend" class="ai-suggest">
                <div class="ai-title">
                  建议：
                  <strong>{{ aiRecommend.categoryName }}</strong>
                  <span v-if="aiRecommend.parentName">（父类：{{ aiRecommend.parentName }}）</span>
                  <span v-if="aiRecommend.created" class="badge">新建</span>
                  <span v-if="aiRecommend.confidence != null" class="subtle">置信度：{{ (aiRecommend.confidence * 100).toFixed(0) }}%</span>
                </div>
                <div v-if="aiRecommend.reason" class="subtle ai-reason">{{ aiRecommend.reason }}</div>
                <div class="ai-actions">
                  <button class="btn btn-primary" @click="acceptAiCategory">
                    使用该分类
                  </button>
                  <button class="btn" @click="clearAiRecommend">忽略</button>
                </div>
              </div>
            </div>
            <div class="subtle">你可以采纳 AI 建议，也可以继续手动选择分类，两者并存。</div>
          </div>

          <div class="field">
            <label>标题</label>
            <input class="input" v-model.trim="createForm.title" placeholder="写一个清晰的标题" />
          </div>

          <div class="field">
            <label>内容</label>
            <textarea class="textarea" v-model.trim="createForm.content" rows="10" placeholder="分享你的想法..." />
          </div>

          <button class="btn btn-primary" @click="createPost" :disabled="creating">
            {{ creating ? '提交中...' : '发布' }}
          </button>

          <div class="subtle">提示：只有管理员审核通过后，帖子才会出现在帖子列表。</div>
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

type Category = {
  id: number
  name: string
}

type RecommendCategoryResponse = {
  categoryId: number
  categoryName: string
  parentId?: number
  parentName?: string
  confidence?: number
  reason?: string
  created?: boolean
}

const creating = ref(false)
const recommending = ref(false)
const categories = ref<Category[]>([])
const aiRecommend = ref<RecommendCategoryResponse | null>(null)

const createForm = reactive({
  categoryId: null as number | null,
  title: '',
  content: ''
})

const router = useRouter()

async function loadCategories(preselectId?: number) {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) {
    categories.value = res.data || []
    if (preselectId) {
      createForm.categoryId = preselectId
      return
    }
    if (categories.value.length > 0 && !createForm.categoryId) {
      createForm.categoryId = categories.value[0].id
    }
  }
}

async function recommendCategory() {
  if (!createForm.title || !createForm.content) {
    showToast('error', '无法推荐', '请先填写标题和内容')
    return
  }

  recommending.value = true
  const res = await apiPost<ApiResult<RecommendCategoryResponse>>('/categories/recommend', {
    title: createForm.title,
    content: createForm.content
  })
  recommending.value = false

  if (!res) {
    showToast('error', '推荐失败', '无法连接后端服务')
    return
  }

  if (res.code !== 200) {
    showToast('error', '推荐失败', res.message)
    return
  }

  aiRecommend.value = res.data
  if (!aiRecommend.value?.categoryId) {
    showToast('error', '推荐失败', 'AI 未返回可用分类')
    return
  }

  // 若 AI 新建了分类，刷新分类列表以便手动下拉也能看到
  if (aiRecommend.value.created) {
    await loadCategories(aiRecommend.value.categoryId)
  }
}

function acceptAiCategory() {
  if (!aiRecommend.value?.categoryId) return
  createForm.categoryId = aiRecommend.value.categoryId
  showToast('success', '已采纳', `分类已设置为：${aiRecommend.value.categoryName}`)
}

function clearAiRecommend() {
  aiRecommend.value = null
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

  if (res.code === 200) {
    showToast('success', '发布成功', '帖子已提交审核')
    router.push('/posts')
  } else {
    showToast('error', '发布失败', res.message)
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.ai-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.ai-suggest {
  flex: 1;
  min-width: 260px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.12);
}

.ai-title {
  display: flex;
  gap: 8px;
  align-items: baseline;
  flex-wrap: wrap;
}

.ai-reason {
  margin-top: 6px;
}

.ai-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 999px;
  background: rgba(80, 160, 255, 0.25);
  border: 1px solid rgba(80, 160, 255, 0.35);
}
</style>
