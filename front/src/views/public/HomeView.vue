<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { publicApi } from '@/api/public'
import type { HomeProfileDto, PostSummaryDto, CategoryDto } from '@/types/api'
import SmartContent from '@/components/public/SmartContent.vue'
import ArticleCard from '@/components/public/ArticleCard.vue'
import SearchModal from '@/components/public/SearchModal.vue'
import SocialLinks from '@/components/public/SocialLinks.vue'

const home = ref<HomeProfileDto | null>(null)
const recentPosts = ref<PostSummaryDto[]>([])
const categories = ref<CategoryDto[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const showSearch = ref(false)

const fetchData = async () => {
  loading.value = true
  error.value = null
  try {
    const [homeData, postsData, categoriesData] = await Promise.all([
      publicApi.getHome().catch(() => null),
      publicApi.getRecentPosts(6).catch(() => []),
      publicApi.getCategories().catch(() => [])
    ])
    home.value = homeData as HomeProfileDto | null
    recentPosts.value = postsData as PostSummaryDto[]
    categories.value = categoriesData as CategoryDto[]
  } catch (err) {
    console.error('Failed to fetch home data:', err)
    error.value = 'Unable to load page data, please try again later.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-if="loading" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="animate-pulse">loading...</div>
  </div>

  <div v-else-if="error" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="text-red-500 dark:text-red-400 mb-4">{{ error }}</div>
    <button @click="fetchData" class="px-4 py-2 border border-current hover:bg-zinc-100 dark:hover:bg-zinc-900 transition-colors">
      Retry
    </button>
  </div>

  <div v-else class="max-w-6xl mx-auto px-4 py-12">
    <header v-if="home" class="mb-20 relative">
      <div class="flex items-center justify-between">
        <h1 v-if="home.title" class="text-4xl font-bold tracking-tighter">{{ home.title }}</h1>
        <button
            @click="showSearch = true"
            class="flex items-center gap-2 text-sm font-bold text-zinc-400 hover:text-zinc-900 dark:hover:text-white transition-colors uppercase tracking-tighter"
            title="Search"
        >
          <span class="i-carbon-search w-5 h-5"></span>
          <span class="hidden sm:inline-block">Search</span>
        </button>
      </div>
      <SmartContent v-if="home.content" :content="home.content" class="text-lg leading-relaxed" />
    </header>

    <SearchModal v-if="showSearch" @close="showSearch = false" />

    <section v-if="recentPosts.length > 0" class="mb-20">
      <h2 class="text-2xl font-bold mb-8 pb-2 border-b border-zinc-100 dark:border-zinc-900">
        Recently released
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <ArticleCard v-for="post in recentPosts" :key="post.id" :post="post" />
      </div>
    </section>

    <section v-if="categories.length > 0">
      <h2 class="text-2xl font-bold mb-8 pb-2 border-b border-zinc-100 dark:border-zinc-900">
        Classification
      </h2>
      <div class="flex flex-wrap gap-3">
        <router-link
            v-for="category in categories"
            :key="category.id"
            :to="`/category/${category.slug}`"
            class="px-4 py-2 border border-zinc-200 dark:border-zinc-800 hover:border-zinc-900 dark:hover:border-zinc-600 transition-colors text-sm font-bold uppercase tracking-tighter"
        >
          {{ category.name }}
        </router-link>
      </div>
    </section>

    <section v-if="!loading && !error" class="mt-16 pt-8 border-t border-zinc-100 dark:border-zinc-900">
      <div class="max-w-6xl mx-auto px-4">
        <div class="text-center mb-10">
          <h2 class="text-2xl font-bold mb-2">Connect</h2>
          <p class="text-sm text-zinc-500 dark:text-zinc-400">
            Follow me on social platforms
          </p>
        </div>
        <SocialLinks variant="home" />
      </div>
    </section>

    <div v-if="!home && recentPosts.length === 0 && categories.length === 0"
         class="text-center py-20 border border-zinc-200 dark:border-zinc-800">
      <h3 class="text-lg font-bold mb-2">Welcome to the blog</h3>
      <p class="text-zinc-600 dark:text-zinc-400 mb-6">
        No content available. Please visit again later.
      </p>
    </div>
  </div>
</template>