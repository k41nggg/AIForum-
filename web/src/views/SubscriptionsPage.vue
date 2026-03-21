<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">我的订阅</h1>
        <div class="subtle">管理你关注的话题分类</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
      </div>
    </div>

    <section class="glass card">
      <div class="section-head">
        <h2 class="h2">订阅列表</h2>
        <span class="pill">共 {{ categories.length }} 个</span>
      </div>

      <div v-if="loading" class="subtle">加载中...</div>
      <div v-else-if="categories.length === 0" class="empty">你还没有订阅任何分类</div>

      <div class="list" v-else>
        <div class="item" v-for="c in categories" :key="c.id">
          <div>
            <div class="title">{{ c.name }}</div>
            <div class="subtle">#{{ c.id }} · parent {{ c.parentId }}</div>
            <div class="subtle" v-if="c.description">{{ c.description }}</div>
          </div>
          <div class="actions">
            <button class="btn" @click="unsubscribe(c.id)" :disabled="unsubscribingId === c.id">
              {{ unsubscribingId === c.id ? '处理中...' : '取消订阅' }}
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiDelete, apiGet, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type Category = {
  id: number
  name: string
  description?: string
  parentId: number
}

const loading = ref(false)
const categories = ref<Category[]>([])
const unsubscribingId = ref<number | null>(null)

async function load() {
  loading.value = true

  const res = await apiGet<ApiResult<Category[]>>('/subscriptions/me')
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    categories.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '加载失败')
    categories.value = []
    return
  }

  categories.value = res.data || []
}

async function unsubscribe(categoryId: number) {
  unsubscribingId.value = categoryId
  const res = await apiDelete<ApiResult<null>>(`/subscriptions/${categoryId}`)
  unsubscribingId.value = null

  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '取消订阅失败')
    return
  }

  showToast('success', '已取消订阅', '你将不再收到该分类的更新')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }
.empty { color: var(--muted); padding: 10px 0; }

.list { display: grid; gap: 10px; }
.item {
  border: 1px solid rgba(15,23,42,0.10);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255,255,255,0.70);
  display:flex;
  justify-content: space-between;
  gap: 12px;
}
.title { font-weight: 800; }
.actions { display:flex; align-items:flex-start; }
</style>
