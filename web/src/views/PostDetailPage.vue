<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">帖子详情</h1>
        <div class="subtle">阅读内容、参与评论交流</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="reload" :disabled="loading">刷新</button>
        <RouterLink class="btn" to="/">返回列表</RouterLink>
      </div>
    </div>

    <section class="glass card" v-if="post">
      <div class="post-title">{{ post.title }}</div>
      <div class="post-meta">
        <span class="pill">#{{ post.id }}</span>
        <span class="pill">作者 {{ post.userNickname || post.userId }}</span>
        <span class="pill">分类 {{ post.categoryId }}</span>
        <span class="pill">浏览 {{ post.viewCount }}</span>
        <span class="pill">点赞 {{ post.likeCount }}</span>
      </div>
      <div class="post-content">{{ post.content }}</div>
      <div class="post-actions">
        <button class="btn" @click="likePost" :disabled="liking">{{ liking ? '处理中...' : '点赞' }}</button>
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
        <div class="comment" v-for="c in comments" :key="c.id">
          <div class="comment-head">
            <span class="pill">#{{ c.id }}</span>
            <span class="pill">{{ c.userNickname || c.userId }}</span>
            <span class="pill">赞 {{ c.likeCount }}</span>
          </div>
          <div class="comment-content">{{ c.content }}</div>
          <div class="comment-actions">
            <button class="btn" @click="likeComment(c.id)">点赞</button>
            <button class="btn" @click="setReplyTo(c)">回复</button>
            <button v-if="canDelete(c)" class="btn" @click="deleteComment(c.id)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无评论</div>

      <div v-if="replyTo" class="reply-box">
        <div class="subtle">回复评论 #{{ replyTo.id }}（user {{ replyTo.userId }}）</div>
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
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { apiDelete, apiGet, apiPost, getToken, type ApiResult } from '../lib/api'
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
}

type Comment = {
  id: number
  postId: number
  userId: number
  userNickname?: string
  parentId: number
  rootId: number
  content: string
  likeCount: number
  createTime: string
}

const route = useRoute()
const postId = computed(() => Number(route.params.id))

const loading = ref(false)
const post = ref<Post | null>(null)
const comments = ref<Comment[]>([])

const newComment = ref('')
const commenting = ref(false)

const replyTo = ref<Comment | null>(null)
const replyContent = ref('')
const replying = ref(false)

const liking = ref(false)

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
  post.value = res.data
}

async function loadComments() {
  const res = await apiGet<ApiResult<Comment[]>>(`/comments/post/${postId.value}`)
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
  comments.value = res.data || []
}

async function reload() {
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
  const res = await apiPost<ApiResult<Comment>>('/comments', {
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

  newComment.value = ''
  showToast('success', '发布成功', '评论已发布')
  await loadComments()
}

function setReplyTo(c: Comment) {
  replyTo.value = c
  replyContent.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyContent.value = ''
}

async function submitReply() {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能回复')
    return
  }
  if (!replyTo.value) return
  if (!replyContent.value) {
    showToast('error', '发布失败', '回复内容不能为空')
    return
  }

  replying.value = true
  const target = replyTo.value
  const res = await apiPost<ApiResult<Comment>>('/comments', {
    postId: postId.value,
    content: replyContent.value,
    parentId: target.id,
    rootId: target.rootId && target.rootId !== 0 ? target.rootId : target.id
  })
  replying.value = false

  if (!res) {
    showToast('error', '发布失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '发布失败', res.message || '发布失败')
    return
  }

  cancelReply()
  showToast('success', '回复成功', '已发布回复')
  await loadComments()
}

async function likeComment(id: number) {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能点赞评论')
    return
  }
  const res = await apiPost<ApiResult<null>>(`/comments/${id}/like`)
  if (!res) {
    showToast('error', '点赞失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '点赞失败', res.message || '点赞失败')
    return
  }
  showToast('success', '点赞成功', '已点赞该评论')
  await loadComments()
}

// 当前后端没有提供 /api/users/me 的前端读取缓存接口，这里只做一个简单策略：
// 能删除：先允许所有已登录用户点“删除”，由后端鉴权决定是否成功。
function canDelete(_c: Comment) {
  return Boolean(getToken())
}

async function deleteComment(id: number) {
  if (!getToken()) {
    showToast('error', '需要登录', '登录后才能删除评论')
    return
  }

  const res = await apiDelete<ApiResult<null>>(`/comments/${id}`)
  if (!res) {
    showToast('error', '删除失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '删除失败', res.message || '删除失败')
    return
  }

  showToast('success', '删除成功', '评论已删除')
  await loadComments()
}

onMounted(reload)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; flex-wrap: wrap; }

.post-title { font-weight: 900; font-size: 20px; letter-spacing: 0.2px; margin-bottom: 10px; }
.post-meta { display:flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.post-content { color: rgba(15, 23, 42, 0.88); white-space: pre-wrap; line-height: 1.7; }
.post-actions { margin-top: 14px; display:flex; gap: 10px; }

.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }

.comments { display: grid; gap: 12px; margin-top: 14px; }
.comment {
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255,255,255,0.70);
  border-radius: 12px;
  padding: 12px;
}
.comment-head { display:flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.comment-content { color: rgba(15, 23, 42, 0.86); white-space: pre-wrap; line-height: 1.6; }
.comment-actions { margin-top: 10px; display:flex; gap: 10px; flex-wrap: wrap; }

.empty { color: var(--muted); padding: 10px 0; }
.reply-box {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.10);
}
</style>
