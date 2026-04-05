<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">内容审核</h1>
        <div class="subtle">管理员审核待发布帖子（通过 / 下架）</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
      </div>
    </div>

    <section class="glass card">
      <div class="section-head">
        <h2 class="h2">待审核列表</h2>
        <span class="pill">status=1</span>
      </div>

      <div v-if="loading" class="subtle">加载中...</div>
      <div v-else-if="items.length === 0" class="empty">暂无待审核帖子</div>

      <div class="item" v-for="p in items" :key="p.id">
        <div class="item-head">
          <div>
            <div class="title">{{ p.title }}</div>
            <div class="meta">
              <span class="pill">#{{ p.id }}</span>
              <span class="pill">分类 {{ p.categoryId }}</span>
              <span class="pill">作者 {{ p.userId }}</span>
            </div>
          </div>
          <div class="actions">
            <button class="btn btn-primary" @click="audit(p.id, 'PUBLISHED')" :disabled="auditingId === p.id">通过</button>
            <button class="btn" @click="audit(p.id, 'DELETED')" :disabled="auditingId === p.id">下架</button>
          </div>
        </div>
        <div v-if="p.auditReason" class="audit-reason">
          <strong>AI 审核意见:</strong> {{ p.auditReason }}
        </div>
        <div class="content">{{ p.content }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiGet, apiPut, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type Post = {
  id: number
  userId: number
  categoryId: number
  title: string
  content: string
  status: string
  auditReason?: string
  createTime: string
}

type Page<T> = { records: T[] }

const loading = ref(false)
const items = ref<Post[]>([])
const auditingId = ref<number | null>(null)

async function load() {
  loading.value = true
  const res = await apiGet<ApiResult<Page<Post>>>('/posts/audit/list?current=1&size=50')
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    items.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '无权访问或加载失败')
    items.value = []
    return
  }

  items.value = res.data.records || []
}

async function audit(id: number, status: 'PUBLISHED' | 'DELETED') {
  auditingId.value = id
  const res = await apiPut<ApiResult<null>>(`/posts/${id}/audit?status=${status}`)
  auditingId.value = null

  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '操作失败')
    return
  }

  showToast('success', '操作成功', status === 'PUBLISHED' ? '已通过审核' : '已下架')
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

.item { padding: 14px 0; border-top: 1px solid rgba(15,23,42,0.10); }
.item:first-of-type { border-top: none; padding-top: 0; }
.item-head { display:flex; justify-content: space-between; gap: 12px; align-items:flex-start; }
.title { font-weight: 850; letter-spacing: 0.2px; }
.meta { display:flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.actions { display:flex; gap: 10px; flex-wrap: wrap; }
.content { margin-top: 10px; color: rgba(15,23,42,0.86); white-space: pre-wrap; line-height: 1.6; }
.audit-reason {
  margin-top: 10px;
  padding: 8px 12px;
  background-color: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  color: #b88100;
  border-radius: 8px;
  font-size: 14px;
}
</style>
