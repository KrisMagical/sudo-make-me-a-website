<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import type { SidebarDto, PageTreeNodeDto, CategoryDto, SiteConfigDto } from '@/types/api'
import SocialLinks from '@/components/public/SocialLinks.vue'
import { useThemeStore } from '@/stores/themeStore'

const route = useRoute()
const isMobile = ref(false)
const isSidebarOpen = ref(false) // 控制移动端（遮罩层平移）
const isPcCollapsed = ref(false)
const pagesSectionExpanded = ref(false)
const themeStore = useThemeStore()

// 侧边栏数据
const sidebarData = ref<SidebarDto | null>(null)
const loading = ref(true)

// 计算属性
const pages = computed(() => sidebarData.value?.pages || [])
const categories = computed(() => sidebarData.value?.categories || [])
const siteConfig = computed(() => sidebarData.value?.siteConfig || {
  id: 0,
  siteName: 'My Blog',
  authorName: 'Author',
  siteAvatarUrl: '',
  footerText: '',
  metaDescription: '',
  metaKeywords: '',
  copyrightText: '',
  isActive: true
})

// 展开/收起的页面 ID
const expandedPageIds = ref<Set<number>>(new Set())

// 修复后的 isActive 函数
const isActive = (item: { slug: string; type: 'page' | 'category' }) => {
  if (item.type === 'page') {
    return route.name === 'page' && route.params.slug === item.slug
  } else {
    return route.name === 'category' && route.params.slug === item.slug
  }
}

// 获取侧边栏完整数据
const fetchSidebarData = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/sidebar')
    sidebarData.value = response
    autoExpandCurrentPage()
  } catch (error) {
    console.error('Failed to fetch sidebar data:', error)
    await fetchFallbackData()
  } finally {
    loading.value = false
  }
}

const fetchFallbackData = async () => {
  try {
    const [pagesResponse, categoriesResponse, siteConfigResponse] = await Promise.all([
      request.get('/api/pages').catch(() => []),
      request.get('/api/categories').catch(() => []),
      request.get('/api/sidebar/site-config').catch(() => null)
    ])

    const pageTree = buildPageTree(pagesResponse)

    sidebarData.value = {
      siteConfig: siteConfigResponse || {
        id: 0,
        siteName: 'My Blog',
        authorName: 'Author',
        siteAvatarUrl: '',
        footerText: '',
        metaDescription: '',
        metaKeywords: '',
        copyrightText: '',
        isActive: true
      },
      pages: pageTree,
      categories: categoriesResponse || [],
      browserIcon: {
        id: 0,
        faviconUrl: '/favicon.ico',
        appleTouchIconUrl: '/apple-touch-icon.png',
        isActive: true
      }
    }
  } catch (error) {
    console.error('Fallback data fetch failed:', error)
  }
}

// 构建页面树形结构
const buildPageTree = (flatPages: any[]): PageTreeNodeDto[] => {
  if (!flatPages || flatPages.length === 0) return []

  const pageMap = new Map<number, any>()
  const rootPages: any[] = []

  flatPages.forEach(page => {
    const node: PageTreeNodeDto = {
      id: page.id,
      slug: page.slug,
      title: page.title,
      content: page.content || '',
      parentId: page.parentId,
      orderIndex: page.orderIndex || 0,
      children: [],
      hasChildren: false,
      depth: 0
    }
    pageMap.set(page.id, node)
  })

  flatPages.forEach(page => {
    const node = pageMap.get(page.id)
    if (page.parentId && page.parentId !== null) {
      const parent = pageMap.get(page.parentId)
      if (parent) {
        parent.children.push(node)
        parent.hasChildren = true
        node.depth = (parent.depth || 0) + 1
      } else {
        rootPages.push(node)
      }
    } else {
      rootPages.push(node)
    }
  })

  const sortPages = (pages: any[]) => {
    return pages.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
  }

  const sortTree = (pageList: any[]) => {
    pageList.forEach(page => {
      if (page.children && page.children.length > 0) {
        page.children = sortPages(page.children)
        sortTree(page.children)
      }
    })
    return sortPages(pageList)
  }

  return sortTree(rootPages)
}

// 自动展开当前页面的父级页面
const autoExpandCurrentPage = () => {
  if (route.name !== 'page') return

  const currentSlug = route.params.slug as string
  if (!currentSlug) return

  const findPageAndParents = (pageList: PageTreeNodeDto[], targetSlug: string): number[] => {
    for (const page of pageList) {
      if (page.slug === targetSlug) {
        return [page.id]
      }
      if (page.children && page.children.length > 0) {
        const childResult = findPageAndParents(page.children, targetSlug)
        if (childResult.length > 0) {
          return [page.id, ...childResult]
        }
      }
    }
    return []
  }

  const pageIds = findPageAndParents(pages.value, currentSlug)
  pageIds.slice(0, -1).forEach(id => {
    expandedPageIds.value.add(id)
  })
}

// 切换页面展开状态
const togglePage = (pageId: number, hasChildren: boolean) => {
  if (!hasChildren) return
  if (expandedPageIds.value.has(pageId)) {
    expandedPageIds.value.delete(pageId)
  } else {
    expandedPageIds.value.add(pageId)
  }
}

// 响应式处理
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  // 可选：切换到移动端时自动展开 PC 折叠的侧边栏，避免状态混乱
  if (isMobile.value) {
    isPcCollapsed.value = false
  }
}

// 渲染页面树
const renderPageTree = (pageList: PageTreeNodeDto[], depth = 0) => {
  return pageList.map(page => {
    const isExpanded = expandedPageIds.value.has(page.id)
    const hasChildren = page.children && page.children.length > 0

    return {
      id: page.id,
      slug: page.slug,
      title: page.title,
      type: 'page' as const,
      depth,
      hasChildren,
      isExpanded,
      children: hasChildren && isExpanded ? renderPageTree(page.children, depth + 1) : []
    }
  })
}

const renderedPages = computed(() => renderPageTree(pages.value))

onMounted(() => {
  fetchSidebarData()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

// 监听路由变化，更新高亮状态
watch(() => route.fullPath, () => {
  expandedPageIds.value.clear()
  autoExpandCurrentPage()
})

// 移动端切换侧边栏
const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

// 点击链接时在移动端关闭侧边栏
const handleLinkClick = () => {
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
}
</script>

<template>
  <!-- 移动端汉堡菜单按钮 -->
  <button
    v-if="isMobile"
    @click="toggleSidebar"
    class="fixed top-4 left-4 z-50 p-2 bg-white dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-md shadow-sm"
  >
    <div class="w-6 h-6 flex flex-col justify-center gap-1">
      <span class="w-full h-0.5 bg-current"></span>
      <span class="w-full h-0.5 bg-current"></span>
      <span class="w-full h-0.5 bg-current"></span>
    </div>
  </button>

  <!-- PC 端悬浮小按钮 (左下角) -->
  <button
    v-if="!isMobile"
    @click="isPcCollapsed = !isPcCollapsed"
    class="fixed bottom-6 left-6 z-[60] p-2 flex items-center justify-center bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 rounded-full shadow-sm hover:shadow group transition-all duration-300"
    title="切换侧边栏"
  >
    <div
      class="w-5 h-5 text-zinc-500 dark:text-zinc-400 group-hover:text-zinc-900 dark:group-hover:text-zinc-100 transition-transform duration-300"
      :class="isPcCollapsed ? 'i-carbon-chevron-right' : 'i-carbon-chevron-left'"
    ></div>
  </button>

  <!-- 遮罩层 - 设置 z-40，低于侧边栏的 z-50 -->
  <div
    v-if="isMobile && isSidebarOpen"
    @click="toggleSidebar"
    class="fixed inset-0 bg-black/50 z-40"
  ></div>

  <!-- 侧边栏主体 -->
  <aside
    :class="[
      'sidebar transition-all duration-500 cubic-bezier(0.4, 0, 0.2, 1) z-50 flex-shrink-0',
      // 移动端: 默认 w-64，采用 translate 平移控制悬浮层
      'fixed inset-y-0 left-0 w-64 shadow-lg',
      isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
      // PC 端: 取消平移，采用 sticky 占位，通过控制宽度实现页面重排
      'md:sticky md:top-0 md:h-screen md:shadow-none md:translate-x-0',
      // PC 端收缩状态控制
      isPcCollapsed
        ? 'md:w-0 md:opacity-0 md:border-none'
        : 'md:w-64 md:opacity-100 md:border-r'
    ]"
    class="bg-white dark:bg-zinc-950 border-zinc-100 dark:border-zinc-700 overflow-hidden"
      >
        <div class="sidebar-scroll-container h-full overflow-y-auto p-6 pb-20 w-64">
      <!-- 用户信息区 -->
      <div class="mb-8">
        <router-link
          to="/"
          @click="handleLinkClick"
          class="flex items-center gap-3 mb-6 group"
        >
          <div class="w-10 h-10 rounded-lg bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center overflow-hidden">
            <img
              v-if="siteConfig.siteAvatarUrl"
              :src="siteConfig.siteAvatarUrl"
              alt="Avatar"
              class="w-full h-full object-cover"
            >
            <div v-else class="text-xl font-bold text-zinc-400">
              {{ siteConfig.siteName.charAt(0) }}
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <h2 class="font-bold text-lg truncate group-hover:text-zinc-900 dark:group-hover:text-white transition-colors">
              {{ siteConfig.siteName }}
            </h2>
            <p class="text-sm text-zinc-500 truncate">
              {{ siteConfig.authorName }}
            </p>
          </div>
        </router-link>
      </div>

      <!-- 静态页面导航 -->
      <nav class="mb-8">
        <button
          @click="pagesSectionExpanded = !pagesSectionExpanded"
          class="w-full flex items-center justify-between text-xs font-semibold uppercase tracking-widest text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300 mb-3 transition-colors"
        >
          <span>Pages</span>
          <span :class="pagesSectionExpanded ? 'i-carbon-chevron-up' : 'i-carbon-chevron-down'" class="text-base"></span>
        </button>

        <div v-if="pagesSectionExpanded">
          <div v-if="loading" class="space-y-1">
            <div v-for="i in 3" :key="i" class="h-8 bg-zinc-100 dark:bg-zinc-800 animate-pulse rounded"></div>
          </div>

          <ul v-else class="space-y-1 max-h-[340px] overflow-y-auto pr-2 custom-scroll">
            <li v-for="page in renderedPages" :key="page.id">
              <!-- 原有页面项代码保持完全不变（包括展开箭头、子页面等） -->
              <div class="flex items-center">
                <router-link
                  :to="`/page/${page.slug}`"
                  @click="handleLinkClick"
                  :class="[
                    'flex-1 py-2 px-3 rounded-md transition-colors text-sm',
                    'hover:bg-zinc-100 dark:hover:bg-zinc-900',
                    {
                      'font-medium bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-white': isActive(page),
                      'text-zinc-700 dark:text-zinc-300': !isActive(page)
                    }
                  ]"
                  :style="{ paddingLeft: `${page.depth * 20 + 12}px` }"
                >
                  {{ page.title }}
                </router-link>

                <button
                  v-if="page.hasChildren"
                  @click="togglePage(page.id, page.hasChildren)"
                  class="p-1 ml-1 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-200 transition-colors"
                >
                  <div v-if="page.isExpanded" class="w-4 h-4 i-carbon-chevron-down"></div>
                  <div v-else class="w-4 h-4 i-carbon-chevron-right"></div>
                </button>
              </div>

              <!-- 子页面（原有 transition 保持不变） -->
              <transition name="slide">
                <ul v-if="page.children && page.children.length > 0 && page.isExpanded" class="mt-1">
                  <li v-for="child in page.children" :key="child.id">
                    <router-link
                      :to="`/page/${child.slug}`"
                      @click="handleLinkClick"
                      :class="[
                        'block py-2 px-3 rounded-md transition-colors text-sm ml-4',
                        'hover:bg-zinc-100 dark:hover:bg-zinc-900',
                        {
                          'font-medium bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-white': isActive(child),
                          'text-zinc-700 dark:text-zinc-300': !isActive(child)
                        }
                      ]"
                      :style="{ paddingLeft: `${child.depth * 20 + 12}px` }"
                    >
                      {{ child.title }}
                    </router-link>
                  </li>
                </ul>
              </transition>
            </li>
          </ul>
        </div>
      </nav>

      <!-- 分类导航 -->
      <nav class="mb-8">
        <h3 class="text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-3">
          Categories
        </h3>
        <div v-if="loading" class="space-y-1">
          <div v-for="i in 3" :key="i" class="h-8 bg-zinc-100 dark:bg-zinc-800 animate-pulse rounded"></div>
        </div>
        <ul v-else class="space-y-1">
          <li v-for="category in categories" :key="category.id">
            <router-link
              :to="`/category/${category.slug}`"
              @click="handleLinkClick"
              :class="[
                'block py-2 px-3 rounded-md transition-colors text-sm',
                'hover:bg-zinc-100 dark:hover:bg-zinc-900',
                {
                  'font-medium bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-white': isActive({ slug: category.slug, type: 'category' }),
                  'text-zinc-700 dark:text-zinc-300': !isActive({ slug: category.slug, type: 'category' })
                }
              ]"
            >
              {{ category.name }}
            </router-link>
          </li>
        </ul>
      </nav>

      <!-- 分隔线 -->
      <div class="border-t border-zinc-100 dark:border-zinc-700 my-6"></div>

      <!-- 社交链接 -->
      <div v-if="!loading" class="mb-6">
        <h3 class="text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-3">
          Connect
        </h3>
        <SocialLinks variant="sidebar" />
      </div>

      <div class="mb-6">
        <h3 class="text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-3">
          Appearance
        </h3>
        <button
          @click="themeStore.toggleTheme"
          class="w-full flex items-center justify-between px-3 py-2 text-sm border border-zinc-200 dark:border-zinc-700 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-900 transition-colors"
        >
          <span>{{ themeStore.isDark ? 'Dark Mode' : 'Light Mode' }}</span>
          <div :class="themeStore.isDark ? 'i-carbon-moon' : 'i-carbon-sun'" class="w-5 h-5" />
        </button>
      </div>

      <!-- 分隔线 -->
      <div class="border-t border-zinc-100 dark:border-zinc-700 my-6"></div>

      <!-- 版权信息 -->
      <div class="text-xs text-zinc-500">
        <p v-if="siteConfig.copyrightText" class="mb-2">{{ siteConfig.copyrightText }}</p>
        <p v-else>© {{ new Date().getFullYear() }} {{ siteConfig.siteName }}</p>
      </div>
    </div>
  </aside>
</template>

<style scoped>
/* 侧边栏容器：隐藏滚动条但保留滚动功能 */
.sidebar-scroll-container {
  /* Firefox */
  scrollbar-width: none;
  /* IE/Edge */
  -ms-overflow-style: none;
  /* 平滑滚动体验 */
  scroll-behavior: smooth;
  /* 避免内容在宽度变化时折行 */
  white-space: nowrap;
}

/* Chrome, Safari, Edge (Blink 引擎) */
.sidebar-scroll-container::-webkit-scrollbar {
  width: 4px; /* 极细的滚动条 */
  display: none; /* 默认完全隐藏 */
}

/* 仅在鼠标悬浮在侧边栏时才显示滚动条轨道，增强交互感 */
.sidebar-scroll-container:hover::-webkit-scrollbar {
  display: block;
}

.sidebar-scroll-container::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.3); /* 淡灰色半透明 */
  border-radius: 10px;
}

.sidebar-scroll-container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.5);
}

/* --- 原有的动画类保持不变 --- */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-10px);
}

.slide-enter-to,
.slide-leave-from {
  max-height: 500px;
  opacity: 1;
  transform: translateY(0);
}

.custom-scroll::-webkit-scrollbar {
  width: 3px;
}
.custom-scroll::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.4);
  border-radius: 10px;
}
</style>
