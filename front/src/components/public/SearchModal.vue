<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from 'vue'
import { publicApi } from '@/api/public'
import type { PostSummaryDto } from '@/types/api'

const emit = defineEmits<{ (e: 'close'): void }>()

const keyword = ref('')
const loading = ref(false)
const posts = ref<PostSummaryDto[]>([])
const error = ref<string | null>(null)

let abortController: AbortController | null = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const cleanExcerpt = (excerpt: string) => {
  if (!excerpt) return ''
  let cleaned = excerpt.replace(/!\[.*?]\(.*?\)/g, '')
  cleaned = cleaned.replace(/https?:\/\/[^\s<>[\]()"]+/g, '')
  return cleaned.replace(/\s+/g, ' ').trim()
}

const performSearch = async (q: string) => {
  if (abortController) abortController.abort()
  abortController = new AbortController()
  const signal = abortController.signal

  loading.value = true
  error.value = null
  try {
    const postResults = await publicApi.searchPosts(q, { signal }) as unknown as PostSummaryDto[]
    if (keyword.value.trim() === q.trim()) {
      posts.value = postResults
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      error.value = 'Search failed, please try again later.'
      console.error(err)
    }
  } finally {
    loading.value = false
  }
}

watch(keyword, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  const q = newVal.trim()
  if (q.length < 2) {
    posts.value = []
    return
  }
  debounceTimer = setTimeout(() => performSearch(q), 300)
})

const totalCount = computed(() => posts.value.length)

const close = () => emit('close')

onUnmounted(() => {
  if (abortController) abortController.abort()
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-start justify-center pt-16 px-4 bg-zinc-900/60 backdrop-blur-sm transition-opacity" @click.self="close">
    <!-- 模态框主体 -->
    <div class="w-full max-w-3xl bg-white border-2 border-zinc-900 shadow-2xl flex flex-col overflow-hidden">

      <!-- 搜索输入区 -->
      <div class="p-4 border-b-2 border-zinc-900 flex items-center gap-3 bg-zinc-50">
        <span class="font-mono text-zinc-400 font-bold">></span>
        <input
            v-model="keyword"
            type="text"
            placeholder="Grep posts..."
            class="flex-1 bg-transparent border-none outline-none text-lg font-mono placeholder-zinc-300 text-zinc-900"
            autofocus
        />
        <button @click="close" class="font-mono text-xs font-bold px-2 py-1 border border-transparent hover:border-zinc-900 hover:bg-zinc-900 hover:text-white transition-colors uppercase">
          [ ESC ]
        </button>
      </div>

      <!-- 结果展示区 -->
      <div class="max-h-[70vh] overflow-y-auto bg-white">
        <!-- 状态：加载中 -->
        <div v-if="loading" class="py-16 text-center font-mono text-zinc-400 uppercase tracking-widest animate-pulse">
          Searching...
        </div>

        <!-- 状态：错误 -->
        <div v-else-if="error" class="py-16 text-center font-mono text-red-600 bg-red-50">
          {{ error }}
        </div>

        <!-- 状态：未找到 -->
        <div v-else-if="keyword.trim().length >= 2 && totalCount === 0" class="py-16 text-center font-mono text-zinc-500 uppercase tracking-widest">
          0 Matches found for "{{ keyword }}"
        </div>

        <!-- 状态：展示结果 -->
        <div v-else-if="totalCount > 0">
          <div class="px-6 py-2 bg-zinc-100 border-b border-zinc-200 flex justify-between items-center sticky top-0 z-10">
            <span class="text-xs font-bold uppercase tracking-wider text-zinc-500">Results</span>
            <span class="text-xs font-mono text-zinc-400">Found {{ totalCount }}</span>
          </div>

          <div class="flex flex-col">
            <router-link
                v-for="post in posts"
                :key="'post-'+post.id"
                :to="`/post/${post.slug}`"
                @click="close"
                class="group block p-6 border-b border-dashed border-zinc-200 hover:bg-zinc-50 transition-colors last:border-b-0"
            >
              <h4 class="text-xl font-black tracking-tight text-zinc-900 mb-2 group-hover:underline underline-offset-4 decoration-2">{{ post.title }}</h4>

              <p class="text-sm text-zinc-600 line-clamp-2 leading-relaxed mb-4">
                {{ cleanExcerpt(post.excerpt) || 'No excerpt available...' }}
              </p>

              <!-- 展示分类和所属合集（优化为 Tag 样式） -->
              <div class="flex flex-wrap items-center gap-2 font-mono text-[11px] font-bold uppercase">
                <!-- 分类 -->
                <span class="flex items-center gap-1.5 px-2 py-1 bg-zinc-100 text-zinc-700 border border-zinc-200">
                  <div class="i-carbon-tag w-3 h-3"></div>
                  {{ post.categoryName || 'Uncategorized' }}
                </span>

                <!-- 合集 -->
                <span v-if="post.collectionNames && post.collectionNames.length > 0"
                      class="flex items-center gap-1.5 px-2 py-1 bg-zinc-800 text-zinc-100 border border-zinc-700">
                  <div class="i-carbon-folder w-3 h-3 text-white"></div>
                  {{ post.collectionNames.join(', ') }}
                </span>

                <!-- 日期 -->
                <span class="flex items-center gap-1.5 text-zinc-400 ml-auto">
                  <div class="i-carbon-calendar w-3 h-3"></div>
                  {{ new Date(post.createdAt).toISOString().split('T')[0] }}
                </span>
              </div>
            </router-link>
          </div>
        </div>

        <!-- 状态：输入提示 -->
        <div v-else-if="keyword.trim().length < 2" class="py-16 text-center font-mono text-zinc-400">
          >_ Type at least 2 characters to search
        </div>
      </div>
    </div>
  </div>
</template>