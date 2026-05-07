<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { collectionsApi } from '@/api/collections'
import type { PostGroupDto, PostSummaryDto, PageResponse } from '@/types/api'
import ArticleCard from '@/components/public/ArticleCard.vue'

const route = useRoute()
const collection = ref<PostGroupDto | null>(null)
const posts = ref<PostSummaryDto[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

// 搜索状态
const searchKeyword = ref('')
const isSearching = computed(() => !!searchKeyword.value.trim())

// 分页状态
const currentPage = ref(0)      // 0-based
const pageSize = ref(8)
const totalPages = ref(0)
const totalElements = ref(0)

const jumpPage = ref(1)

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

const fetchCollectionData = async (slug: string, resetPage = true) => {
  if (slug === '00100000') {
    error.value = 'Collection not found'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  const kw = searchKeyword.value.trim()

  try {
    let response: PageResponse<PostSummaryDto>

    if (kw) {
      response = await collectionsApi.searchPostsInCollection(slug, kw, currentPage.value, pageSize.value)
    } else {
      response = await collectionsApi.getPostsByCollection(slug, currentPage.value, pageSize.value)
    }

    posts.value = response.content
    totalPages.value = response.totalPages
    totalElements.value = response.totalElements
    jumpPage.value = currentPage.value + 1

    if (!collection.value || resetPage) {
      collection.value = await collectionsApi.getBySlug(slug)
    }
  } catch (err: any) {
    console.error('Fetch failed:', err)
    error.value = 'Collection not found'
  } finally {
    loading.value = false
  }
}

const performSearch = () => {
  currentPage.value = 0
  fetchCollectionData(route.params.slug as string, false)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, (newVal) => {
  if (searchTimer) clearTimeout(searchTimer)
  const delay = newVal.trim() ? 300 : 0
  searchTimer = setTimeout(performSearch, delay)
})

const goToPage = (page: number) => {
  if (page === currentPage.value || page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchCollectionData(route.params.slug as string, false)
}

const prevPage = () => hasPrev.value && goToPage(currentPage.value - 1)
const nextPage = () => hasNext.value && goToPage(currentPage.value + 1)

const handleJump = () => {
  let page = jumpPage.value - 1
  if (page < 0) page = 0
  if (page >= totalPages.value) page = totalPages.value - 1
  if (page !== currentPage.value) {
    goToPage(page)
  } else {
    jumpPage.value = currentPage.value + 1
  }
}

watch(() => route.params.slug, (newSlug) => {
  if (newSlug) {
    searchKeyword.value = ''
    currentPage.value = 0
    fetchCollectionData(newSlug as string, true)
  }
})

onMounted(() => fetchCollectionData(route.params.slug as string, true))
</script>

<template>
  <div v-if="loading && posts.length === 0" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="animate-pulse font-mono text-zinc-500 uppercase tracking-widest text-sm">Loading Collection...</div>
  </div>

  <div v-else-if="error"
       class="max-w-2xl mx-auto py-20 px-4 text-center border border-dashed border-zinc-300 dark:border-zinc-700 mt-12">
    <h1 class="text-2xl font-bold tracking-tighter mb-4">{{ error }}</h1>
    <p class="text-zinc-600 dark:text-zinc-400 mb-8 font-mono text-sm">
      > 404_COLLECTION_NOT_FOUND
    </p>
    <router-link to="/"
                 class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 text-sm font-bold uppercase tracking-tighter transition-colors">
      Return to root
    </router-link>
  </div>

  <div v-else-if="collection" class="max-w-5xl mx-auto px-4 py-12">
    <header class="mb-16">
      <div class="flex flex-col md:flex-row gap-10 items-start">
        <div class="flex-shrink-0 w-full md:w-2/5 group">
          <div
              class="aspect-[4/3] border-2 border-zinc-900 dark:border-zinc-100 bg-zinc-50 dark:bg-zinc-900 overflow-hidden relative">
            <img
                v-if="collection.coverImageUrl"
                :src="collection.coverImageUrl"
                :alt="collection.name"
                class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
            />
            <div v-else
                 class="w-full h-full flex items-center justify-center text-8xl font-black text-zinc-200 dark:text-zinc-800">
              {{ collection.name.charAt(0).toUpperCase() }}
            </div>
            <div
                class="absolute top-4 left-4 bg-zinc-900 text-white dark:bg-white dark:text-zinc-900 px-3 py-1 font-mono text-xs uppercase font-bold">
              {{ totalElements }} POST{{ totalElements !== 1 ? 'S' : '' }}
            </div>
          </div>
        </div>

        <div class="flex-1 pt-2">
          <div class="flex justify-between items-start flex-wrap gap-4">
            <h1 class="text-5xl md:text-6xl font-black tracking-tighter mb-6 leading-none">
              {{ collection.name }}
            </h1>
            <!-- 搜索框：与 CategoryView 样式一致 -->
            <div class="relative">
              <div class="i-carbon-search absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400"></div>
              <input
                  v-model="searchKeyword"
                  type="text"
                  placeholder="Search in this collection..."
                  class="pl-9 pr-4 py-2 border border-zinc-200 dark:border-zinc-700 rounded-md bg-transparent text-sm focus:outline-none focus:border-zinc-500 dark:focus:border-zinc-400"
              />
            </div>
          </div>
          <div class="font-mono text-sm text-zinc-500 mb-6 flex items-center gap-2">
            <span class="w-4 h-px bg-zinc-400"></span>
            {{ collection.slug }}
          </div>
          <p v-if="collection.description"
             class="text-lg text-zinc-700 dark:text-zinc-300 leading-relaxed font-serif prose-vim">
            {{ collection.description }}
          </p>
        </div>
      </div>
    </header>

    <div class="w-full h-px bg-zinc-200 dark:bg-zinc-800 mb-12"></div>

    <section v-if="posts.length > 0">
      <div class="flex justify-between items-end mb-8">
        <h2 class="text-xl font-bold uppercase tracking-tighter">{{ isSearching ? 'Filtered Results' : 'Index' }}</h2>
        <div v-if="isSearching" class="text-xs text-zinc-500">
          Found {{ totalElements }} matching results
        </div>
      </div>
      <div class="grid grid-cols-1 gap-8">
        <ArticleCard v-for="post in posts" :key="post.id" :post="post" />
      </div>
    </section>

    <!-- 分页组件（与 CategoryView 一致，包含跳转输入框） -->
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

      <div class="flex items-center gap-2">
        <span class="text-xs text-zinc-500 font-mono">Jump to</span>
        <input
            type="number"
            v-model.number="jumpPage"
            :min="1"
            :max="totalPages"
            class="w-20 px-3 py-2 text-center border border-zinc-200 dark:border-zinc-800 rounded-md bg-transparent text-sm font-mono focus:outline-none focus:ring-1 focus:ring-zinc-400 dark:focus:ring-zinc-600"
            @keyup.enter="handleJump"
            :disabled="loading"
        />
        <span class="text-xs text-zinc-500 font-mono">page</span>
        <button
            @click="handleJump"
            :disabled="loading"
            class="px-3 py-2 border border-zinc-200 dark:border-zinc-800 rounded-md text-sm font-mono uppercase tracking-tighter hover:bg-zinc-100 dark:hover:bg-zinc-800 transition disabled:opacity-40 disabled:cursor-not-allowed"
        >
          GO
        </button>
      </div>
    </div>

    <div v-else-if="posts.length === 0 && !loading"
         class="text-center py-24 border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/50">
      <h3 class="font-mono text-zinc-500 mb-2 uppercase tracking-widest text-sm">
        {{ isSearching ? 'No matching posts found' : 'Empty Directory' }}
      </h3>
      <p class="text-zinc-600 dark:text-zinc-400">
        {{ isSearching ? 'Try a different keyword.' : 'Waiting for content to be indexed.' }}
      </p>
    </div>
  </div>
</template>