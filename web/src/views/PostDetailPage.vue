<template>
  <div class="page-layout">
    <div class="main-content">
      <div class="page-head">
        <div>
          <h1 class="h1">帖子详情</h1>
          <div class="subtle">阅读内容、参与评论交流</div>
        </div>
        <div class="page-actions">
          <button class="btn" @click="reload" :disabled="loading">刷新</button>
          <RouterLink class="btn" to="/posts">返回列表</RouterLink>
        </div>
      </div>

      <section class="glass card" v-if="post">
        <div class="post-title">{{ post.title }}</div>
        <div v-if="post.categoryId" class="post-category-row">
          <span class="category-badge">{{ getCategoryName(post.categoryId) }}</span>
          <button
            v-if="getToken()"
            type="button"
            class="btn btn-category-sub"
            :class="{ 'btn-category-sub--active': subscribedToCategory }"
            @click="toggleCategorySubscribe"
            :disabled="categorySubLoading"
          >
            {{ categorySubLoading ? '...' : subscribedToCategory ? '已订阅分类' : '+ 订阅分类' }}
          </button>
          <span v-else class="subtle category-hint">登录后可订阅该分类</span>
        </div>
        <div class="post-author-row">
          <UserAvatar :avatar="pickUserAvatar(post)" :name="authorLabel(post)" size="lg" />
          <div class="post-author-main">
            <div class="post-author-top">
              <span class="post-author-name">{{ authorLabel(post) }}</span>
              <button
                v-if="canFollowAuthor()"
                type="button"
                class="btn btn-follow"
                :class="{ 'btn-follow--active': followingAuthor }"
                @click="toggleFollowAuthor"
                :disabled="followLoading"
              >
                {{ followLoading ? '...' : followingAuthor ? '已关注' : '+ 关注' }}
              </button>
            </div>
            <div class="post-meta">
              <span class="pill">浏览 {{ post.viewCount }}</span>
              <span class="pill">点赞 {{ post.likeCount }}</span>
            </div>
          </div>
        </div>
        <div class="post-content markdown-body" v-html="postContentHtml" />
        <div class="post-actions">
          <button class="btn" @click="likePost" :disabled="liking">{{ liking ? '处理中...' : '点赞' }}</button>
          <button class="btn" v-if="canDeletePost()" @click="deletePost" :disabled="deletingPost">
            {{ deletingPost ? '删除中...' : '删除帖子' }}
          </button>
        </div>
      </section>

      <section class="glass card" v-else>
        <div class="subtle">{{ loading ? '加载中...' : '帖子不存在或已被删除' }}</div>
      </section>

      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">评论</h2>
          <span class="pill">共 {{ comments.length }} 条</span>
        </div>

        <div class="row">
          <div class="field">
            <label>发表评论</label>
            <textarea class="textarea" v-model.trim="newComment" rows="4" placeholder="写下你的看法..." />
          </div>
          <button class="btn btn-primary" @click="submitComment" :disabled="commenting">
            {{ commenting ? '提交中...' : '发布评论' }}
          </button>
        </div>

        <div class="comments" v-if="comments.length > 0">
          <CommentNode
            v-for="c in comments"
            :key="c.id"
            :comment="c"
            :level="0"
            @like="likeComment"
            @reply="setReplyTo"
          />
        </div>
        <div v-else class="empty">暂无评论</div>

        <div v-if="replyTo" class="reply-box">
          <div class="reply-to-row">
            <UserAvatar
              :avatar="replyTo.userAvatar"
              :name="replyTo.userNickname?.trim() || '未知用户'"
              size="sm"
            />
            <span class="subtle">回复 {{ replyTo.userNickname?.trim() || '未知用户' }}</span>
          </div>
          <div class="row" style="margin-top: 10px">
            <textarea class="textarea" v-model.trim="replyContent" rows="3" placeholder="写下回复内容..." />
            <div style="display:flex; gap: 10px; justify-content:flex-end;">
              <button class="btn" @click="cancelReply">取消</button>
              <button class="btn btn-primary" @click="submitReply" :disabled="replying">
                {{ replying ? '提交中...' : '提交回复' }}
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <aside class="sidebar">
      <section class="glass card ai-card">
        <div class="section-head">
          <h2 class="h2">AI 问答</h2>
        </div>
        <div class="field">
          <label>提问（会附带帖子正文作为上下文）</label>
          <input class="input" v-model.trim="question" placeholder="输入你的问题..." @keyup.enter="askQuestion" />
        </div>
        <div class="ai-controls">
          <button class="btn" @click="getSummary" :disabled="summarizing">{{ summarizing ? '读取中...' : '查看AI总结' }}</button>
          <button class="btn btn-primary" @click="askQuestion" :disabled="asking">
            {{ asking ? '思考中...' : '提问' }}
          </button>
        </div>
        <div v-if="aiResults.length > 0" class="ai-answer">
          <div v-for="(result, index) in aiResults" :key="index" class="ai-result-item">
            <template v-if="result.type === 'summary'">
              <strong>AI 总结:</strong>
              <p class="ai-text">{{ result.content }}</p>
            </template>
            <template v-else-if="result.type === 'qa'">
              <div class="qa-pair">
                <strong>提问:</strong>
                <p class="ai-text">{{ result.content.question }}</p>
                <strong>AI 回答:</strong>
                <p class="ai-text">{{ result.content.answer }}</p>
              </div>
            </template>
          </div>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { renderMarkdown } from '../lib/markdown'
import { useRoute, useRouter } from 'vue-router'
import { apiDelete, apiGet, apiPost, getToken, userFollow, type ApiResult, comment as commentApi } from '../lib/api'
import { pickUserAvatar } from '../lib/avatar'
import { showToast } from '../lib/toast'
import CommentNode from '../components/CommentNode.vue'
import UserAvatar from '../components/UserAvatar.vue'

type Post = {
  id: number
  userId: number
  userNickname?: string
  userAvatar?: string
  categoryId: number
  title: string
  content: string
  viewCount: number
  likeCount: number
}

type Category = {
  id: number
  name: string
}

type UserBrief = {
  id: number
  avatar?: string
  nickname?: string
}

type Comment = {
  id: number
  postId: number
  userId: number
  userNickname?: string
  userAvatar?: string
  parentId: number
  rootId: number
  content: string
  likeCount: number
  createTime: string
  children?: Comment[]
}

type Me = {
  id: number
  username: string
  role?: string
}

const route = useRoute()
const router = useRouter()
const postId = computed(() => Number(route.params.id))
const postContentHtml = computed(() => (post.value ? renderMarkdown(post.value.content) : ''))

const loading = ref(false)
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])
const categories = ref<Category[]>([])

const newComment = ref('')
const commenting = ref(false)

const replyTo = ref<Comment | null>(null)
const replyContent = ref('')
const replying = ref(false)

const liking = ref(false)

const summarizing = ref(false)
const asking = ref(false)
const question = ref('')
const aiResults = ref<{ type: 'summary' | 'qa'; content: any }[]>([])

const me = ref<Me | null>(null)
const deletingPost = ref(false)
const followingAuthor = ref(false)
const followLoading = ref(false)
const subscribedCategoryIds = ref<Set<number>>(new Set())
const categorySubLoading = ref(false)
const userAvatarCache = new Map<number, string>()

const subscribedToCategory = computed(() => {
  if (!post.value?.categoryId) return false
  return subscribedCategoryIds.value.has(post.value.categoryId)
})

async function fetchUserAvatar(userId: number): Promise<string> {
  if (userAvatarCache.has(userId)) return userAvatarCache.get(userId)!
  const res = await apiGet<ApiResult<UserBrief>>(`/users/${userId}`)
  const av = res?.code === 200 ? pickUserAvatar(res.data) : ''
  userAvatarCache.set(userId, av)
  return av
}

async function ensureUserAvatar<T extends { userId: number; userAvatar?: string }>(item: T): Promise<T> {
  let userAvatar = pickUserAvatar(item)
  if (!userAvatar) userAvatar = await fetchUserAvatar(item.userId)
  return { ...item, userAvatar: userAvatar || undefined }
}

async function ensureCommentAvatars(list: Comment[]): Promise<Comment[]> {
  return Promise.all(
    list.map(async (c) => {
      const base = await ensureUserAvatar(c)
      if (base.children?.length) {
        base.children = await ensureCommentAvatars(base.children)
      }
      return base
    })
  )
}

function authorLabel(p: Post) {
  const name = p.userNickname?.trim()
  return name || '未知用户'
}

function getCategoryName(id: number) {
  const cat = categories.value.find((c) => c.id === id)
  return cat ? cat.name : '未知分类'
}

async function loadCategories() {
  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  if (res?.code === 200) categories.value = res.data || []
}

async function loadSubscribedCategories() {
  if (!getToken()) {
    subscribedCategoryIds.value = new Set()
    return
  }
  const res = await apiGet<ApiResult<Category[]>>('/subscriptions/me')
  if (res?.code === 200) {
    subscribedCategoryIds.value = new Set((res.data || []).map((c) => c.id))
  }
}

async function toggleCategorySubscribe() {
  if (!post.value?.categoryId) return
  if (!getToken()) {
    showToast('error', '需要登录', '请先登录后再订阅分类')
    return
  }
  const categoryId = post.value.categoryId
  categorySubLoading.value = true
  const res = subscribedToCategory.value
    ? await apiDelete<ApiResult<null>>(`/subscriptions/${categoryId}`)
    : await apiPost<ApiResult<null>>('/subscriptions', { categoryId })
  categorySubLoading.value = false

  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    if (!subscribedToCategory.value && res.message === '已订阅') {
      subscribedCategoryIds.value = new Set([...subscribedCategoryIds.value, categoryId])
      showToast('success', '已订阅', '你已关注该分类')
      return
    }
    showToast('error', '操作失败', res.message || '操作失败')
    return
  }

  const next = new Set(subscribedCategoryIds.value)
  if (subscribedToCategory.value) next.delete(categoryId)
  else next.add(categoryId)
  subscribedCategoryIds.value = next
  showToast('success', subscribedToCategory.value ? '订阅成功' : '已取消订阅', '')
}

async function loadPost() {
  loading.value = true
  const res = await apiGet<ApiResult<Post>>(`/posts/${postId.value}`)
  loading.value = false

  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    post.value = null
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '帖子加载失败')
    post.value = null
    return
  }
  post.value = await ensureUserAvatar(res.data)
  await loadFollowStatus()
}

async function loadFollowStatus() {
  followingAuthor.value = false
  if (!post.value || !me.value || post.value.userId === me.value.id) return
  const res = await userFollow.check(post.value.userId)
  if (res?.code === 200) followingAuthor.value = Boolean(res.data?.following)
}

function canFollowAuthor() {
  return Boolean(getToken() && post.value && me.value && post.value.userId !== me.value.id)
}

async function toggleFollowAuthor() {
  if (!post.value || !me.value) return
  followLoading.value = true
  const authorId = post.value.userId
  const res = followingAuthor.value
    ? await userFollow.unfollow(authorId)
    : await userFollow.follow(authorId)
  followLoading.value = false
  if (!res) {
    showToast('error', '操作失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '操作失败', res.message || '操作失败')
    return
  }
  followingAuthor.value = !followingAuthor.value
  showToast('success', followingAuthor.value ? '关注成功' : '已取消关注', '')
}

async function loadComments() {
  const res = await commentApi.getComments(postId.value)
  if (!res) {
    showToast('error', '加载失败', '无法连接后端服务')
    comments.value = []
    return
  }
  if (res.code !== 200) {
    showToast('error', '加载失败', res.message || '评论加载失败')
    comments.value = []
    return
  }
  comments.value = await ensureCommentAvatars(res.data || [])
}

async function loadMe() {
  const res = await apiGet<ApiResult<Me>>('/users/me')
  if (res?.code === 200) me.value = res.data
  else me.value = null
}

async function reload() {
  await loadSubscribedCategories()
  await loadPost()
  await loadComments()
}

async function likePost() {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能点赞')
    return
  }
  liking.value = true
  const res = await apiPost<ApiResult<null>>(`/posts/${postId.value}/like`)
  liking.value = false

  if (!res) {
    showToast('error', '点赞失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '点赞失败', res.message || '点赞失败')
    return
  }
  showToast('success', '点赞成功', '感谢你的支持')
  await loadPost()
}

async function likeComment(commentId: number) {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能点赞')
    return
  }
  const res = await commentApi.likeComment(commentId)
  if (!res) {
    showToast('error', '点赞失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '点赞失败', res.message || '点赞失败')
    return
  }
  showToast('success', '点赞成功', '已记录你的点赞')
  await loadComments()
}

async function submitComment() {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能发表评论')
    return
  }
  if (!newComment.value) {
    showToast('error', '发布失败', '评论内容不能为空')
    return
  }

  commenting.value = true
  const res = await commentApi.addComment({
    postId: postId.value,
    content: newComment.value
  })
  commenting.value = false

  if (!res) {
    showToast('error', '发布失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '发布失败', res.message || '发布失败')
    return
  }
  showToast('success', '发布成功', '评论已发布')
  newComment.value = ''
  await loadComments()
}

function setReplyTo(c: Comment) {
  replyTo.value = c
  replyContent.value = ''
}

function cancelReply() {
  replyTo.value = null
}

async function submitReply() {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能回复')
    return
  }
  if (!replyContent.value) {
    showToast('error', '回复失败', '回复内容不能为空')
    return
  }
  if (!replyTo.value) {
    showToast('error', '回复失败', '未指定回复对象')
    return
  }

  replying.value = true
  const res = await commentApi.addComment({
    postId: postId.value,
    content: replyContent.value,
    parentId: replyTo.value.id
  })
  replying.value = false

  if (!res) {
    showToast('error', '回复失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '回复失败', res.message || '回复失败')
    return
  }
  showToast('success', '回复成功', '回复已发布')
  cancelReply()
  await loadComments()
}

async function getSummary() {
  if (!post.value) return
  summarizing.value = true
  try {
    const res = await apiPost<string>(`/ai/summary/${post.value.id}`)
    if (res) {
      aiResults.value.push({ type: 'summary', content: res })
    } else {
      showToast('error', '获取失败', '未能获取到AI总结（可能还未审核生成）')
    }
  } catch (e: any) {
    showToast('error', '获取失败', e.message || '请求失败')
  } finally {
    summarizing.value = false
  }
}

async function askQuestion() {
  if (!question.value) {
    showToast('error', '问题不能为空', '请输入你的问题')
    return
  }
  if (!post.value) return

  asking.value = true
  const currentQuestion = question.value
  question.value = ''
  try {
    // 后端接收 PostQaRequest：{ question: string }
    const res = await apiPost<string>(`/ai/qa/${post.value.id}`, { question: currentQuestion })
    if (res) {
      aiResults.value.push({ type: 'qa', content: { question: currentQuestion, answer: res } })
    } else {
      showToast('error', '提问失败', '未能获取到AI回答')
    }
  } catch (e: any) {
    showToast('error', '提问失败', e.message || '请求失败')
  } finally {
    asking.value = false
  }
}

function canDeletePost() {
  if (!post.value || !me.value) return false
  if (me.value.role === 'ADMIN') return true
  return me.value.id === post.value.userId
}

async function deletePost() {
  if (!post.value) return
  deletingPost.value = true
  const res = await apiDelete<ApiResult<null>>(`/posts/${post.value.id}`)
  deletingPost.value = false

  if (!res) {
    showToast('error', '删除失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '删除失败', res.message || '删除失败')
    return
  }

  showToast('success', '删除成功', '帖子已删除')
  router.push('/posts')
}

// 权限判断 (示例)
function canDelete(c: Comment) {
  // 实际项目中应从 token 解析出当前用户 ID 和角色
  return false
}

onMounted(async () => {
  await loadCategories()
  await loadMe()
  await loadSubscribedCategories()
  await reload()
})
</script>

<style scoped>
.page-layout {
  display: grid;
  grid-template-columns: 1fr 450px;
  gap: 20px;
}

.main-content {
  display: grid;
  gap: 16px;
  align-content: start;
}

.sidebar {
  position: sticky;
  top: 20px;
  height: fit-content;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.post-title {
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 12px;
}

.post-category-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.category-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 650;
  color: rgba(29, 78, 216, 0.95);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(37, 99, 235, 0.2);
}

.btn-category-sub {
  padding: 4px 14px;
  font-size: 13px;
  min-height: 30px;
  border-radius: 999px;
  border: 1px solid rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.12);
  color: rgba(29, 78, 216, 0.95);
}

.btn-category-sub:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.2);
}

.btn-category-sub--active {
  border-color: rgba(15, 23, 42, 0.18);
  background: rgba(15, 23, 42, 0.06);
  color: rgba(15, 23, 42, 0.65);
}

.category-hint {
  font-size: 0.85rem;
}

.post-author-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 20px;
}

.post-author-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.post-author-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.post-author-name {
  font-weight: 700;
  font-size: 1.05rem;
  line-height: 1.3;
}

.btn-follow {
  padding: 4px 14px;
  font-size: 13px;
  line-height: 1.2;
  min-height: 30px;
  border-radius: 999px;
  flex-shrink: 0;
  border: 1px solid rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.1);
  color: rgba(29, 78, 216, 0.95);
}

.btn-follow:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.18);
}

.btn-follow--active {
  border-color: rgba(15, 23, 42, 0.18);
  background: rgba(15, 23, 42, 0.06);
  color: rgba(15, 23, 42, 0.65);
}

.btn-follow--active:hover:not(:disabled) {
  background: rgba(15, 23, 42, 0.1);
}

.post-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.post-content {
  color: rgba(15, 23, 42, 0.88);
  line-height: 1.8;
  margin-bottom: 20px;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 12px 0;
}

.markdown-body :deep(p) {
  margin: 0.6em 0;
}

.markdown-body :deep(pre) {
  overflow-x: auto;
  padding: 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.06);
}

.post-actions {
  text-align: right;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.post-summary {
  margin-top: 20px;
  padding: 15px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.8;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.field {
  flex-grow: 1;
}

.comments {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.empty {
  text-align: center;
  padding: 40px;
  color: #888;
}

.reply-box {
  margin-top: 20px;
  padding: 15px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
}

.reply-to-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 40px);
}

.ai-controls {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.ai-controls .btn {
  flex-grow: 1;
}

.ai-answer {
  flex: 1;
  min-height: 120px;
  overflow-y: auto;
}

.ai-result-item {
  margin-bottom: 15px;
}

.ai-result-item:last-child {
  margin-bottom: 0;
}

.ai-text {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.qa-pair {
  margin-bottom: 15px;
}

.qa-pair:last-child {
  margin-bottom: 0;
}

.qa-pair strong {
  display: block;
  margin-bottom: 5px;
}

.ai-answer p {
  margin-top: 8px;
}
</style>
