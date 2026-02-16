<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { socialsApi } from '@/api/socials'
import type { SocialDto } from '@/types/api'

interface Props {
  variant?: 'home' | 'sidebar' | 'footer'
  compact?: boolean
  showLabels?: boolean
  limit?: number
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'home',
  compact: false,
  showLabels: true,
  limit: 0
})

const socials = ref<SocialDto[]>([])
const loading = ref(false)

const fetchSocials = async () => {
  loading.value = true
  try {
    const data = await socialsApi.list()
    socials.value = props.limit > 0 ? data.slice(0, props.limit) : data
  } catch (error) {
    console.error('Failed to fetch social links:', error)
  } finally {
    loading.value = false
  }
}

// 根据变体确定样式
const getVariantStyles = () => {
  switch (props.variant) {
    case 'home':
      return {
        container: 'gap-4',
        item: 'px-3 py-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-900',
        iconSize: 'w-5 h-5',
        showLabel: true
      }
    case 'sidebar':
      return {
        container: 'gap-3 flex-col',
        item: 'p-2 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-900 w-full',
        iconSize: 'w-6 h-6',
        showLabel: false
      }
    case 'footer':
      return {
        container: 'gap-3',
        item: 'p-2 hover:opacity-80',
        iconSize: 'w-4 h-4',
        showLabel: props.showLabels
      }
    default:
      return {
        container: 'gap-3',
        item: 'p-2',
        iconSize: 'w-5 h-5',
        showLabel: props.showLabels
      }
  }
}

const styles = getVariantStyles()

onMounted(fetchSocials)
</script>

<template>
  <div v-if="loading" class="flex justify-center items-center py-4">
    <div class="flex space-x-2">
      <div v-for="i in 3" :key="i" class="w-6 h-6 bg-zinc-200 dark:bg-zinc-800 animate-pulse rounded-full"></div>
    </div>
  </div>

  <div v-else-if="socials.length > 0" :class="[
    'social-links-enhanced',
    styles.container,
    compact ? 'compact' : '',
    variant
  ]" class="flex items-center">
    <a
      v-for="social in socials"
      :key="social.id"
      :href="social.url"
      target="_blank"
      rel="noopener noreferrer"
      :title="social.description || social.name"
      :class="[
        styles.item,
        'group relative flex items-center transition-all duration-200',
        variant === 'sidebar' ? 'justify-center' : 'justify-start'
      ]"
    >
      <!-- 图标 -->
      <div class="flex-shrink-0">
        <div v-if="social.iconUrl || social.externalIconUrl" class="relative">
          <img
            :src="social.iconUrl || social.externalIconUrl"
            :alt="social.name"
            :class="[styles.iconSize, 'object-contain transition-transform group-hover:scale-110']"
          />
        </div>
        <div v-else :class="[styles.iconSize, 'bg-zinc-200 dark:bg-zinc-800 rounded flex items-center justify-center']">
          <span class="text-xs font-bold text-zinc-500">{{ social.name.charAt(0) }}</span>
        </div>
      </div>

      <!-- 标签（如果显示） -->
      <div v-if="styles.showLabel" class="ml-2">
        <span class="text-sm font-medium text-zinc-700 dark:text-zinc-300">
          {{ social.name }}
        </span>
        <span
          v-if="social.description && variant !== 'footer'"
          class="block text-xs text-zinc-500 dark:text-zinc-400 mt-0.5"
        >
          {{ social.description }}
        </span>
      </div>

      <!-- 悬停提示（用于不显示标签的情况） -->
      <div
        v-if="!styles.showLabel"
        class="absolute left-full top-1/2 transform -translate-y-1/2 ml-3 px-3 py-2 bg-zinc-900 dark:bg-zinc-800 text-white text-sm rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 min-w-[200px]"
      >
        <div class="font-bold mb-1">{{ social.name }}</div>
        <div v-if="social.description" class="text-zinc-300 text-xs">
          {{ social.description }}
        </div>
        <div class="text-xs text-zinc-400 mt-1 font-mono break-all max-w-xs">
          {{ social.url.replace(/^https?:\/\//, '') }}
        </div>
        <div class="absolute left-0 top-1/2 transform -translate-y-1/2 -ml-2 w-0 h-0 border-t-4 border-b-4 border-l-0 border-r-8 border-transparent border-r-zinc-900 dark:border-r-zinc-800"></div>
      </div>
    </a>
  </div>

  <div v-else class="text-center py-4 text-zinc-400 dark:text-zinc-600 text-sm">
    No social links available
  </div>
</template>

<style scoped>
.social-links-enhanced.home {
  @apply justify-center flex-wrap;
}

.social-links-enhanced.sidebar {
  @apply justify-center;
}

.social-links-enhanced.footer {
  @apply justify-center;
}

.social-links-enhanced.compact {
  @apply gap-2;
}

.social-links-enhanced.compact .item {
  @apply p-1;
}
</style>