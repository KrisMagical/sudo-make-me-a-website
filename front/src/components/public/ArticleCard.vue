<script setup lang="ts">
import { computed } from 'vue'
import type { PostSummaryDto } from '@/types/api'

const props = defineProps<{
  post: PostSummaryDto
}>()

const cleanExcerpt = computed(() => {
  if (!props.post.excerpt) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(props.post.excerpt, 'text/html')
  const body = doc.body
  const mediaTags = ['img', 'video', 'iframe', 'audio', 'embed', 'object']
  mediaTags.forEach(tag => {
    body.querySelectorAll(tag).forEach(el => el.remove())
  })
  body.querySelectorAll('p, div, span').forEach(el => {
    if (el.children.length === 0 && !el.textContent?.trim()) {
      el.remove()
    }
  })
  return body.innerHTML
})
</script>

<template>
  <article class="group border border-zinc-100 dark:border-zinc-900 hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors p-6">
    <div class="flex items-start justify-between mb-3">
      <div class="text-xs font-mono text-zinc-500 uppercase tracking-tighter">
        {{ post.categoryName }}
      </div>
      <div class="text-xs text-zinc-400 font-mono">
        {{ new Date(post.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) }}
      </div>
    </div>

    <router-link :to="`/post/${post.slug}`">
      <h3 class="text-xl font-bold mb-3 group-hover:text-zinc-900 dark:group-hover:text-white transition-colors">
        {{ post.title }}
      </h3>
    </router-link>

    <div
      v-if="cleanExcerpt"
      class="text-sm text-zinc-600 dark:text-zinc-400 mb-4 line-clamp-2 [&>*]:m-0"
      v-html="cleanExcerpt"
    />

    <div class="flex items-center justify-between text-xs text-zinc-500">
      <div class="flex items-center space-x-4">
        <span class="flex items-center gap-1 text-green-600 dark:text-green-400">
          <div i-carbon-thumbs-up class="w-3 h-3" /> {{ post.likeCount }}
        </span>
        <span class="flex items-center gap-1 text-red-600 dark:text-red-400">
          <div i-carbon-thumbs-down class="w-3 h-3" /> {{ post.dislikeCount }}
        </span>
        <span>Views: {{ post.viewCount }}</span>
      </div>

      <router-link
        :to="`/post/${post.slug}`"
        class="font-bold uppercase tracking-tighter hover:underline"
      >
        Read →
      </router-link>
    </div>
  </article>
</template>