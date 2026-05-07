<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import SmartContent from '@/components/public/SmartContent.vue'
import CommentForm from '@/components/public/CommentForm.vue'
import CommentNode from '@/components/public/CommentNode.vue'
import type { CommentDto, PostDetailDto, LikeResponseDto } from '@/types/api'

const route = useRoute()

const post = ref<PostDetailDto | null>(null)
const comments = ref<CommentDto[]>([])
const replyingTo = ref<number | undefined>()
const displayCount = ref(0)

const COLLAPSE_THRESHOLD = 15

// 加载文章和评论
const loadData = async () => {
  try {
    const slug = route.params.slug as string
    post.value = (await publicApi.getPost(slug)) as unknown as PostDetailDto
    comments.value = (await publicApi.getComments(post.value!.id)) as unknown as CommentDto[]

    // 初始化显示数量：若评论数超过阈值则只显示前15条，否则显示全部
    displayCount.value = comments.value.length > COLLAPSE_THRESHOLD
        ? COLLAPSE_THRESHOLD
        : comments.value.length
  } catch (error) {
    console.error('Failed to load post data', error)
  }
}

// 点赞/踩
const handleLike = async (positive: boolean) => {
  if (!post.value) return
  try {
    const res = (await publicApi.likePost(post.value.id, positive)) as unknown as LikeResponseDto
    post.value.likeCount = res.likes
    post.value.dislikeCount = res.dislikes
  } catch (error: any) {
    const message = error.response?.data?.message || 'An error occurred. Please try again.'
    alert(message)
  }
}

// 设置回复目标
const replyTo = (commentId?: number) => {
  replyingTo.value = commentId
}

// 评论按创建时间排序（所有评论扁平展示）
const sortedComments = computed(() => {
  return [...comments.value].sort((a, b) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  )
})

// 实际显示的评论列表（前 displayCount 条）
const visibleComments = computed(() => {
  return sortedComments.value.slice(0, displayCount.value)
})

// 是否需要显示“展示剩下评论”按钮
const showLoadMore = computed(() => {
  return comments.value.length > COLLAPSE_THRESHOLD && displayCount.value < comments.value.length
})

// 展示所有评论
const loadAllComments = () => {
  displayCount.value = comments.value.length
}

onMounted(loadData)
</script>

<template>
  <article v-if="post" class="max-w-3xl mx-auto py-12 px-4 md:px-6">
    <header class="mb-12 border-b border-zinc-200 dark:border-zinc-800 pb-8">
      <h1 class="text-3xl md:text-4xl font-black tracking-tighter mb-6">{{ post.title }}</h1>

      <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs font-mono text-zinc-600 dark:text-zinc-400">
        <!-- 分类 -->
        <span class="inline-flex items-center gap-1.5">
          <span class="i-carbon-tag w-3.5 h-3.5"></span>
          <span class="font-bold text-zinc-800 dark:text-zinc-200">{{ post.categoryName }}</span>
        </span>

        <!-- 合集（如果有） -->
        <span v-if="post.collectionNames && post.collectionNames.length > 0" class="inline-flex items-center gap-1.5">
          <span class="i-carbon-folder w-3.5 h-3.5"></span>
          <span>{{ post.collectionNames.join(', ') }}</span>
        </span>

        <!-- 浏览量 -->
        <span class="inline-flex items-center gap-1.5">
          <span class="i-carbon-view w-3.5 h-3.5"></span>
          <span>{{ post.viewCount }} views</span>
        </span>

        <!-- 创建日期 -->
        <span class="inline-flex items-center gap-1.5">
          <span class="i-carbon-calendar w-3.5 h-3.5"></span>
          <span>created {{ new Date(post.createdAt).toLocaleDateString() }}</span>
        </span>

        <!-- 更新日期（如果不同） -->
        <span v-if="post.updatedAt && post.updatedAt !== post.createdAt" class="inline-flex items-center gap-1.5">
          <span class="i-carbon-edit w-3.5 h-3.5"></span>
          <span>updated {{ new Date(post.updatedAt).toLocaleDateString() }}</span>
        </span>
      </div>
    </header>

    <SmartContent :content="post.content" class="prose prose-zinc dark:prose-invert max-w-none mb-12 text-base leading-relaxed" />

    <div class="flex items-center gap-6 border-t border-zinc-200 dark:border-zinc-800 pt-6 mb-12">
      <button
          @click="handleLike(true)"
          class="flex items-center gap-2 px-3 py-1.5 border border-zinc-300 dark:border-zinc-700 rounded-md hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors text-sm font-mono"
      >
        <span class="i-carbon-thumbs-up w-4 h-4 text-green-600 dark:text-green-400"></span>
        <span>Like</span>
        <span class="ml-1 font-bold">{{ post.likeCount }}</span>
      </button>
      <button
          @click="handleLike(false)"
          class="flex items-center gap-2 px-3 py-1.5 border border-zinc-300 dark:border-zinc-700 rounded-md hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors text-sm font-mono"
      >
        <span class="i-carbon-thumbs-down w-4 h-4 text-red-600 dark:text-red-400"></span>
        <span>Dislike</span>
        <span class="ml-1 font-bold">{{ post.dislikeCount }}</span>
      </button>
    </div>

    <section id="comments-section" class="mt-12">
      <h3 class="text-sm font-bold uppercase tracking-widest text-zinc-500 mb-6 border-b border-zinc-200 dark:border-zinc-800 pb-2">
        Comments ({{ comments.length }})
      </h3>

      <div class="space-y-6 mb-12">
        <template v-for="comment in visibleComments" :key="comment.id">
          <CommentNode
              :comment="comment"
              :all-comments="comments"
              @reply="replyTo"
          />
          <div v-if="comment !== visibleComments[visibleComments.length - 1]" class="border-t border-zinc-100 dark:border-zinc-800" />
        </template>
        <div v-if="comments.length === 0" class="text-sm text-zinc-400 italic text-center py-8">
          No comments yet. Be the first to share your thoughts!
        </div>
      </div>

      <div v-if="showLoadMore" class="text-center py-4 mb-8">
        <button
            @click="loadAllComments"
            class="inline-flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium transition-colors"
        >
          <span class="i-carbon-chevron-down w-4 h-4"></span>
          <span>Show {{ comments.length - displayCount }} more comments</span>
        </button>
      </div>

      <div id="comment-form" class="scroll-mt-20 mt-8">
        <CommentForm
            :post-id="post.id"
            :parent-id="replyingTo"
            @success="loadData"
            @cancel="replyTo(undefined)"
        />
      </div>
    </section>
  </article>
</template>

<style scoped>
.prose pre {
  @apply bg-zinc-100 dark:bg-zinc-800 rounded-md p-4 overflow-x-auto;
}
.prose code {
  @apply font-mono text-sm;
}
.prose blockquote {
  @apply border-l-4 border-zinc-300 dark:border-zinc-700 pl-4 italic text-zinc-600 dark:text-zinc-400;
}
</style>