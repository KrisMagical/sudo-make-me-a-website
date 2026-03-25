<!-- src/views/admin/CommentListView.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { publicApi } from '@/api/public'
import { commentsApi } from '@/api/comments'
import { useAuthStore } from '@/stores/authStore'
import { notify } from '@/utils/feedback'
import CommentNode from '@/components/public/CommentNode.vue'
import type { CommentDto, PostSummaryDto, CommentSearchResult } from '@/types/api'

const auth = useAuthStore()
const loading = ref(false)
const searchKeyword = ref('')
const searchResults = ref<CommentSearchResult[]>([])
const searching = ref(false)

// 回复弹窗状态
const replyModal = ref({
  visible: false,
  postId: 0,
  parentId: undefined as number | undefined,
  content: '',
  name: auth.username || 'Admin',
  email: ''
})

// 执行搜索
const performSearch = async () => {
  if (!searchKeyword.value.trim()) {
    notify('Please enter a search keyword', 'info')
    return
  }

  searching.value = true
  try {
    searchResults.value = await commentsApi.search(searchKeyword.value.trim())
    if (searchResults.value.length === 0) {
      notify('No comments found matching your search', 'info')
    }
  } catch (error) {
    console.error('Search failed:', error)
    notify('Search failed. Please try again.', 'error')
  } finally {
    searching.value = false
  }
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
}

// 删除评论
const handleDelete = async (comment: CommentDto | CommentSearchResult) => {
  const email = window.prompt('Please enter the email used for this comment to delete:')
  if (email === null) return
  if (!email.trim()) {
    notify('Email is required', 'error')
    return
  }
  if (!confirm('Delete this comment?')) return

  try {
    await commentsApi.delete(comment.id, email.trim())
    notify('Comment deleted')

    // 从搜索结果中移除
    const index = searchResults.value.findIndex(c => c.id === comment.id)
    if (index !== -1) {
      searchResults.value.splice(index, 1)
    }
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to delete comment. Make sure email is correct.'
    notify(message, 'error')
  }
}

// 打开回复弹窗
const openReply = (postId: number, parentId?: number) => {
  replyModal.value = {
    visible: true,
    postId,
    parentId,
    content: '',
    name: auth.username || 'Admin',
    email: ''
  }
}

// 提交回复/新评论（使用管理员专用接口）
const submitReply = async () => {
  if (!replyModal.value.content.trim()) {
    notify('Content cannot be empty', 'error')
    return
  }
  if (!replyModal.value.email.trim()) {
    notify('Email is required', 'error')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(replyModal.value.email)) {
    notify('Please enter a valid email address', 'error')
    return
  }

  try {
    const newComment = await commentsApi.addAdminComment(replyModal.value.postId, {
      name: replyModal.value.name,
      email: replyModal.value.email,
      content: replyModal.value.content,
      parentId: replyModal.value.parentId
    })

    // 刷新搜索结果
    if (searchKeyword.value.trim()) {
      await performSearch()
    }

    replyModal.value.visible = false
    notify('Reply posted successfully', 'success')
  } catch (error) {
    notify('Failed to post reply', 'error')
  }
}

// 为搜索结果的评论构建父评论信息
const getParentInfo = (comment: CommentSearchResult) => {
  if (!comment.parentId) return undefined

  return {
    name: comment.parentName || '',
    content: comment.parentContent || '',
    exists: comment.parentExists
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">COMMENTS MANAGEMENT</h2>
    </div>

    <!-- 搜索框 -->
    <div class="flex gap-4 items-center">
      <div class="flex-1">
        <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Search Comments</label>
        <div class="flex gap-2">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Search by content, username, post title or slug..."
            class="flex-1 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            @keyup.enter="performSearch"
          />
          <button
            @click="performSearch"
            :disabled="searching || !searchKeyword.trim()"
            class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
          >
            {{ searching ? 'Searching...' : 'Search' }}
          </button>
          <button
            v-if="searchResults.length > 0 || searchKeyword"
            @click="clearSearch"
            class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 text-sm font-bold uppercase tracking-tighter"
          >
            Clear
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchResults.length > 0">
      <div class="mb-4 text-sm text-zinc-500">
        Found {{ searchResults.length }} comment(s) matching "{{ searchKeyword }}"
      </div>

      <div class="space-y-6">
        <div v-for="comment in searchResults" :key="comment.id" class="border border-zinc-200 dark:border-zinc-800 p-4">
          <!-- 文章信息 -->
          <div class="mb-3 pb-2 border-b border-zinc-200 dark:border-zinc-800">
            <div class="text-xs text-zinc-500">Post:</div>
            <router-link
              :to="`/admin/posts/edit/${comment.postSlug}`"
              class="text-sm font-mono text-blue-600 hover:underline"
            >
              {{ comment.postTitle }} ({{ comment.postSlug }})
            </router-link>
          </div>

          <!-- 邮箱信息 -->
          <div class="mb-2 text-xs text-zinc-400">
            Email: {{ comment.email }}
          </div>

          <!-- 评论内容 -->
          <CommentNode
            :comment="comment"
            :parent-info="getParentInfo(comment)"
            @reply="openReply(comment.postId, $event)"
          >
            <template #actions="{ comment: cmt }">
              <button
                @click="handleDelete(cmt)"
                class="text-sm text-red-600 hover:text-red-800 ml-2"
              >
                Delete
              </button>
            </template>
          </CommentNode>
        </div>
      </div>
    </div>

    <div v-else-if="searching" class="text-center py-12 text-zinc-500">
      Searching...
    </div>

    <div v-else-if="searchKeyword && !searching && searchResults.length === 0" class="text-center py-12 text-zinc-500">
      No comments found matching "{{ searchKeyword }}"
    </div>

    <div v-else class="text-center py-12 text-zinc-500">
      Enter a keyword to search for comments by content, username, or post title/slug
    </div>

    <!-- 回复/添加评论弹窗 -->
    <div v-if="replyModal.visible" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-zinc-900 p-6 w-96 rounded shadow-lg">
        <h3 class="text-lg font-bold mb-4">
          {{ replyModal.parentId ? 'Reply to Comment' : 'Add Comment to Post' }}
        </h3>
        <input
          v-model="replyModal.name"
          placeholder="Name"
          class="w-full border p-2 mb-2 dark:bg-zinc-800"
        />
        <input
          v-model="replyModal.email"
          type="email"
          placeholder="Email (required)"
          class="w-full border p-2 mb-2 dark:bg-zinc-800"
        />
        <textarea
          v-model="replyModal.content"
          placeholder="Content"
          rows="4"
          class="w-full border p-2 mb-2 dark:bg-zinc-800"
        ></textarea>
        <div class="flex justify-end gap-2">
          <button @click="replyModal.visible = false" class="px-4 py-2 bg-gray-300 dark:bg-zinc-700 rounded">
            Cancel
          </button>
          <button @click="submitReply" class="px-4 py-2 bg-blue-600 text-white rounded">
            Submit
          </button>
        </div>
      </div>
    </div>
  </div>
</template>