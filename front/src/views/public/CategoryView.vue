<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import type { PostSummaryDto, CategoryDto } from '@/types/api'
import ArticleCard from '@/components/public/ArticleCard.vue'

const route = useRoute()
const posts = ref<PostSummaryDto[]>([])
const category = ref<CategoryDto | null>(null)
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const slug = route.params.slug as string
    posts.value = await publicApi.getPostsByCategory(slug)

    // 获取分类信息
    const categories = await publicApi.getCategories()
    category.value = categories.find(c => c.slug === slug) || null
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-if="loading" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="animate-pulse">Loading...</div>
  </div>

  <div v-else class="max-w-6xl mx-auto px-4 py-12">
    <!-- 分类标题 -->
    <header class="mb-12">
      <h1 class="text-4xl font-bold tracking-tighter mb-4">
        {{ category?.name || 'Category' }}
      </h1>
      <div class="text-sm text-zinc-500 font-mono uppercase tracking-tighter">
        {{ posts.length }} POSTS
      </div>
    </header>

    <!-- 文章列表 -->
    <div v-if="posts.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <ArticleCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-20 border border-zinc-200 dark:border-zinc-800">
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