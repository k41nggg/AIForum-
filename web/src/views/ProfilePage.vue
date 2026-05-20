<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">个人中心</h1>
        <div class="subtle">资料、我的帖子、分类订阅与关注</div>
      </div>
      <div class="page-actions">
        <RouterLink v-if="me" class="btn" :to="`/users/${me.id}`">查看公开主页</RouterLink>
        <button class="btn" @click="reload" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'profile' }" @click="setTab('profile')">资料</button>
      <button class="tab" :class="{ active: tab === 'posts' }" @click="setTab('posts')">我的帖子</button>
      <button class="tab" :class="{ active: tab === 'categories' }" @click="setTab('categories')">分类订阅</button>
      <button class="tab" :class="{ active: tab === 'users' }" @click="setTab('users')">关注用户</button>
      <button class="tab" :class="{ active: tab === 'feed' }" @click="setTab('feed')">关注动态</button>
    </div>

    <!-- 资料 -->
    <section v-show="tab === 'profile'" class="glass card">
      <div v-if="!me" class="empty">未登录或无法获取用户信息</div>
      <div v-else class="grid">
        <div class="avatar">
          <div class="avatar-img">
            <img v-if="avatarSrc" :src="avatarSrc" alt="avatar" />
            <div v-else class="avatar-placeholder">{{ (me.nickname || me.username).slice(0, 1).toUpperCase() }}</div>
          </div>
          <label class="btn btn-upload">
            {{ uploadingAvatar ? '上传中...' : '更换头像' }}
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              hidden
              :disabled="uploadingAvatar"
              @change="onAvatarSelect"
            />
          </label>
          <div class="subtle">当前角色：{{ me.role || 'USER' }}</div>
        </div>
        <div class="form">
          <div class="row">
            <div class="field">
              <label>昵称</label>
              <input class="input" v-model.trim="form.nickname" placeholder="昵称" />
            </div>
            <div class="field">
              <label>个人简介</label>
              <textarea class="textarea" v-model.trim="form.bio" rows="5" placeholder="介绍一下自己..." />
            </div>
            <button class="btn btn-primary" @click="save" :disabled="saving">
              {{ saving ? '保存中...' : '保存资料' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 我的帖子 -->
    <section v-show="tab === 'posts'" class="glass card">
      <div class="section-head">
        <h2 class="h2">我的帖子</h2>
        <span class="pill">共 {{ myPosts.length }} 篇</span>
      </div>
      <div v-if="postsLoading" class="subtle">加载中...</div>
      <div v-else-if="myPosts.length === 0" class="empty">你还没有发布过帖子</div>
      <div v-else>
        <div class="post" v-for="p in myPosts" :key="p.id">
          <div class="post-title" @click="goDetail(p.id)">{{ p.title }}</div>
          <div class="post-meta-row">
            <div class="post-meta">
              <span class="pill" :class="getStatusClass(p.status)">{{ getStatusText(p.status) }}</span>
              <span class="pill">浏览 {{ p.viewCount }}</span>
              <span class="pill">点赞 {{ p.likeCount }}</span>
              <span class="pill">{{ new Date(p.createTime).toLocaleString() }}</span>
            </div>
            <button class="btn btn-danger btn-compact" @click="removeMyPost(p.id)" :disabled="deletingId === p.id">
              {{ deletingId === p.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 分类订阅 -->
    <section v-show="tab === 'categories'" class="glass card">
      <div class="section-head">
        <h2 class="h2">分类订阅</h2>
        <span class="pill">共 {{ categories.length }} 个</span>
      </div>
      <div v-if="loadingCategories" class="subtle">加载中...</div>
      <div v-else-if="categories.length === 0" class="empty">你还没有订阅任何分类，可在帖子详情或分类页订阅</div>
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
          <UserAvatar :avatar="u.avatar" :name="userDisplayName(u)" :user-id="u.id" size="md" />
          <div class="user-info">
            <div class="title link-name" @click="goUser(u.id)">{{ userDisplayName(u) }}</div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostPreviewContent from '../components/PostPreviewContent.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { apiDelete, apiGet, apiPut, apiUpload, userFollow, type ApiResult, type UserSummary } from '../lib/api'
import { authorLabel, normalizePostAuthor } from '../lib/postDisplay'
import { showToast } from '../lib/toast'

const TAB_KEYS = ['profile', 'posts', 'categories', 'users', 'feed'] as const
type TabKey = (typeof TAB_KEYS)[number]

type User = {
  id: number
  username: string
  nickname?: string
  avatar?: string
  bio?: string
  role?: string
}

type Post = {
  id: number
  title: string
  status: string
  viewCount: number
  likeCount: number
  createTime: string
}

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

type Page<T> = { records: T[] }

const route = useRoute()
const router = useRouter()

function parseTab(q: unknown): TabKey {
  const t = String(q || '')
  return TAB_KEYS.includes(t as TabKey) ? (t as TabKey) : 'profile'
}

const tab = ref<TabKey>(parseTab(route.query.tab))
const loading = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)
const me = ref<User | null>(null)
const postsLoading = ref(false)
const myPosts = ref<Post[]>([])
const deletingId = ref<number | null>(null)

const loadingCategories = ref(false)
const loadingUsers = ref(false)
const loadingFeed = ref(false)
const categories = ref<Category[]>([])
const followingUsers = ref<UserSummary[]>([])
const feedPosts = ref<FeedPost[]>([])
const unsubscribingCategoryId = ref<number | null>(null)
const unfollowingUserId = ref<number | null>(null)

const form = reactive({
  nickname: '',
  avatar: '',
  bio: ''
})

const avatarSrc = computed(() => {
  const url = form.avatar?.trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
})

function setTab(t: TabKey) {
  tab.value = t
}

function userDisplayName(u: UserSummary) {
  return u.nickname?.trim() || `用户${u.id}`
}

async function onAvatarSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploadingAvatar.value = true
  const res = await apiUpload(file)
  uploadingAvatar.value = false

  if (!res) {
    showToast('error', '上传失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200 || !res.data?.url) {
    showToast('error', '上传失败', res.message || '上传失败')
    return
  }

  form.avatar = res.data.url
  await save()
}

async function loadProfile() {
  loading.value = true
  const res = await apiGet<ApiResult<User>>('/users/me')
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    me.value = null
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '未登录')
    me.value = null
    return
  }

  me.value = res.data
  form.nickname = res.data.nickname || ''
  form.avatar = res.data.avatar || ''
  form.bio = res.data.bio || ''
}

async function loadMyPosts() {
  postsLoading.value = true
  const res = await apiGet<ApiResult<Page<Post>>>('/posts/my')
  postsLoading.value = false
  if (res?.code === 200) myPosts.value = res.data.records || []
}

async function loadCategories() {
  loadingCategories.value = true
  const res = await apiGet<ApiResult<Category[]>>('/subscriptions/me')
  loadingCategories.value = false
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

async function loadTabData(t: TabKey) {
  if (t === 'posts') await loadMyPosts()
  else if (t === 'categories') await loadCategories()
  else if (t === 'users') await loadFollowingUsers()
  else if (t === 'feed') await loadFeed()
}

async function save() {
  saving.value = true
  const res = await apiPut<ApiResult<string>>('/users/profile', {
    nickname: form.nickname || null,
    avatar: form.avatar || null,
    bio: form.bio || null
  })
  saving.value = false

  if (!res) {
    showToast('error', '保存失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '保存失败', res.message || '保存失败')
    return
  }

  showToast('success', '保存成功', '资料已更新')
  await loadProfile()
}

async function removeMyPost(id: number) {
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

  showToast('success', '删除成功', '帖子已删除')
  await loadMyPosts()
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

async function reload() {
  await loadProfile()
  await loadTabData(tab.value)
}

function goDetail(id: number) {
  router.push(`/posts/${id}`)
}

function goUser(id: number) {
  router.push(`/users/${id}`)
}

function getStatusText(status: string) {
  switch (status) {
    case 'AUDIT_PENDING': return '待审核'
    case 'PUBLISHED': return '已发布'
    case 'DELETED': return '已删除'
    default: return status
  }
}

function getStatusClass(status: string) {
  switch (status) {
    case 'AUDIT_PENDING': return 'status-pending'
    case 'PUBLISHED': return 'status-published'
    case 'DELETED': return 'status-deleted'
    default: return ''
  }
}

watch(tab, (t) => {
  if (route.path === '/profile' && route.query.tab !== t) {
    router.replace({ path: '/profile', query: { tab: t } })
  }
  loadTabData(t)
})

watch(
  () => route.query.tab,
  (q) => {
    const next = parseTab(q)
    if (next !== tab.value) tab.value = next
  }
)

onMounted(async () => {
  tab.value = parseTab(route.query.tab)
  await loadProfile()
  await loadTabData(tab.value)
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
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

.grid { display: grid; grid-template-columns: 220px 1fr; gap: 16px; align-items: start; }
.avatar { display: grid; gap: 10px; }
.avatar-img {
  width: 140px;
  height: 140px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-sm);
}
.avatar-img img { width: 100%; height: 100%; object-fit: cover; }
.btn-upload {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 48px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(29, 78, 216, 0.1));
  color: rgba(15, 23, 42, 0.8);
}

.form { flex: 1; }
.row { display: flex; flex-direction: column; gap: 16px; }

.list { display: grid; gap: 10px; }
.item {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.user-item { align-items: center; }
.user-info { flex: 1; min-width: 0; }
.title { font-weight: 800; }
.link-name { cursor: pointer; }
.link-name:hover { color: var(--primary); }

.post { padding: 14px 0; border-top: 1px solid var(--border); }
.post:first-of-type { border-top: none; padding-top: 0; }
.post-title { font-weight: 750; letter-spacing: 0.2px; margin-bottom: 8px; cursor: pointer; }
.post-title:hover { color: var(--primary); }
.post-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.post-meta-row {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}
.post :deep(.post-preview) { margin-bottom: 10px; }

.btn-danger {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.08);
  color: rgba(185, 28, 28, 0.95);
}
.btn-danger:hover { background: rgba(239, 68, 68, 0.12); }
.btn-compact {
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.2;
  min-height: 28px;
  border-radius: 10px;
  white-space: nowrap;
}

.status-pending { background-color: #f59e0b; color: white; }
.status-published { background-color: #10b981; color: white; }
.status-deleted { background-color: #ef4444; color: white; }

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .post-meta-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
