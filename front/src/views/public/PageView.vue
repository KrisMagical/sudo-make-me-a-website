<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { publicApi } from '@/api/public'
import type { PageDto } from '@/types/api'
import SmartContent from '@/components/public/SmartContent.vue'

const route = useRoute()
const page = ref<PageDto | null>(null)
const loading = ref(true)

const fetchPage = async (slug: string) => {
  loading.value = true
  try {
    page.value = await publicApi.getPage(slug)
  } catch (error) {
    console.error('Failed to fetch page:', error)
    page.value = null
  } finally {
    loading.value = false
  }
}

// 初始加载
onMounted(() => {
  fetchPage(route.params.slug as string)
})

// 监听路由参数变化
watch(
  () => route.params.slug,
  (newSlug) => {
    if (newSlug) {
      fetchPage(newSlug as string)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div v-if="loading" class="max-w-2xl mx-auto py-20 px-4 text-center">
    <div class="animate-pulse">Loading...</div>
  </div>

  <div v-else-if="page" class="max-w-2xl mx-auto py-12 px-4">
    <header class="mb-8 border-b border-zinc-100 dark:border-zinc-800 pb-8">
      <h1 class="text-3xl font-bold tracking-tighter mb-4">{{ page.title }}</h1>
      <div class="text-xs text-zinc-500 font-mono uppercase tracking-tighter">
        PAGE / {{ page.slug }}
      </div>
    </header>

    <!-- 页面内容 -->
    <SmartContent :content="page.content" class="prose prose-lg leading-relaxed" />
  </div>

  <div v-else class="max-w-2xl mx-auto py-20 px-4 text-center">
    <h1 class="text-2xl font-bold mb-4">Page Not Found</h1>
    <p class="text-zinc-600 dark:text-zinc-400 mb-8">
      The page you're looking for doesn't exist.
    </p>
    <router-link to="/" class="text-sm font-bold uppercase tracking-tighter hover:underline">
      ← Back to Home
    </router-link>
  </div>
</template>