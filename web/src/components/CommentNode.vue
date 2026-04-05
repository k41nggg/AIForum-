<template>
  <div class="comment-item" :style="{ marginLeft: `${level * 20}px` }">
    <div class="comment-head">
      <span class="pill">#{{ comment.id }}</span>
      <span class="pill">{{ comment.userNickname || comment.userId }}</span>
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
</template>

<script setup lang="ts">
defineProps<{
  comment: any
  level: number
}>()

const emit = defineEmits(['like', 'reply'])
</script>

<style scoped>
.comment-item {
  border-left: 2px solid #eee;
  padding-left: 15px;
  margin-top: 15px;
}
.comment-head,
.comment-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.comment-content {
  margin-bottom: 8px;
}
.comment-children {
  margin-top: 10px;
}
</style>
