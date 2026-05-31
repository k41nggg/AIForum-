<template>
  <button
    type="button"
    class="action-btn action-btn--collect"
    :class="{ 'action-btn--active': collected, 'action-btn--loading': loading }"
    :disabled="loading"
    :title="collected ? '点击取消收藏' : '收藏'"
    @click="emit('click')"
  >
    <span class="action-icon" aria-hidden="true">{{ collected ? '★' : '☆' }}</span>
    <span class="action-label">{{ collected ? '已藏' : '收藏' }}</span>
    <span v-if="showCount" class="action-count">{{ count }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    collected: boolean
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

.action-btn--collect:hover:not(:disabled) {
  border-color: rgba(217, 119, 6, 0.5);
  color: rgba(146, 64, 14, 0.95);
  background: rgba(254, 243, 199, 0.55);
}

.action-btn--collect.action-btn--active {
  border-color: rgba(217, 119, 6, 0.55);
  background: rgba(254, 243, 199, 0.9);
  color: rgba(146, 64, 14, 0.95);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.15);
}

.action-btn--collect.action-btn--active .action-icon {
  color: #d97706;
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
