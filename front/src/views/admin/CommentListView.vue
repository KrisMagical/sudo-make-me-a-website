<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { commentsApi } from '@/api/comments'
import { publicApi } from '@/api/public'
import type { CommentDto } from '@/types/api'
import { notify } from '@/utils/feedback'

const comments = ref<CommentDto[]>([])
const loading = ref(false)
const email = ref('')
const postComments = ref<Record<number, CommentDto[]>>({})

const fetchComments = async () => {
  loading.value = true
  try {
    // 这里需要获取所有文章的评论
    // 由于API没有直接获取所有评论的端点，我们需要先获取文章
    const posts = await publicApi.getRecentPosts(50)

    // 为每篇文章获取评论
    for (const post of posts) {
      try {
        const postCommentsData = await publicApi.getComments(post.id)
        if (postCommentsData.length > 0) {
          postComments.value[post.id] = postCommentsData
          comments.value.push(...postCommentsData)
        }
      } catch (error) {
        console.error(`Failed to fetch comments for post ${post.id}:`, error)
      }
    }
  } finally {
    loading.value = false
  }
}

const handleDelete = async (comment: CommentDto) => {
  if (!email.value) {
    notify('Please enter admin email to delete comments', 'error')
    return
  }

  if (!confirm('Delete this comment?')) return

  try {
    await commentsApi.delete(comment.id, email.value)
    notify('Comment deleted')
    comments.value = comments.value.filter(c => c.id !== comment.id)
  } catch (error) {
    notify('Failed to delete comment. Make sure email is correct.', 'error')
  }
}

onMounted(fetchComments)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">COMMENTS</h2>
    </div>

    <!-- 管理员邮箱输入 -->
    <div class="border border-zinc-200 dark:border-zinc-800 p-4">
      <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
        Admin Email (required for deletion)
      </label>
      <input
        v-model="email"
        type="email"
        class="w-full md:w-96 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
        placeholder="admin@example.com"
      />
      <div class="mt-2 text-xs text-zinc-500">
        Enter your admin email address to verify comment deletion.
      </div>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading comments...</div>

    <div v-else-if="comments.length > 0" class="space-y-6">
      <div
        v-for="comment in comments"
        :key="comment.id"
        class="border border-zinc-200 dark:border-zinc-800 p-4 hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors"
      >
        <div class="flex items-start justify-between mb-3">
          <div>
            <h4 class="font-bold">{{ comment.name }}</h4>
            <div class="text-xs text-zinc-500 font-mono">
              {{ new Date(comment.createdAt).toLocaleString() }}
            </div>
          </div>
          <button
            @click="handleDelete(comment)"
            :disabled="!email"
            class="text-red-500 hover:text-red-700 hover:font-bold text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            DELETE
          </button>
        </div>

        <p class="text-sm text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap">
          {{ comment.content }}
        </p>
      </div>
    </div>

    <div v-else class="text-center py-12 text-zinc-500">
      No comments found
    </div>
  </div>
</template>