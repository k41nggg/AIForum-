<template>
  <div class="comment-thread" :class="{ 'comment-thread--nested': level > 0 }">
    <div
      class="comment-item"
      :class="{ 'comment-item--highlight': activeReplyId === comment.id }"
    >
      <UserAvatar
        class="comment-avatar"
        :avatar="commentAvatar(comment)"
        :name="authorLabel(comment)"
        :user-id="comment.userId"
        size="sm"
      />
      <div class="comment-bubble">
        <div class="comment-head">
          <button type="button" class="comment-author" @click="goUser(comment.userId)">
            {{ authorLabel(comment) }}
          </button>
          <span class="comment-time">{{ formatCommentTime(comment.createTime) }}</span>
        </div>
        <div class="comment-content">{{ comment.content }}</div>
        <div class="comment-actions">
          <button
            type="button"
            class="action-link"
            :class="{ 'action-link--liked': comment.likeCount > 0 }"
            @click="emit('like', comment.id)"
          >
            <span class="action-icon">♥</span>
            {{ comment.likeCount > 0 ? comment.likeCount : '赞' }}
          </button>
          <button type="button" class="action-link" @click="emit('reply', comment)">回复</button>
        </div>
      </div>
    </div>

    <div v-if="comment.children?.length" class="comment-children">
      <CommentNode
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :level="level + 1"
        :active-reply-id="activeReplyId"
        @like="emit('like', $event)"
        @reply="emit('reply', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { pickUserAvatar } from '../lib/avatar'
import { formatCommentTime } from '../lib/time'
import UserAvatar from './UserAvatar.vue'

type CommentItem = {
  id: number
  userId: number
  likeCount: number
  content: string
  createTime?: string
  userNickname?: string
  userAvatar?: string
  children?: CommentItem[]
}

defineProps<{
  comment: CommentItem
  level: number
  activeReplyId?: number | null
}>()

const emit = defineEmits<{
  like: [id: number]
  reply: [comment: CommentItem]
}>()

const router = useRouter()

function authorLabel(c: CommentItem) {
  const name = c.userNickname?.trim()
  return name || '未知用户'
}

function commentAvatar(c: CommentItem) {
  return pickUserAvatar(c)
}

function goUser(userId: number) {
  router.push(`/users/${userId}`)
}
</script>

<style scoped>
.comment-thread--nested {
  margin-top: 4px;
}

.comment-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-item--highlight .comment-bubble {
  border-color: rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.06);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.comment-bubble {
  flex: 1;
  min-width: 0;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
}

.comment-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.comment-author {
  padding: 0;
  border: none;
  background: none;
  font-weight: 700;
  font-size: 14px;
  color: rgba(15, 23, 42, 0.92);
  cursor: pointer;
}

.comment-author:hover {
  color: var(--primary);
}

.comment-time {
  font-size: 12px;
  color: var(--muted);
}

.comment-content {
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
  color: rgba(15, 23, 42, 0.88);
}

.comment-actions {
  display: flex;
  gap: 14px;
  margin-top: 8px;
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  font-size: 13px;
  color: var(--muted);
  cursor: pointer;
}

.action-link:hover {
  color: var(--primary);
}

.action-link--liked {
  color: rgba(220, 38, 38, 0.85);
}

.action-link--liked:hover {
  color: rgba(185, 28, 28, 0.95);
}

.action-icon {
  font-size: 12px;
}

.comment-children {
  margin: 8px 0 0 42px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
