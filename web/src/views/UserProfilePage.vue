<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">用户主页</h1>
        <div class="subtle">查看 TA 的资料与已发布帖子</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="reload" :disabled="loading">刷新</button>
        <button class="btn" @click="router.back()">返回</button>
      </div>
    </div>

    <section class="glass card" v-if="user">
      <div class="profile-head">
        <UserAvatar :avatar="user.avatar" :name="displayName" size="lg" />
        <div class="profile-main">
          <div class="profile-title-row">
            <h2 class="profile-name">{{ displayName }}</h2>
            <button
              v-if="canFollow"
              type="button"
              class="btn btn-follow"
              :class="{ 'btn-follow--active': following }"
              @click="toggleFollow"
              :disabled="followLoading"
            >
              {{ followLoading ? '...' : following ? '已关注' : '+ 关注' }}
            </button>
            <RouterLink v-if="isSelf" class="btn" to="/profile">编辑我的资料</RouterLink>
          </div>
          <div v-if="user.bio" class="bio">{{ user.bio }}</div>
          <div v-else class="subtle">暂无个人简介</div>
        </div>
      </div>
    </section>

    <section class="glass card" v-else-if="!loading">
      <div class="empty">用户不存在或无法加载</div>
    </section>

    <section class="glass card" v-if="user">
      <div class="section-head">
        <h2 class="h2">已发布帖子</h2>
        <span class="pill">共 {{ posts.length }} 篇</span>
      </div>
      <div v-if="postsLoading" class="subtle">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">暂无已发布的帖子</div>
      <div v-else>
        <div class="post" v-for="p in posts" :key="p.id">
          <div class="post-title" @click="goDetail(p.id)">{{ p.title }}</div>
          <div class="post-meta">
            <span class="pill">浏览 {{ p.viewCount }}</span>
            <span class="pill">点赞 {{ p.likeCount }}</span>
            <span class="pill">{{ formatTime(p.createTime) }}</span>
          </div>
          <PostPreviewContent :content="p.content" />
          <button class="btn" @click="goDetail(p.id)">查看详情</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostPreviewContent from '../components/PostPreviewContent.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { apiGet, getToken, userFollow, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type User = {
  id: number
  username: string
  nickname?: string
  avatar?: string
  bio?: string
}

type Post = {
  id: number
  title: string
  content: string
  viewCount: number
  likeCount: number
  createTime?: string
}

type Page<T> = { records: T[] }

const route = useRoute()
const router = useRouter()
const userId = computed(() => Number(route.params.id))

const loading = ref(false)
const postsLoading = ref(false)
const user = ref<User | null>(null)
const posts = ref<Post[]>([])
const meId = ref<number | null>(null)
const following = ref(false)
const followLoading = ref(false)

const displayName = computed(() => {
  if (!user.value) return '用户'
  return user.value.nickname?.trim() || user.value.username || '用户'
})

const isSelf = computed(() => meId.value != null && user.value != null && meId.value === user.value.id)
const canFollow = computed(() => getToken() && user.value && !isSelf.value)

async function loadMe() {
  if (!getToken()) {
    meId.value = null
    return
  }
  const res = await apiGet<ApiResult<{ id: number }>>('/users/me')
  meId.value = res?.code === 200 ? res.data.id : null
}

async function loadUser() {
  if (!userId.value || Number.isNaN(userId.value)) {
    user.value = null
    return
  }
  loading.value = true
  const res = await apiGet<ApiResult<User>>(`/users/${userId.value}`)
  loading.value = false
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    user.value = null
    return
  }
  if (res.code !== 200 || !res.data) {
    user.value = null
    showToast('error', '加载失败', res.message || '用户不存在')
    return
  }
  user.value = res.data
}

async function loadFollowState() {
  if (!canFollow.value || !user.value) {
    following.value = false
    return
  }
  const res = await userFollow.check(user.value.id)
  following.value = res?.code === 200 && Boolean(res.data?.following)
}

async function loadPosts() {
  if (!userId.value || Number.isNaN(userId.value)) return
  postsLoading.value = true
  const res = await apiGet<ApiResult<Page<Post>>>(`/posts?userId=${userId.value}&current=1&size=50`)
  postsLoading.value = false
  if (res?.code === 200) posts.value = res.data?.records ?? []
  else posts.value = []
}

async function toggleFollow() {
  if (!user.value || !canFollow.value) return
  followLoading.value = true
  const res = following.value
    ? await userFollow.unfollow(user.value.id)
    : await userFollow.follow(user.value.id)
  followLoading.value = false
  if (!res || res.code !== 200) {
    showToast('error', '操作失败', res?.message || '请稍后重试')
    return
  }
  following.value = !following.value
  showToast('success', following.value ? '已关注' : '已取消关注', '')
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? t : d.toLocaleString()
}

async function reload() {
  await loadUser()
  await loadFollowState()
  await loadPosts()
}

watch(userId, () => {
  reload()
})

onMounted(async () => {
  await loadMe()
  await reload()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-actions { display: flex; gap: 10px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.empty { color: var(--muted); padding: 10px 0; }

.profile-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.profile-main { flex: 1; min-width: 0; }
.profile-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.profile-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
}
.bio {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.btn-follow {
  padding: 4px 14px;
  font-size: 13px;
  min-height: 30px;
  border-radius: 999px;
  border: 1px solid rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.1);
  color: rgba(29, 78, 216, 0.95);
}
.btn-follow--active {
  border-color: rgba(15, 23, 42, 0.18);
  background: rgba(15, 23, 42, 0.06);
  color: rgba(15, 23, 42, 0.65);
}

.post { padding: 14px 0; border-top: 1px solid rgba(15, 23, 42, 0.1); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; margin-bottom: 8px; cursor: pointer; }
.post-title:hover { color: var(--primary); }
.post-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.post :deep(.post-preview) { margin-bottom: 10px; }
</style>
