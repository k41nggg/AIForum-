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

const creating = ref(false)
const categories = ref<Category[]>([])

const createForm = reactive({
  categoryId: null as number | null,
  title: '',
  content: ''
})

const router = useRouter()

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
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
