<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import type { PostSummaryDto, CategoryDto } from '@/types/api'
import ArticleCard from '@/components/public/ArticleCard.vue'

const route = useRoute()
const posts = ref<PostSummaryDto[]>([])
const category = ref<CategoryDto | null>(null)
const loading = ref(true)

// 分页状态
const currentPage = ref(0)      // 0-based
const pageSize = ref(8)
const totalPages = ref(0)
const totalElements = ref(0)

// 跳转页码输入框绑定值（1-based）
const jumpPage = ref(1)

// 页码列表（最多显示5个连续页码）
const pageNumbers = computed(() => {
  const pages: number[] = []
  const maxVisible = 5
  const half = Math.floor(maxVisible / 2)
  let start = Math.max(0, currentPage.value - half)
  let end = Math.min(totalPages.value - 1, start + maxVisible - 1)

  if (end - start + 1 < maxVisible) {
    start = Math.max(0, end - maxVisible + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const hasPrev = computed(() => currentPage.value > 0)
const hasNext = computed(() => currentPage.value + 1 < totalPages.value)

const fetchData = async (resetCategory = true) => {
  loading.value = true
  try {
    const slug = route.params.slug as string
    const response = await publicApi.getPostsByCategory(slug, currentPage.value, pageSize.value)
    posts.value = response.content
    totalPages.value = response.totalPages
    totalElements.value = response.totalElements

    // 同步跳转输入框的当前页码值（+1 转换为 1-based）
    jumpPage.value = currentPage.value + 1

    if (resetCategory && !category.value) {
      const categories = await publicApi.getCategories()
      category.value = categories.find(c => c.slug === slug) || null
    }
  } catch (error) {
    console.error('Failed to fetch posts:', error)
    posts.value = []
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

const goToPage = (page: number) => {
  // page 是 0-based
  if (page === currentPage.value || page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchData(false)
}

const prevPage = () => hasPrev.value && goToPage(currentPage.value - 1)
const nextPage = () => hasNext.value && goToPage(currentPage.value + 1)

// 处理跳转输入框提交
const handleJump = () => {
  let page = jumpPage.value - 1  // 转换为 0-based
  // 边界检查
  if (page < 0) page = 0
  if (page >= totalPages.value) page = totalPages.value - 1
  if (page !== currentPage.value) {
    goToPage(page)
  } else {
    // 如果页码相同，只需同步输入框显示
    jumpPage.value = currentPage.value + 1
  }
}

// 监听路由参数变化，重置分页并重新加载
watch(
  () => route.params.slug,
  (newSlug, oldSlug) => {
    if (newSlug !== oldSlug) {
      currentPage.value = 0
      posts.value = []
      category.value = null
      fetchData(true)
    }
  }
)

onMounted(() => fetchData(true))
</script>

<template>
  <div v-if="loading && posts.length === 0" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="animate-pulse">Loading...</div>
  </div>

  <div v-else class="max-w-6xl mx-auto px-4 py-12">
    <header class="mb-12">
      <h1 class="text-4xl font-bold tracking-tighter mb-4">
        {{ category?.name || 'Category' }}
      </h1>
      <div class="text-sm text-zinc-500 font-mono uppercase tracking-tighter">
        {{ totalElements > 0 ? `${totalElements} POSTS` : '0 POSTS' }}
      </div>
    </header>

    <div v-if="posts.length > 0" class="grid grid-cols-1 gap-6">
      <ArticleCard v-for="post in posts" :key="post.id" :post="post" />
    </div>

    <!-- 分页组件（含跳转输入框） -->
    <div v-if="totalPages > 1" class="flex flex-wrap justify-center items-center gap-4 mt-12">
      <div class="flex items-center gap-2">
        <button
          @click="prevPage"
          :disabled="!hasPrev || loading"
          class="px-4 py-2 border border-zinc-200 dark:border-zinc-800 rounded-md text-sm font-mono uppercase tracking-tighter transition disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-100 dark:hover:bg-zinc-800"
        >
          ← Prev
        </button>

        <div class="flex gap-1">
          <button
            v-for="page in pageNumbers"
            :key="page"
            @click="goToPage(page)"
            :class="[
              'px-3 py-2 text-sm font-mono uppercase tracking-tighter rounded-md transition',
              currentPage === page
                ? 'bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900'
                : 'border border-zinc-200 dark:border-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-800'
            ]"
            :disabled="loading"
          >
            {{ page + 1 }}
          </button>
        </div>

        <button
          @click="nextPage"
          :disabled="!hasNext || loading"
          class="px-4 py-2 border border-zinc-200 dark:border-zinc-800 rounded-md text-sm font-mono uppercase tracking-tighter transition disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-100 dark:hover:bg-zinc-800"
        >
          Next →
        </button>
      </div>

      <!-- 跳转输入框 -->
      <!-- 跳转输入框 -->
      <div class="flex items-center gap-2">
        <span class="text-xs text-zinc-500 font-mono">跳转至</span>
        <input
          type="number"
          v-model.number="jumpPage"
          :min="1"
          :max="totalPages"
          class="w-20 px-3 py-2 text-center border border-zinc-200 dark:border-zinc-800 rounded-md bg-transparent text-sm font-mono focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-600"
          @keyup.enter="handleJump"
          :disabled="loading"
        />
        <span class="text-xs text-zinc-500 font-mono">页</span>
        <button
          @click="handleJump"
          :disabled="loading"
          class="px-3 py-2 border border-zinc-200 dark:border-zinc-800 rounded-md text-sm font-mono uppercase tracking-tighter hover:bg-zinc-100 dark:hover:bg-zinc-800 transition disabled:opacity-40 disabled:cursor-not-allowed"
        >
          GO
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="posts.length === 0 && !loading" class="text-center py-20 border border-zinc-200 dark:border-zinc-800">
      <h3 class="text-lg font-bold mb-2">No posts in this category</h3>
      <p class="text-zinc-600 dark:text-zinc-400 mb-6">
        No articles have been published in this category yet.
      </p>
      <router-link to="/" class="text-sm font-bold uppercase tracking-tighter hover:underline">
        ← Back to Home
      </router-link>
    </div>
  </div>
</template>