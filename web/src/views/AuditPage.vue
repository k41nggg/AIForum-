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

    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'pending' }" @click="switchTab('pending')">待审核</button>
      <button class="tab" :class="{ active: tab === 'rejected' }" @click="switchTab('rejected')">未通过</button>
    </div>

    <section class="glass card" v-if="tab === 'pending'">
      <div class="section-head">
        <h2 class="h2">待审核列表</h2>
        <span class="pill">status=AUDIT_PENDING</span>
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

    <section class="glass card" v-else>
      <div class="section-head">
        <h2 class="h2">未通过列表</h2>
        <span class="pill">status=DELETED</span>
      </div>

      <div v-if="loadingRejected" class="subtle">加载中...</div>
      <div v-else-if="rejectedItems.length === 0" class="empty">暂无未通过帖子</div>

      <div class="item" v-for="p in rejectedItems" :key="p.id">
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
            <button class="btn btn-primary" @click="audit(p.id, 'PUBLISHED')" :disabled="auditingId === p.id">重新上架</button>
            <button
              class="btn btn-danger"
              @click="removePost(p.id)"
              :disabled="deletingId === p.id || auditingId === p.id"
            >
              {{ deletingId === p.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
        <div v-if="p.auditReason" class="audit-reason">
          <strong>未通过原因:</strong> {{ p.auditReason }}
        </div>
        <div class="content">{{ p.content }}</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiDelete, apiGet, apiPut, type ApiResult } from '../lib/api'
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

type Tab = 'pending' | 'rejected'

const tab = ref<Tab>('pending')

const loading = ref(false)
const items = ref<Post[]>([])

const loadingRejected = ref(false)
const rejectedItems = ref<Post[]>([])

const auditingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

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

async function loadRejected() {
  loadingRejected.value = true
  const res = await apiGet<ApiResult<Page<Post>>>('/posts/audit/rejected?current=1&size=50')
  loadingRejected.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    rejectedItems.value = []
    return
  }

  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '无权访问或加载失败')
    rejectedItems.value = []
    return
  }

  rejectedItems.value = res.data.records || []
}

function switchTab(next: Tab) {
  tab.value = next
  if (next === 'pending') load()
  else loadRejected()
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
  if (tab.value === 'pending') await load()
  else await loadRejected()
}

async function removePost(id: number) {
  if (!confirm('确定永久删除该帖子？此操作不可恢复。')) return

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

  showToast('success', '删除成功', '帖子已永久删除')
  await loadRejected()
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }

.tabs { display:flex; gap: 10px; }
.tab {
  border: 1px solid rgba(15,23,42,0.12);
  background: rgba(255,255,255,0.70);
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
  color: rgba(15,23,42,0.8);
}
.tab.active {
  border-color: rgba(37, 99, 235, 0.35);
  background: rgba(37, 99, 235, 0.10);
  color: rgba(29, 78, 216, 0.95);
}

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
.btn-danger {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.08);
  color: rgba(185, 28, 28, 0.95);
}
.btn-danger:hover {
  background: rgba(239, 68, 68, 0.12);
}
</style>
