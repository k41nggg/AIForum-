<template>
  <button
    type="button"
    class="action-btn action-btn--like"
    :class="{ 'action-btn--active': liked, 'action-btn--loading': loading }"
    :disabled="loading || liked"
    :title="liked ? '你已赞过' : '点赞'"
    @click="emit('click')"
  >
    <span class="action-icon" aria-hidden="true">{{ liked ? '♥' : '♡' }}</span>
    <span class="action-label">{{ liked ? '已赞' : '赞' }}</span>
    <span v-if="showCount" class="action-count">{{ count }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    liked: boolean
    count?: number
    loading?: boolean
    showCount?: boolean
  }>(),
  { count: 0, loading: false, showCount: true }
)

const emit = defineEmits<{ click: [] }>()
</script>

<style scoped>
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  min-height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.14);
  background: rgba(255, 255, 255, 0.85);
  color: rgba(15, 23, 42, 0.72);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.action-btn--like:hover:not(:disabled) {
  border-color: rgba(220, 38, 38, 0.45);
  color: rgba(220, 38, 38, 0.9);
  background: rgba(254, 226, 226, 0.5);
}

.action-btn--like.action-btn--active {
  border-color: rgba(220, 38, 38, 0.55);
  background: rgba(254, 226, 226, 0.85);
  color: rgba(185, 28, 28, 0.95);
  cursor: default;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
}

.action-btn--like.action-btn--active .action-icon {
  color: #dc2626;
  transform: scale(1.08);
}

.action-btn--loading {
  opacity: 0.7;
  cursor: wait;
}

.action-icon {
  font-size: 16px;
  line-height: 1;
  transition: transform 0.15s;
}

.action-count {
  font-size: 12px;
  opacity: 0.85;
}
</style>
