<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">我的订阅</h1>
        <div class="subtle">管理分类订阅与用户关注</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="reload" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'categories' }" @click="tab = 'categories'">分类</button>
      <button class="tab" :class="{ active: tab === 'users' }" @click="tab = 'users'">关注用户</button>
      <button class="tab" :class="{ active: tab === 'feed' }" @click="tab = 'feed'">关注动态</button>
    </div>

    <!-- 分类订阅 -->
    <section v-show="tab === 'categories'" class="glass card">
      <div class="section-head">
        <h2 class="h2">分类订阅</h2>
        <span class="pill">共 {{ categories.length }} 个</span>
      </div>
      <div v-if="loading" class="subtle">加载中...</div>
      <div v-else-if="categories.length === 0" class="empty">你还没有订阅任何分类</div>
      <div class="list" v-else>
        <div class="item" v-for="c in categories" :key="c.id">
          <div>
            <div class="title">{{ c.name }}</div>
            <div class="subtle" v-if="c.description">{{ c.description }}</div>
          </div>
          <button class="btn" @click="unsubscribeCategory(c.id)" :disabled="unsubscribingCategoryId === c.id">
            {{ unsubscribingCategoryId === c.id ? '处理中...' : '取消订阅' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 关注用户 -->
    <section v-show="tab === 'users'" class="glass card">
      <div class="section-head">
        <h2 class="h2">关注的用户</h2>
        <span class="pill">共 {{ followingUsers.length }} 人</span>
      </div>
      <div v-if="loadingUsers" class="subtle">加载中...</div>
      <div v-else-if="followingUsers.length === 0" class="empty">你还没有关注任何用户，可在帖子详情页关注作者</div>
      <div class="list" v-else>
        <div class="item user-item" v-for="u in followingUsers" :key="u.id">
          <UserAvatar :avatar="u.avatar" :name="userDisplayName(u)" size="md" />
          <div class="user-info">
            <div class="title">{{ userDisplayName(u) }}</div>
            <div class="subtle" v-if="u.bio">{{ u.bio }}</div>
          </div>
          <button class="btn" @click="unfollowUser(u.id)" :disabled="unfollowingUserId === u.id">
            {{ unfollowingUserId === u.id ? '处理中...' : '取消关注' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 关注动态 -->
    <section v-show="tab === 'feed'" class="glass card">
      <div class="section-head">
        <h2 class="h2">关注动态</h2>
        <span class="pill">共 {{ feedPosts.length }} 篇</span>
      </div>
      <div v-if="loadingFeed" class="subtle">加载中...</div>
      <div v-else-if="feedPosts.length === 0" class="empty">暂无动态，先去关注一些用户吧</div>
      <div v-else>
        <div class="post" v-for="p in feedPosts" :key="p.id">
          <div class="post-title" @click="goDetail(p.id)">{{ p.title }}</div>
          <div class="post-meta">
            <span class="pill">作者 {{ authorLabel(p) }}</span>
            <span class="pill">浏览 {{ p.viewCount }}</span>
            <span class="pill">点赞 {{ p.likeCount }}</span>
          </div>
          <PostPreviewContent :content="p.content" />
          <button class="btn" @click="goDetail(p.id)">查看详情</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { apiDelete, apiGet, userFollow, type ApiResult, type UserSummary } from '../lib/api'
import PostPreviewContent from '../components/PostPreviewContent.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { authorLabel, normalizePostAuthor } from '../lib/postDisplay'
import { showToast } from '../lib/toast'

type Category = {
  id: number
  name: string
  description?: string
  parentId: number
}

type FeedPost = {
  id: number
  title: string
  content: string
  viewCount: number
  likeCount: number
  userNickname?: string
  user_nickname?: string
}

const tab = ref<'categories' | 'users' | 'feed'>('categories')
const loading = ref(false)
const loadingUsers = ref(false)
const loadingFeed = ref(false)
const categories = ref<Category[]>([])
const followingUsers = ref<UserSummary[]>([])
const feedPosts = ref<FeedPost[]>([])
const unsubscribingCategoryId = ref<number | null>(null)
const unfollowingUserId = ref<number | null>(null)
const router = useRouter()

function userDisplayName(u: UserSummary) {
  return u.nickname?.trim() || `用户${u.id}`
}

async function loadCategories() {
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

async function loadFollowingUsers() {
  loadingUsers.value = true
  const res = await userFollow.listFollowing()
  loadingUsers.value = false
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    followingUsers.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '加载失败')
    followingUsers.value = []
    return
  }
  followingUsers.value = res.data || []
}

async function loadFeed() {
  loadingFeed.value = true
  const res = await userFollow.feed(20)
  loadingFeed.value = false
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    feedPosts.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '加载失败')
    feedPosts.value = []
    return
  }
  feedPosts.value = (res.data || []).map(normalizePostAuthor)
}

async function reload() {
  await loadCategories()
  if (tab.value === 'users') await loadFollowingUsers()
  if (tab.value === 'feed') await loadFeed()
}

async function unsubscribeCategory(categoryId: number) {
  unsubscribingCategoryId.value = categoryId
  const res = await apiDelete<ApiResult<null>>(`/subscriptions/${categoryId}`)
  unsubscribingCategoryId.value = null
  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '取消订阅失败')
    return
  }
  showToast('success', '已取消订阅', '')
  await loadCategories()
}

async function unfollowUser(userId: number) {
  unfollowingUserId.value = userId
  const res = await userFollow.unfollow(userId)
  unfollowingUserId.value = null
  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '取消关注失败')
    return
  }
  showToast('success', '已取消关注', '')
  await loadFollowingUsers()
  if (feedPosts.value.length) await loadFeed()
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

watch(tab, (t) => {
  if (t === 'users' && followingUsers.value.length === 0 && !loadingUsers.value) loadFollowingUsers()
  if (t === 'feed' && feedPosts.value.length === 0 && !loadingFeed.value) loadFeed()
})

onMounted(async () => {
  await loadCategories()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }
.empty { color: var(--muted); padding: 10px 0; }

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tab {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
}
.tab.active {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.35);
  font-weight: 700;
}

.list { display: grid; gap: 10px; }
.item {
  border: 1px solid rgba(15,23,42,0.10);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255,255,255,0.70);
  display:flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.user-item { align-items: center; }
.user-info { flex: 1; min-width: 0; }
.title { font-weight: 800; }

.post { padding: 14px 0; border-top: 1px solid rgba(15, 23, 42, 0.10); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; margin-bottom: 8px; cursor: pointer; }
.post-title:hover { color: var(--primary); }
.post-meta { display:flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.post :deep(.post-preview) { margin-bottom: 10px; }
</style>
