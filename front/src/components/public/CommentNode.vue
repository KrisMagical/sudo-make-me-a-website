<!-- src/components/public/CommentNode.vue -->
<script setup lang="ts">
import type { CommentDto, CommentSearchResult } from '@/types/api'

const props = defineProps<{
  comment: CommentDto | CommentSearchResult
  allComments?: CommentDto[]
  parentInfo?: {
    name: string
    content: string
    exists: boolean
  }
}>()

const emit = defineEmits<{
  (e: 'reply', commentId: number): void
}>()

// 获取父评论信息
const quotedComment = () => {
  if (props.parentInfo) {
    return props.parentInfo
  }

  const comment = props.comment as CommentDto
  if (comment.parentId && props.allComments) {
    const parent = props.allComments.find(c => c.id === comment.parentId)
    if (parent) {
      return {
        name: parent.name,
        content: parent.content,
        exists: true
      }
    }
    return {
      name: '',
      content: '',
      exists: false
    }
  }

  return null
}

// 滚动到被引用的评论（仅当父评论存在时）
const scrollToComment = (commentId: number) => {
  if (!quotedComment()?.exists) return

  const element = document.getElementById(`comment-${commentId}`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    element.classList.add('ring-2', 'ring-blue-500')
    setTimeout(() => {
      element.classList.remove('ring-2', 'ring-blue-500')
    }, 1500)
  }
}
</script>

<template>
  <div :id="`comment-${comment.id}`" class="group">
    <div class="flex flex-col gap-3">
      <!-- 引用块 -->
      <div
        v-if="comment.parentId && quotedComment()"
        class="bg-zinc-50 dark:bg-zinc-800/50 rounded-lg p-3 border border-zinc-200 dark:border-zinc-600"
      >
        <div class="flex items-center gap-2 mb-2">
          <span class="text-[10px] font-bold uppercase tracking-widest text-zinc-400">
            REPLY TO
          </span>
          <div class="flex-1 h-px bg-zinc-200 dark:bg-zinc-700"></div>
        </div>

        <button
          v-if="quotedComment()!.exists"
          @click="scrollToComment(comment.parentId!)"
          class="w-full text-left hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded px-2 py-1 -mx-2 -my-1 transition-colors"
        >
          <div class="text-xs font-bold text-zinc-800 dark:text-zinc-200">
            @{{ quotedComment()!.name }}
          </div>
          <!-- 父评论内容添加 break-words -->
          <p class="text-sm text-zinc-500 dark:text-zinc-400 mt-1 line-clamp-1 break-words">
            {{ quotedComment()!.content }}
          </p>
        </button>

        <div v-else class="px-2 py-1 text-sm text-zinc-400 italic break-words">
          This comment has been deleted.
        </div>
      </div>

      <div class="flex items-start justify-between">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-sm font-bold text-zinc-900 dark:text-zinc-100">
            {{ comment.name }} said
          </span>
          <span
            v-if="comment.author"
            class="text-[10px] font-bold uppercase bg-black text-white px-1.5 py-0.5 rounded"
          >
            Author
          </span>
          <span class="text-xs text-zinc-400">
            {{ new Date(comment.createdAt).toLocaleString() }}
          </span>
        </div>
        <div class="flex items-center gap-2">
          <slot name="actions" :comment="comment" />
          <button
            @click="emit('reply', comment.id)"
            class="text-xs font-bold uppercase tracking-wider text-zinc-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
          >
            Reply
          </button>
        </div>
      </div>
      <!-- 评论内容添加 break-words -->
      <p class="text-sm text-zinc-600 dark:text-zinc-300 leading-relaxed break-words">
        {{ comment.content }}
      </p>
    </div>
  </div>
</template>