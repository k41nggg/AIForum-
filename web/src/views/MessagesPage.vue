<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">消息通知</h1>
        <div class="subtle">审核、评论、点赞、关注与订阅动态</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="markAllRead" :disabled="markingAll || unreadCount === 0">
          {{ markingAll ? '处理中...' : '全部已读' }}
        </button>
        <button class="btn" @click="reload" :disabled="loading">刷新</button>
      </div>
    </div>

    <section class="glass card">
      <div class="section-head">
        <h2 class="h2">通知列表</h2>
        <span class="pill" v-if="unreadCount > 0">{{ unreadCount }} 条未读</span>
        <span class="pill" v-else>暂无未读</span>
      </div>

      <div v-if="loading" class="subtle">加载中...</div>
      <div v-else-if="items.length === 0" class="empty">暂无消息</div>
      <div v-else class="list">
        <div
          v-for="m in items"
          :key="m.id"
          class="item"
          :class="{ unread: m.isRead === 0 }"
          @click="onItemClick(m)"
        >
          <UserAvatar
            v-if="m.senderId"
            :avatar="m.senderAvatar"
            :name="m.senderNickname || '用户'"
            size="md"
          />
          <div v-else class="sys-icon">系</div>
          <div class="body">
            <div class="title-row">
              <span class="title">{{ m.title || typeLabel(m.type) }}</span>
              <span class="type-pill">{{ typeLabel(m.type) }}</span>
            </div>
            <div class="content">{{ m.content }}</div>
            <div class="meta">
              <span v-if="m.senderNickname">{{ m.senderNickname }}</span>
              <span>{{ formatTime(m.createTime) }}</span>
            </div>
          </div>
          <span v-if="m.isRead === 0" class="dot-unread" title="未读" />
        </div>
      </div>

      <div v-if="total > size" class="pager">
        <button class="btn" :disabled="current <= 1 || loading" @click="goPage(current - 1)">上一页</button>
        <span class="subtle">第 {{ current }} / {{ totalPages }} 页</span>
        <button class="btn" :disabled="current >= totalPages || loading" @click="goPage(current + 1)">下一页</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import UserAvatar from '../components/UserAvatar.vue'
import { messages, type MessageItem, type MessagePage } from '../lib/api'
import { showToast } from '../lib/toast'

const TYPE_LABELS: Record<number, string> = {
  10: '系统',
  20: '审核通过',
  21: '审核未通过',
  22: '帖子下架',
  30: '帖子评论',
  31: '评论回复',
  40: '帖子点赞',
  41: '评论点赞',
  50: '新增关注',
  60: '关注发帖',
  61: '分类新帖'
}

const router = useRouter()
const loading = ref(false)
const markingAll = ref(false)
const items = ref<MessageItem[]>([])
const unreadCount = ref(0)
const current = ref(1)
const size = ref(20)
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function typeLabel(type: number) {
  return TYPE_LABELS[type] || '通知'
}

function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString()
}

async function loadUnread() {
  const res = await messages.unreadCount()
  if (res?.code === 200 && res.data) {
    unreadCount.value = res.data.count ?? 0
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const fn = (window as any).__MESSAGES_CHANGED__
  if (typeof fn === 'function') fn()
}

async function reload() {
  loading.value = true
  const res = await messages.list(current.value, size.value)
  loading.value = false
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '获取消息失败')
    return
  }
  const page = res.data as MessagePage
  items.value = page?.records ?? []
  total.value = page?.total ?? 0
  await loadUnread()
}

function goPage(p: number) {
  current.value = p
  reload()
}

async function markAllRead() {
  markingAll.value = true
  const res = await messages.markAllRead()
  markingAll.value = false
  if (!res || res.code !== 200) {
    showToast('error', '操作失败', res?.message || '请稍后重试')
    return
  }
  showToast('success', '已全部标为已读', '')
  await reload()
}

function resolveRoute(m: MessageItem): string | null {
  if (m.targetType === 'post' && m.targetId) return `/posts/${m.targetId}`
  if (m.targetType === 'comment') {
    const postId = m.extraId ?? m.targetId
    if (postId) return `/posts/${postId}`
  }
  if (m.targetType === 'user' && m.targetId) return '/subscriptions'
  return null
}

async function onItemClick(m: MessageItem) {
  if (m.isRead === 0) {
    await messages.markRead(m.id)
    m.isRead = 1
    if (unreadCount.value > 0) unreadCount.value -= 1
    await loadUnread()
  }
  const path = resolveRoute(m)
  if (path) router.push(path)
}

onMounted(reload)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-actions { display: flex; gap: 10px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.empty { color: var(--muted); padding: 10px 0; }

.list { display: grid; gap: 10px; }
.item {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  position: relative;
}
.item:hover { border-color: rgba(37, 99, 235, 0.35); }
.item.unread { background: rgba(37, 99, 235, 0.06); }

.sys-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  flex-shrink: 0;
}

.body { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
.title { font-weight: 800; }
.type-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: rgba(37, 99, 235, 0.95);
}
.content { color: var(--text); line-height: 1.5; margin-bottom: 6px; }
.meta { display: flex; gap: 10px; font-size: 12px; color: var(--muted); flex-wrap: wrap; }

.dot-unread {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
</style>
