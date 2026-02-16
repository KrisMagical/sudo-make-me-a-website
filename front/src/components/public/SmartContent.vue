<script setup lang="ts">
import { computed } from 'vue'
import { parseVideoUrl, splitTextByVideoLinks } from '@/utils/videoParser'

const props = defineProps<{ content: string }>()

/**
 * 正则说明：
 * 1.  [[slug]] 或 [[slug|label]] —— Wiki 链接
 * 2.  [label](url) —— Markdown 链接
 */
const combinedRegex = /\[\[([^\]|]+)(?:\|([^\]]+))?\]\]|\[([^\]]+)\]\(([^)]+)\)/g

/**
 * 解析内容，生成包含文本、链接、视频的片段数组
 */
const parsedSegments = computed(() => {
  if (!props.content) return []

  let lastIndex = 0
  const segments: Array<{
    type: 'text' | 'link' | 'video'
    value?: string
    url?: string
    label?: string
    isInternal?: boolean
    embedUrl?: string
    provider?: string
  }> = []
  let match: RegExpExecArray | null

  while ((match = combinedRegex.exec(props.content)) !== null) {
    // ----- 处理匹配项之前的普通文本 -----
    if (match.index > lastIndex) {
      const textBefore = props.content.slice(lastIndex, match.index)
      // 将普通文本进一步拆分为视频片段和纯文本
      const textParts = splitTextByVideoLinks(textBefore)
      for (const part of textParts) {
        if (part.type === 'video') {
          segments.push({
            type: 'video',
            embedUrl: part.embedUrl,
            provider: part.provider
          })
        } else {
          segments.push({ type: 'text', value: part.value })
        }
      }
    }

    // ----- 解析当前匹配到的链接 -----
    const wikiSlug = match[1]      // [[slug]]
    const wikiLabel = match[2]     // [[slug|label]]
    const mdLabel = match[3]       // [label](url)
    const mdUrl = match[4]        // (url)

    if (wikiSlug) {
      // Wiki 链接：总是内部页面链接，不做视频检测
      segments.push({
        type: 'link',
        label: wikiLabel || wikiSlug,
        url: `/page/${wikiSlug.trim()}`,
        isInternal: true
      })
    } else if (mdUrl) {
      // Markdown 链接：先检测是否为视频链接
      const videoInfo = parseVideoUrl(mdUrl)
      if (videoInfo) {
        // 视频链接 → 渲染为 iframe
        segments.push({
          type: 'video',
          embedUrl: videoInfo.embedUrl,
          provider: videoInfo.provider
        })
      } else {
        // 普通链接 → 渲染为 <a> 或 <router-link>
        segments.push({
          type: 'link',
          label: mdLabel,
          url: mdUrl,
          isInternal: mdUrl.startsWith('/') || (typeof window !== 'undefined' && mdUrl.includes(window.location.hostname))
        })
      }
    }

    lastIndex = combinedRegex.lastIndex
  }

  // ----- 处理剩余文本 -----
  if (lastIndex < props.content.length) {
    const remainingText = props.content.slice(lastIndex)
    const textParts = splitTextByVideoLinks(remainingText)
    for (const part of textParts) {
      if (part.type === 'video') {
        segments.push({
          type: 'video',
          embedUrl: part.embedUrl,
          provider: part.provider
        })
      } else {
        segments.push({ type: 'text', value: part.value })
      }
    }
  }

  return segments
})
</script>

<template>
  <div class="prose-vim break-words leading-relaxed text-zinc-800 dark:text-zinc-200">
    <template v-for="(seg, i) in parsedSegments" :key="i">
      <!-- 普通文本（直接输出 HTML） -->
      <span v-if="seg.type === 'text'" v-html="seg.value" />

      <!-- 内部/外部链接 -->
      <template v-else-if="seg.type === 'link'">
        <router-link
          v-if="seg.isInternal"
          :to="seg.url"
          class="text-zinc-900 dark:text-zinc-100 font-bold underline decoration-2 decoration-zinc-300 hover:decoration-zinc-800 dark:decoration-zinc-600 dark:hover:decoration-zinc-400 transition-all mx-1"
        >
          {{ seg.label }}
        </router-link>
        <a
          v-else
          :href="seg.url"
          target="_blank"
          rel="noopener noreferrer"
          class="text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-200 underline decoration-dotted transition-colors mx-1"
        >
          {{ seg.label }}
        </a>
      </template>

      <!-- 嵌入式视频 iframe -->
      <div
        v-else-if="seg.type === 'video'"
        class="video-wrapper my-6 relative aspect-video w-full"
      >
        <iframe
          :src="seg.embedUrl"
          class="absolute top-0 left-0 w-full h-full border-0"
          frameborder="0"
          allowfullscreen
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.prose-vim :deep(p) {
  margin-bottom: 1.25em;
  line-height: 1.75;
}

.prose-vim :deep(h1),
.prose-vim :deep(h2),
.prose-vim :deep(h3) {
  margin-top: 2em;
  margin-bottom: 1em;
  font-weight: bold;
  letter-spacing: -0.02em;
}

.prose-vim :deep(ul),
.prose-vim :deep(ol) {
  margin-bottom: 1.25em;
  padding-left: 1.5em;
}

.prose-vim :deep(li) {
  margin-bottom: 0.5em;
}

.prose-vim :deep(img) {
  border-radius: 4px;
  max-width: 100%;
  height: auto;
  margin: 1.5rem 0;
}

.prose-vim :deep(blockquote) {
  border-left: 4px solid #e4e4e7;
  padding-left: 1rem;
  font-style: italic;
  margin: 1.5rem 0;
}

.dark .prose-vim :deep(blockquote) {
  border-left-color: #3f3f46;
}

/* 视频容器响应式 */
.video-wrapper {
  background: #0a0a0a;
  border-radius: 8px;
  overflow: hidden;
}

.video-wrapper iframe {
  transition: opacity 0.2s;
}
</style>