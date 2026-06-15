<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { commentsApi } from '@/api/comments'
import { useAuthStore } from '@/stores/authStore'
import { notify } from '@/utils/feedback'
import { getApiErrorMessageWithRequestId } from '@/utils/apiError'
import CommentNode from '@/components/public/CommentNode.vue'
import type {
  BulkCommentAction,
  CommentSearchResult,
  CommentStatsDto,
  CommentStatusFilter
} from '@/types/api'

const auth = useAuthStore()
const statusOptions: CommentStatusFilter[] = ['PENDING', 'APPROVED', 'REJECTED', 'ALL']

const statusFilter = ref<CommentStatusFilter>('PENDING')
const keyword = ref('')
const comments = ref<CommentSearchResult[]>([])
const stats = ref<CommentStatsDto>({ pending: 0, approved: 0, rejected: 0, total: 0 })
const loading = ref(false)
const page = ref(0)
const size = 20
const total = ref(0)
const totalPages = ref(0)
const selectedIds = ref<Set<number>>(new Set())
const pendingDeleteId = ref<number | null>(null)
const confirmBulkDelete = ref(false)

const replyModal = ref({
  visible: false,
  postId: 0,
  parentId: undefined as number | undefined,
  content: '',
  name: auth.username || 'Admin',
  email: ''
})

const hasSelection = computed(() => selectedIds.value.size > 0)
const hasMore = computed(() => page.value + 1 < totalPages.value)

const selectedCount = computed(() => selectedIds.value.size)

const loadStats = async () => {
  stats.value = await commentsApi.stats() as CommentStatsDto
}

const loadComments = async (reset = true) => {
  loading.value = true
  try {
    const nextPage = reset ? 0 : page.value + 1
    const result = await commentsApi.list({
      status: statusFilter.value,
      keyword: keyword.value.trim() || undefined,
      page: nextPage,
      size,
      sort: 'createdAt desc'
    })

    comments.value = reset ? result.items : [...comments.value, ...result.items]
    page.value = result.page
    total.value = result.total
    totalPages.value = result.totalPages
    if (reset) selectedIds.value = new Set()
  } catch (error) {
    notify(getApiErrorMessageWithRequestId(error, 'Failed to load comments.'), 'error')
  } finally {
    loading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadComments(true), loadStats()])
}

const setStatus = async (status: CommentStatusFilter) => {
  statusFilter.value = status
  await loadComments(true)
}

const performSearch = async () => {
  await loadComments(true)
}

const clearSearch = async () => {
  keyword.value = ''
  await loadComments(true)
}

const toggleSelected = (id: number) => {
  const next = new Set(selectedIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedIds.value = next
}

const toggleAllVisible = () => {
  if (comments.value.length === 0) return
  const allVisibleSelected = comments.value.every(comment => selectedIds.value.has(comment.id))
  selectedIds.value = allVisibleSelected ? new Set() : new Set(comments.value.map(comment => comment.id))
}

const removeFromList = (ids: number[]) => {
  comments.value = comments.value.filter(comment => !ids.includes(comment.id))
  const next = new Set(selectedIds.value)
  ids.forEach(id => next.delete(id))
  selectedIds.value = next
}

const updateLocalStatus = (ids: number[], status: 'PENDING' | 'APPROVED' | 'REJECTED') => {
  comments.value.forEach(comment => {
    if (ids.includes(comment.id)) {
      comment.status = status
      if (status === 'APPROVED') comment.moderationReason = null
    }
  })
}

const handleDelete = async (comment: CommentSearchResult) => {
  if (pendingDeleteId.value !== comment.id) {
    pendingDeleteId.value = comment.id
    return
  }

  try {
    await commentsApi.delete(comment.id)
    removeFromList([comment.id])
    await loadStats()
    notify('Comment deleted', 'success')
  } catch (error) {
    notify(getApiErrorMessageWithRequestId(error, 'Failed to delete comment.'), 'error')
  } finally {
    pendingDeleteId.value = null
  }
}

const updateStatus = async (comment: CommentSearchResult, status: 'PENDING' | 'APPROVED' | 'REJECTED') => {
  try {
    const updated = await commentsApi.updateStatus(comment.id, status)
    comment.status = updated.status
    if (updated.status === 'APPROVED') comment.moderationReason = null
    await loadStats()
    notify(`Comment ${status.toLowerCase()}`, 'success')
  } catch (error) {
    notify(getApiErrorMessageWithRequestId(error, 'Failed to update comment status.'), 'error')
  }
}

const bulkAction = async (action: BulkCommentAction) => {
  const ids = [...selectedIds.value]
  if (ids.length === 0) {
    notify('No comments selected', 'info')
    return
  }

  if (action === 'DELETE' && !confirmBulkDelete.value) {
    confirmBulkDelete.value = true
    return
  }

  try {
    await commentsApi.bulk(ids, action)
    if (action === 'DELETE') {
      removeFromList(ids)
      notify('Comments deleted', 'success')
    } else {
      updateLocalStatus(ids, action === 'APPROVE' ? 'APPROVED' : 'REJECTED')
      selectedIds.value = new Set()
      notify(`Comments ${action.toLowerCase()}`, 'success')
    }
    confirmBulkDelete.value = false
    await loadStats()
  } catch (error) {
    notify(getApiErrorMessageWithRequestId(error, 'Bulk action failed.'), 'error')
  }
}

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
    await commentsApi.addAdminComment(replyModal.value.postId, {
      name: replyModal.value.name,
      email: replyModal.value.email,
      content: replyModal.value.content,
      parentId: replyModal.value.parentId
    })

    replyModal.value.visible = false
    await refreshAll()
    notify('Reply posted', 'success')
  } catch (error) {
    notify(getApiErrorMessageWithRequestId(error, 'Failed to post reply'), 'error')
  }
}

const getParentInfo = (comment: CommentSearchResult) => {
  if (!comment.parentId) return undefined

  return {
    name: comment.parentName || '',
    content: comment.parentContent || '',
    exists: comment.parentExists
  }
}

onMounted(refreshAll)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">COMMENTS</h2>
      <div class="text-xs font-mono text-zinc-500">
        pending: {{ stats.pending }} / approved: {{ stats.approved }} / rejected: {{ stats.rejected }} / total: {{ stats.total }}
      </div>
    </div>

    <div class="flex flex-wrap gap-2 items-end">
      <div class="flex gap-2">
        <button
          v-for="status in statusOptions"
          :key="status"
          @click="setStatus(status)"
          class="px-3 py-1 border text-xs font-mono uppercase"
          :class="statusFilter === status ? 'border-zinc-900 bg-zinc-900 text-white dark:bg-white dark:text-black' : 'border-zinc-300 dark:border-zinc-700'"
        >
          {{ status.toLowerCase() }}
          <span v-if="status === 'PENDING'">:{{ stats.pending }}</span>
        </button>
      </div>

      <div class="flex-1 min-w-64 flex gap-2">
        <input
          v-model="keyword"
          type="text"
          placeholder="search name / email / content / post"
          class="flex-1 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500 text-sm"
          @keyup.enter="performSearch"
        />
        <button
          @click="performSearch"
          :disabled="loading"
          class="px-3 py-2 border border-zinc-900 text-xs font-bold uppercase disabled:opacity-50"
        >
          {{ loading ? '...' : 'Search' }}
        </button>
        <button
          v-if="keyword"
          @click="clearSearch"
          class="px-3 py-2 border border-zinc-300 dark:border-zinc-700 text-xs font-bold uppercase"
        >
          Clear
        </button>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 items-center text-xs font-mono">
      <button @click="toggleAllVisible" class="px-2 py-1 border border-zinc-300 dark:border-zinc-700">
        toggle visible
      </button>
      <button @click="bulkAction('APPROVE')" class="px-2 py-1 border border-zinc-300 dark:border-zinc-700">
        approve selected
      </button>
      <button @click="bulkAction('REJECT')" class="px-2 py-1 border border-zinc-300 dark:border-zinc-700">
        reject selected
      </button>
      <button @click="bulkAction('DELETE')" class="px-2 py-1 border border-red-500 text-red-600">
        {{ confirmBulkDelete ? 'confirm delete selected' : 'delete selected' }}
      </button>
      <span class="text-zinc-500">selected: {{ selectedCount }}</span>
    </div>

    <div class="text-xs font-mono text-zinc-500">
      showing {{ comments.length }} / {{ total }}
    </div>

    <div v-if="loading && comments.length === 0" class="text-center py-12 text-zinc-500">
      Loading comments...
    </div>

    <div v-else-if="comments.length > 0" class="space-y-4">
      <div v-for="comment in comments" :key="comment.id" class="border border-zinc-200 dark:border-zinc-800 p-4">
        <div class="flex gap-3 items-start">
          <input
            type="checkbox"
            class="mt-1"
            :checked="selectedIds.has(comment.id)"
            @change="toggleSelected(comment.id)"
          />
          <div class="flex-1 min-w-0">
            <div class="mb-3 pb-2 border-b border-zinc-200 dark:border-zinc-800 flex flex-wrap gap-2 items-center">
              <router-link :to="`/admin/posts/edit/${comment.postSlug}`" class="text-sm font-mono hover:underline">
                {{ comment.postTitle }} ({{ comment.postSlug }})
              </router-link>
              <span class="border border-zinc-300 dark:border-zinc-700 px-1.5 py-0.5 text-[10px] font-mono">
                {{ comment.status }}
              </span>
            </div>

            <div class="mb-2 text-xs text-zinc-500 font-mono">
              {{ comment.name }} &lt;{{ comment.email }}&gt;
              <span v-if="comment.moderationReason" class="ml-2 text-red-600">
                reason: {{ comment.moderationReason }}
              </span>
            </div>

            <CommentNode
              :comment="comment"
              :parent-info="getParentInfo(comment)"
              @reply="openReply(comment.postId, $event)"
            >
              <template #actions="{ comment: cmt }">
                <button
                  v-if="cmt.status !== 'APPROVED'"
                  @click="updateStatus(comment, 'APPROVED')"
                  class="text-sm hover:underline ml-2"
                >
                  Approve
                </button>
                <button
                  v-if="cmt.status !== 'REJECTED'"
                  @click="updateStatus(comment, 'REJECTED')"
                  class="text-sm text-zinc-500 hover:underline ml-2"
                >
                  Reject
                </button>
                <button
                  @click="handleDelete(comment)"
                  class="text-sm text-red-600 hover:underline ml-2"
                >
                  {{ pendingDeleteId === comment.id ? 'Confirm delete' : 'Delete' }}
                </button>
              </template>
            </CommentNode>
          </div>
        </div>
      </div>

      <div class="text-center">
        <button
          v-if="hasMore"
          @click="loadComments(false)"
          :disabled="loading"
          class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 text-xs font-bold uppercase disabled:opacity-50"
        >
          {{ loading ? 'Loading...' : 'Load more' }}
        </button>
      </div>
    </div>

    <div v-else class="text-center py-12 text-zinc-500">
      No comments.
    </div>

    <div v-if="replyModal.visible" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-zinc-900 p-6 w-96 border border-zinc-900 dark:border-zinc-100">
        <h3 class="text-lg font-bold mb-4">
          {{ replyModal.parentId ? 'Reply' : 'Add comment' }}
        </h3>
        <input v-model="replyModal.name" placeholder="Name" class="w-full border p-2 mb-2 bg-transparent" />
        <input v-model="replyModal.email" type="email" placeholder="Email" class="w-full border p-2 mb-2 bg-transparent" />
        <textarea v-model="replyModal.content" placeholder="Content" rows="4" class="w-full border p-2 mb-2 bg-transparent"></textarea>
        <div class="flex justify-end gap-2">
          <button @click="replyModal.visible = false" class="px-4 py-2 border border-zinc-300 dark:border-zinc-700">
            Cancel
          </button>
          <button @click="submitReply" class="px-4 py-2 border border-zinc-900 dark:border-zinc-100">
            Submit
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
