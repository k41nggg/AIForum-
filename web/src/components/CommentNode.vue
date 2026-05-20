<template>
  <div class="comment-item" :style="{ marginLeft: `${level * 20}px` }">
    <UserAvatar
      class="comment-avatar"
      :avatar="commentAvatar(comment)"
      :name="authorLabel(comment)"
      :user-id="comment.userId"
      size="sm"
    />
    <div class="comment-body">
      <div class="comment-head">
        <span class="comment-author">{{ authorLabel(comment) }}</span>
        <span class="pill">赞 {{ comment.likeCount }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <button class="btn" @click="emit('like', comment.id)">点赞</button>
        <button class="btn" @click="emit('reply', comment)">回复</button>
      </div>

      <div v-if="comment.children && comment.children.length > 0" class="comment-children">
        <CommentNode
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :level="level + 1"
          @like="emit('like', $event)"
          @reply="emit('reply', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { pickUserAvatar } from '../lib/avatar'
import UserAvatar from './UserAvatar.vue'

type CommentItem = {
  id: number
  userId: number
  likeCount: number
  content: string
  userNickname?: string
  userAvatar?: string
  children?: CommentItem[]
}

defineProps<{
  comment: CommentItem
  level: number
}>()

const emit = defineEmits(['like', 'reply'])

function authorLabel(c: CommentItem) {
  const name = c.userNickname?.trim()
  return name || '未知用户'
}

function commentAvatar(c: CommentItem) {
  return pickUserAvatar(c)
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border-left: 2px solid #eee;
  padding-left: 12px;
  margin-top: 15px;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head,
.comment-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.comment-author {
  font-weight: 650;
  color: rgba(15, 23, 42, 0.9);
}
.comment-content {
  margin-bottom: 8px;
  line-height: 1.6;
  word-break: break-word;
}
.comment-children {
  margin-top: 10px;
  margin-left: 0;
}
.comment-children :deep(.comment-item) {
  border-left-color: rgba(15, 23, 42, 0.08);
  padding-left: 10px;
}
</style>
