<template>
  <div class="post-preview">
    <div v-if="excerpt" class="post-preview-text">{{ excerpt }}</div>
    <div v-if="images.length" class="post-preview-images">
      <img
        v-for="(url, i) in images"
        :key="i"
        :src="resolveMediaUrl(url)"
        alt=""
        loading="lazy"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { contentExcerpt, extractImageUrls, resolveMediaUrl } from '../lib/markdown'

const props = withDefaults(
  defineProps<{
    content: string
    maxTextLen?: number
    maxImages?: number
  }>(),
  { maxTextLen: 100, maxImages: 3 }
)

const excerpt = computed(() => contentExcerpt(props.content, props.maxTextLen))
const images = computed(() => extractImageUrls(props.content, props.maxImages))
</script>

<style scoped>
.post-preview {
  display: grid;
  gap: 8px;
}
.post-preview-text {
  color: var(--text, rgba(15, 23, 42, 0.88));
  line-height: 1.6;
  word-break: break-word;
}
.post-preview-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.post-preview-images img {
  max-width: 200px;
  max-height: 140px;
  width: auto;
  height: auto;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.6);
}
</style>
