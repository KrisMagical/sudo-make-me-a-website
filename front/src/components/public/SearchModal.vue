<!-- src/components/public/SearchModal.vue -->
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { publicApi } from '@/api/public'
import type { PostSummaryDto, PageSummaryDto } from '@/types/api'

// 定义 emits
const emit = defineEmits<{
  (e: 'close'): void
}>()

// 搜索状态
const keyword = ref('')
const loading = ref(false)
const posts = ref<PostSummaryDto[]>([])
const pages = ref<PageSummaryDto[]>([])
const error = ref<string | null>(null)

// 防抖处理
let debounceTimer: number
watch(keyword, (newVal) => {
  clearTimeout(debounceTimer)
  if (newVal.trim().length < 2) {
    posts.value = []
    pages.value = []
    return
  }
  debounceTimer = setTimeout(() => performSearch(newVal), 300)
})

// 执行搜索
const performSearch = async (q: string) => {
  loading.value = true
  error.value = null
  try {
    const [postResults, pageResults] = await Promise.all([
      publicApi.searchPosts(q),
      publicApi.searchPages(q)
    ])
    posts.value = postResults
    pages.value = pageResults
  } catch (err) {
    error.value = 'Search failed, please try again later.'
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 合并结果（用于计数）
const totalCount = computed(() => posts.value.length + pages.value.length)

// 关闭模态框
const close = () => emit('close')
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-start justify-center pt-20 px-4 bg-black/50 backdrop-blur-sm" @click.self="close">
    <div class="w-full max-w-2xl bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 shadow-xl rounded-lg overflow-hidden">
      <!-- search input box -->
      <div class="p-4 border-b border-zinc-200 dark:border-zinc-800 flex items-center gap-2">
        <div i-carbon-search class="w-5 h-5 text-zinc-400" />
        <input
          v-model="keyword"
          type="text"
          placeholder="Search for articles or pages…"
          class="flex-1 bg-transparent border-none outline-none text-lg"
          autofocus
        />
        <button @click="close" class="text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200">
          <div i-carbon-close class="w-5 h-5" />
        </button>
      </div>

      <!-- Search results -->
      <div class="max-h-[70vh] overflow-y-auto p-4">
        <!-- loading -->
        <div v-if="loading" class="py-12 text-center text-zinc-500">
          <div class="animate-pulse">loading…</div>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="py-12 text-center text-red-500">
          {{ error }}
        </div>

        <!-- 无结果 -->
        <div v-else-if="keyword.trim().length >= 2 && totalCount === 0" class="py-12 text-center text-zinc-500">
          No content was found related to "{{ keyword }}"
        </div>

        <!-- 有结果 -->
        <div v-else-if="totalCount > 0" class="space-y-6">
          <!-- 文章结果 -->
          <div v-if="posts.length > 0">
            <h3 class="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">Post</h3>
            <div class="space-y-2">
              <router-link
                v-for="post in posts"
                :key="'post-'+post.id"
                :to="`/post/${post.slug}`"
                @click="close"
                class="block p-3 border border-zinc-100 dark:border-zinc-800 hover:bg-zinc-50 dark:hover:bg-zinc-900 rounded-md transition"
              >
                <h4 class="font-bold">{{ post.title }}</h4>
                <p class="text-sm text-zinc-600 dark:text-zinc-400 line-clamp-1">{{ post.excerpt || '...' }}</p>
                <div class="flex items-center gap-3 mt-1 text-xs text-zinc-500">
                  <span>{{ post.categoryName }}</span>
                  <span>·</span>
                  <span>{{ new Date(post.createdAt).toLocaleDateString() }}</span>
                </div>
              </router-link>
            </div>
          </div>

          <!-- 页面结果 -->
          <div v-if="pages.length > 0">
            <h3 class="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">页面</h3>
            <div class="space-y-2">
              <router-link
                v-for="page in pages"
                :key="'page-'+page.id"
                :to="`/page/${page.slug}`"
                @click="close"
                class="block p-3 border border-zinc-100 dark:border-zinc-800 hover:bg-zinc-50 dark:hover:bg-zinc-900 rounded-md transition"
              >
                <h4 class="font-bold">{{ page.title }}</h4>
                <div class="flex items-center gap-3 mt-1 text-xs text-zinc-500">
                  <span>Page</span>
                  <span>·</span>
                  <span>{{ new Date(page.createdAt).toLocaleDateString() }}</span>
                </div>
              </router-link>
            </div>
          </div>
        </div>

        <!-- 输入提示 -->
        <div v-else-if="keyword.trim().length < 2" class="py-12 text-center text-zinc-500">
          Enter at least 2 characters to start the search.
        </div>
      </div>
    </div>
  </div>
</template>