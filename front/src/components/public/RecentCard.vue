<script setup lang="ts">
import { computed } from 'vue'
import type { PostSummaryDto, PageSummaryDto } from '@/types/api'

type RecentItem = (PostSummaryDto | PageSummaryDto) & { type: 'post' | 'page' }

const props = defineProps<{ item: RecentItem }>()

const displayExcerpt = computed(() => {
  if (props.item.type !== 'post') return ''
  const post = props.item as PostSummaryDto
  if (!post.excerpt) return ''

  // 移除 Markdown 图片语法
  const cleaned = post.excerpt.replace(/!\[.*?\]\(.*?\)/g, '')
  return cleaned.trim() ? cleaned : post.excerpt
})
</script>

<template>
  <article class="group border border-zinc-200 dark:border-zinc-600 hover:border-zinc-400 dark:hover:border-zinc-500 hover:shadow-md transition-all p-6 bg-white dark:bg-zinc-950 flex flex-col h-full">
    <div class="flex items-start justify-between mb-3">
      <div class="text-xs font-mono text-zinc-500 uppercase tracking-tighter">
        {{ item.type === 'post' ? item.categoryName || 'Post' : 'Page' }}
      </div>
      <div class="text-xs text-zinc-400 font-mono">
        {{ new Date(item.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) }}
      </div>
    </div>

    <router-link :to="item.type === 'post' ? `/post/${item.slug}` : `/page/${item.slug}`" class="flex-1">
      <h3 class="text-xl font-bold mb-3 group-hover:text-zinc-900 dark:group-hover:text-white transition-colors">
        {{ item.title }}
      </h3>
    </router-link>

    <p
      v-if="item.type === 'post' && displayExcerpt"
      class="text-sm text-zinc-600 dark:text-zinc-400 mb-4 line-clamp-2"
      v-html="displayExcerpt"
    />

    <!-- 底部区域：左侧统计/图标，右侧 Read → -->
    <div class="flex items-center justify-between text-xs text-zinc-500 mt-auto pt-4">
      <div v-if="item.type === 'post'" class="flex items-center space-x-4">
        <span class="flex items-center gap-1 text-green-600 dark:text-green-400">
          <div i-carbon-thumbs-up class="w-3 h-3" /> {{ (item as PostSummaryDto).likeCount }}
        </span>
        <span class="flex items-center gap-1 text-red-600 dark:text-red-400">
          <div i-carbon-thumbs-down class="w-3 h-3" /> {{ (item as PostSummaryDto).dislikeCount }}
        </span>
        <span>Views: {{ (item as PostSummaryDto).viewCount }}</span>
      </div>
      <div v-else class="text-zinc-400">
        <div i-carbon-document class="w-4 h-4" />
      </div>

      <router-link
        :to="item.type === 'post' ? `/post/${item.slug}` : `/page/${item.slug}`"
        class="font-bold uppercase tracking-tighter hover:underline"
      >
        Read →
      </router-link>
    </div>
  </article>
</template>