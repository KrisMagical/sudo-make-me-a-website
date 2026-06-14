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

// 样式配置（仅 sidebar/footer 使用）
const getVariantStyles = () => {
  switch (props.variant) {
    case 'sidebar':
      return { container: 'gap-3 flex-wrap', item: 'p-2 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-900', iconSize: 'w-5 h-5', showLabel: false }
    case 'footer':
      return { container: 'gap-3', item: 'p-2 hover:opacity-80', iconSize: 'w-4 h-4', showLabel: props.showLabels }
    default:
      return { container: 'gap-3', item: 'p-2', iconSize: 'w-5 h-5', showLabel: props.showLabels }
  }
}

const styles = getVariantStyles()

// 悬浮提示（仅 sidebar 使用）
const hoveredSocial = ref<SocialDto | null>(null)
const tooltipStyle = ref({ top: '0px', left: '0px' })

const onMouseEnter = (e: MouseEvent, social: SocialDto) => {
  if (styles.showLabel) return
  hoveredSocial.value = social
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  tooltipStyle.value = { top: `${rect.top - 8}px`, left: `${rect.left + rect.width / 2}px` }
}

const onMouseLeave = () => { hoveredSocial.value = null }

onMounted(fetchSocials)
</script>

<template>
  <div v-if="loading" class="flex justify-center items-center py-4">
    <div class="flex space-x-2">
      <div v-for="i in 3" :key="i" class="w-6 h-6 bg-zinc-200 dark:bg-zinc-800 animate-pulse rounded-full"></div>
    </div>
  </div>

  <!-- ==================== HOME 卡片模式 ==================== -->
 <div v-else-if="variant === 'home' && socials.length > 0"
        class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
     <a
       v-for="social in socials"
       :key="social.id"
       :href="social.url"
       target="_blank"
       rel="noopener noreferrer"
       class="group border border-zinc-200 dark:border-zinc-600 hover:border-zinc-400 dark:hover:border-zinc-500 hover:shadow-md transition-all flex flex-col h-full bg-white dark:bg-zinc-950 p-5"
     >
       <!-- 图标 + 名称 -->
       <div class="flex items-center gap-3 mb-3">
         <div class="w-10 h-10 flex-shrink-0 bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center overflow-hidden ring-1 ring-zinc-100 dark:ring-zinc-700">
           <img
             v-if="social.iconUrl || social.externalIconUrl"
             :src="social.iconUrl || social.externalIconUrl"
             :alt="social.name"
             class="w-7 h-7 object-contain transition-transform group-hover:scale-110"
           />
           <span v-else class="text-3xl font-bold text-zinc-400">
             {{ social.name.charAt(0) }}
           </span>
         </div>
         <div class="flex-1 min-w-0">
           <h4 class="font-bold text-lg tracking-tight leading-none">{{ social.name }}</h4>
           <div class="text-[10px] font-mono text-zinc-500 truncate">
             {{ social.url }}
           </div>
         </div>
       </div>

       <!-- 描述 -->
       <p v-if="social.description" class="text-sm text-zinc-600 dark:text-zinc-400 mb-4 line-clamp-2 flex-1">
         {{ social.description }}
       </p>

       <!-- Visit 按钮 -->
       <div class="mt-auto flex items-center justify-between text-xs font-bold uppercase tracking-widest text-zinc-400 group-hover:text-zinc-900 dark:group-hover:text-white pt-2 border-t border-zinc-100 dark:border-zinc-800">
         <span>VISIT</span>
         <span class="text-base transition-transform group-hover:translate-x-0.5">→</span>
       </div>
     </a>
   </div>

  <!-- ==================== 原有 sidebar / footer 小图标模式 ==================== -->
  <div v-else-if="socials.length > 0"
       :class="['social-links-enhanced', styles.container, compact ? 'compact' : '', variant]"
       class="flex items-center">
    <a
      v-for="social in socials"
      :key="social.id"
      :href="social.url"
      target="_blank"
      rel="noopener noreferrer"
      @mouseenter="onMouseEnter($event, social)"
      @mouseleave="onMouseLeave"
      :class="[styles.item, 'group relative flex items-center transition-all duration-200']"
    >
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

      <div v-if="styles.showLabel" class="ml-2">
        <span class="text-sm font-medium text-zinc-700 dark:text-zinc-300">{{ social.name }}</span>
        <span v-if="social.description && variant !== 'footer'" class="block text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
          {{ social.description }}
        </span>
      </div>
    </a>
  </div>

  <div v-else class="text-center py-4 text-zinc-400 dark:text-zinc-600 text-sm">
    No social links available
  </div>

  <!-- Tooltip（仅 sidebar 使用） -->
  <Teleport to="body">
    <transition name="tooltip-fade">
      <div
        v-if="hoveredSocial"
        :style="tooltipStyle"
        class="fixed z-[99999] px-3 py-2 bg-zinc-900 dark:bg-zinc-800 text-white text-sm rounded shadow-xl pointer-events-none transform -translate-x-1/2 -translate-y-full w-max max-w-[200px] text-center"
      >
        <div class="font-bold">{{ hoveredSocial.name }}</div>
        <div v-if="hoveredSocial.description" class="text-zinc-300 text-xs mt-1 leading-tight">
          {{ hoveredSocial.description }}
        </div>
        <div class="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-[6px] border-r-[6px] border-t-[6px] border-transparent border-t-zinc-900 dark:border-t-zinc-800"></div>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
/* 新增悬浮气泡淡入淡出动画 */
.tooltip-fade-enter-active,
.tooltip-fade-leave-active {
  transition: opacity 0.15s ease-in-out;
}

.tooltip-fade-enter-from,
.tooltip-fade-leave-to {
  opacity: 0;
}

.social-links-enhanced.home {
  @apply justify-center flex-wrap;
}

.social-links-enhanced.sidebar {
  @apply justify-start flex-wrap;
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