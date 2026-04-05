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

    <aside class="sidebar">
      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">AI 问答</h2>
        </div>
        <div class="field">
          <label>基于帖子内容提问</label>
          <input class="input" v-model.trim="question" placeholder="输入你的问题..." @keyup.enter="askQuestion" />
        </div>
        <div class="ai-controls">
          <button class="btn" @click="getSummary" :disabled="summarizing">{{ summarizing ? 'AI总结中...' : 'AI总结' }}</button>
          <button class="btn btn-primary" @click="askQuestion" :disabled="asking">
            {{ asking ? '思考中...' : '提问' }}
          </button>
        </div>
        <div v-if="aiResults.length > 0" class="ai-answer">
          <div v-for="(result, index) in aiResults" :key="index" class="ai-result-item">
            <template v-if="result.type === 'summary'">
              <strong>AI 总结:</strong>
              <p>{{ result.content }}</p>
            </template>
            <template v-else-if="result.type === 'qa'">
              <div class="qa-pair">
                <strong>提问:</strong>
                <p>{{ result.content.question }}</p>
                <strong>AI 回答:</strong>
                <p>{{ result.content.answer }}</p>
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
import { useRoute } from 'vue-router'
import { apiDelete, apiGet, apiPost, getToken, type ApiResult, comment as commentApi } from '../lib/api'
import { showToast } from '../lib/toast'
import CommentNode from '../components/CommentNode.vue'

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
  children?: Comment[]
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

const summarizing = ref(false)
const asking = ref(false)
const question = ref('')
const aiResults = ref<{ type: 'summary' | 'qa'; content: any }[]>([])

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
  showToast('success', '点赞成功')
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
  showToast('success', '发布成功')
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
  showToast('success', '回复成功')
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
      showToast('error', '总结失败', '未能获取到AI总结内容')
    }
  } catch (e: any) {
    showToast('error', '总结失败', e.message || '请求失败')
  } finally {
    summarizing.value = false
  }
}

async function askQuestion() {
  if (!question.value) {
    showToast('error', '问题不能为空')
    return
  }
  if (!post.value) return

  asking.value = true
  const currentQuestion = question.value
  question.value = ''
  try {
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

// 权限判断 (示例)
function canDelete(c: Comment) {
  // 实际项目中应从 token 解析出当前用户 ID 和角色
  return false
}

onMounted(reload)
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
  margin-bottom: 10px;
}

.post-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.post-content {
  color: rgba(15, 23, 42, 0.88);
  white-space: pre-wrap;
  line-height: 1.8;
  margin-bottom: 20px;
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

.ai-controls {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.ai-controls .btn {
  flex-grow: 1;
}

.ai-result-item {
  margin-bottom: 15px;
}

.ai-result-item:last-child {
  margin-bottom: 0;
}

.ai-answer {
  margin-top: 20px;
  padding: 15px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.8;
  max-height: 800px;
  overflow-y: auto;
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
