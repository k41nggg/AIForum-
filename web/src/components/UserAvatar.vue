<template>
  <div class="user-avatar" :class="sizeClass" :title="displayName">
    <img v-if="src" :src="src" :alt="displayName" />
    <span v-else class="user-avatar-placeholder">{{ initial }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { avatarInitial, resolveAvatarUrl } from '../lib/avatar'

const props = withDefaults(
  defineProps<{
    avatar?: string | null
    name?: string | null
    size?: 'sm' | 'md' | 'lg'
  }>(),
  { size: 'md' }
)

const src = computed(() => resolveAvatarUrl(props.avatar))
const displayName = computed(() => props.name?.trim() || '用户')
const initial = computed(() => avatarInitial(props.name))
const sizeClass = computed(() => `user-avatar--${props.size}`)
</script>

<style scoped>
.user-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.08));
}
.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.user-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(29, 78, 216, 0.1));
  color: rgba(15, 23, 42, 0.75);
}
.user-avatar--sm {
  width: 32px;
  height: 32px;
}
.user-avatar--sm .user-avatar-placeholder {
  font-size: 14px;
}
.user-avatar--md {
  width: 40px;
  height: 40px;
}
.user-avatar--md .user-avatar-placeholder {
  font-size: 16px;
}
.user-avatar--lg {
  width: 48px;
  height: 48px;
}
.user-avatar--lg .user-avatar-placeholder {
  font-size: 18px;
}
</style>
