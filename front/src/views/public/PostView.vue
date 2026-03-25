<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import SmartContent from '@/components/public/SmartContent.vue'
import CommentForm from '@/components/public/CommentForm.vue'
import CommentNode from '@/components/public/CommentNode.vue'
import type { CommentDto } from '@/types/api'

const route = useRoute()

const post = ref<any>(null)
const comments = ref<CommentDto[]>([])
const replyingTo = ref<number | undefined>()
const displayCount = ref(0)                 // 实际显示的评论数量

const COLLAPSE_THRESHOLD = 15

// 加载文章和评论
const loadData = async () => {
  try {
    const slug = route.params.slug as string
    post.value = await publicApi.getPost(slug)
    comments.value = await publicApi.getComments(post.value.id)

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
    const res = await publicApi.likePost(post.value.id, positive)
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
  <article v-if="post" class="max-w-2xl mx-auto py-12 px-4">
    <!-- 文章头部 -->
    <header class="mb-8 border-b border-zinc-100 dark:border-zinc-800 pb-8">
      <h1 class="text-3xl font-bold tracking-tighter mb-4">{{ post.title }}</h1>
      <div class="flex flex-wrap items-center gap-x-3 gap-y-2 text-xs font-mono uppercase tracking-tighter text-zinc-500 dark:text-zinc-400">
        <span class="text-zinc-800 dark:text-zinc-200 font-bold">POST / {{ post.categoryName }}</span>
        <span class="text-zinc-300 dark:text-zinc-700">|</span>
        <span>VIEWS: {{ post.viewCount }}</span>
        <span class="text-zinc-300 dark:text-zinc-700">|</span>
        <span>CREATED: {{ new Date(post.createdAt).toLocaleDateString() }}</span>
        <template v-if="post.updatedAt && post.updatedAt !== post.createdAt">
          <span class="text-zinc-300 dark:text-zinc-700">|</span>
          <span class="text-zinc-400 dark:text-zinc-500">UPDATED: {{ new Date(post.updatedAt).toLocaleDateString() }}</span>
        </template>
      </div>
    </header>

    <!-- 文章内容 -->
    <SmartContent :content="post.content" class="mb-12 text-lg" />

    <!-- 点赞/踩区域 -->
    <div class="flex items-center gap-6 border-y border-zinc-100 dark:border-zinc-800 py-6 mb-12">
      <button
        @click="handleLike(true)"
        class="flex items-center gap-2 text-green-600 hover:text-green-700 dark:text-green-400 dark:hover:text-green-300 transition-colors"
      >
        <div class="i-carbon-thumbs-up w-5 h-5" />
        <span>Like</span> {{ post.likeCount }}
      </button>
      <button
        @click="handleLike(false)"
        class="flex items-center gap-2 text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 transition-colors"
      >
        <div class="i-carbon-thumbs-down w-5 h-5" />
        <span>Dislike</span> {{ post.dislikeCount }}
      </button>
    </div>

    <!-- 评论区 -->
    <section id="comments-section">
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-sm font-bold uppercase tracking-widest">Comments ({{ comments.length }})</h3>
      </div>

      <!-- 评论列表（只显示前 displayCount 条） -->
      <div class="space-y-6 mb-12">
        <template v-for="comment in visibleComments" :key="comment.id">
          <CommentNode
            :comment="comment"
            :all-comments="comments"
            @reply="replyTo"
          />
          <!-- 分隔线 -->
          <div
            v-if="comment !== visibleComments[visibleComments.length - 1]"
            class="border-t border-zinc-100 dark:border-zinc-800"
          />
        </template>
        <div v-if="comments.length === 0" class="text-sm text-zinc-400 italic">
          No comments yet. Be the first to share your thoughts!
        </div>
      </div>

      <!-- 展示剩下评论的按钮（仅当评论数超过阈值且尚未显示全部时显示） -->
      <div v-if="showLoadMore" class="text-center py-4 mb-8">
        <button
          @click="loadAllComments"
          class="inline-flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium transition-colors"
        >
          <div class="i-carbon-chevron-down w-4 h-4" />
          <span>Show {{ comments.length - displayCount }} more comments</span>
        </button>
      </div>

      <!-- 评论表单 -->
      <div id="comment-form" class="scroll-mt-20">
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