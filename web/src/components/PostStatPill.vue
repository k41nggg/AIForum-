<template>
  <span
    class="stat-pill"
    :class="[`stat-pill--${kind}`, { 'stat-pill--active': active }]"
    :title="titleText"
  >
    <span class="stat-icon" aria-hidden="true">{{ active ? meta.iconActive : meta.icon }}</span>
    <span class="stat-label">{{ active ? meta.labelActive : meta.label }}</span>
    <span class="stat-count">{{ count }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { POST_STAT_META, type PostStatKind } from '../lib/postStats'

const props = withDefaults(
  defineProps<{
    kind: PostStatKind
    count?: number
    active?: boolean
  }>(),
  { count: 0, active: false }
)

const meta = computed(() => POST_STAT_META[props.kind])

const titleText = computed(() => {
  const n = props.count
  if (props.kind === 'view') return `浏览 ${n}`
  if (props.kind === 'like') return props.active ? `已赞 ${n}` : `点赞 ${n}`
  return props.active ? `已收藏 ${n}` : `收藏 ${n}`
})
</script>

<style scoped>
.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.75);
  color: rgba(15, 23, 42, 0.65);
}

.stat-icon {
  font-size: 14px;
  line-height: 1;
}

.stat-count {
  font-variant-numeric: tabular-nums;
}

.stat-pill--view .stat-icon {
  opacity: 0.85;
}

.stat-pill--like.stat-pill--active {
  border-color: rgba(220, 38, 38, 0.4);
  background: rgba(254, 226, 226, 0.75);
  color: rgba(185, 28, 28, 0.95);
}

.stat-pill--like.stat-pill--active .stat-icon {
  color: #dc2626;
}

.stat-pill--collect.stat-pill--active {
  border-color: rgba(217, 119, 6, 0.45);
  background: rgba(254, 243, 199, 0.85);
  color: rgba(146, 64, 14, 0.95);
}

.stat-pill--collect.stat-pill--active .stat-icon {
  color: #d97706;
}
</style>
