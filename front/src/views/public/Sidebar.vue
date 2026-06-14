<script setup lang="ts">
import {ref, computed, onMounted, onUnmounted} from 'vue'
import request from '@/utils/request'
import type {SidebarDto, CategoryDto, PostSummaryDto, PostGroupDto} from '@/types/api'
import {collectionsApi} from '@/api/collections'
import SocialLinks from '@/components/public/SocialLinks.vue'
import {useThemeStore} from '@/stores/themeStore'

const isMobile = ref(false)
const isSidebarOpen = ref(false)
const isPcCollapsed = ref(false)
const themeStore = useThemeStore()

const sidebarData = ref<SidebarDto | null>(null)
const loading = ref(true)

let abortController: AbortController | null = null

const viewType = ref<'categories' | 'collections'>('categories')

const categories = ref<CategoryDto[]>([])
const collections = ref<PostGroupDto[]>([])

type CategoryPosts = {
  slug: string
  posts: PostSummaryDto[]
  page: number
  hasMore: boolean
  loading: boolean
}

type CollectionPosts = {
  id: number
  allPosts: PostSummaryDto[]   // 全部文章
  visibleCount: number          // 当前显示数量
  loading: boolean
}

const categoryPostsMap = ref<Map<string, CategoryPosts>>(new Map())
const collectionPostsMap = ref<Map<number, CollectionPosts>>(new Map())

// 展开/折叠状态
const expandedCategories = ref<Set<string>>(new Set())
const expandedCollections = ref<Set<number>>(new Set())

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

const fetchSidebarData = async () => {
  if (abortController) abortController.abort()
  abortController = new AbortController()
  const signal = abortController.signal

  loading.value = true
  try {
    const data = await request.get('/api/sidebar', {signal}) as unknown as SidebarDto
    sidebarData.value = data
  } catch (error: any) {
    if (error.name !== 'AbortError') {
      console.error('Failed to fetch sidebar data:', error)
      await fetchFallbackData()
    }
  } finally {
    loading.value = false
  }
}

const fetchFallbackData = async () => {
  try {
    const [categoriesRes, siteConfigRes] = await Promise.all([
      request.get('/api/categories').catch(() => []),
      request.get('/api/sidebar/site-config').catch(() => null)
    ])
    sidebarData.value = {
      siteConfig: (siteConfigRes as any) || {
        id: 0, siteName: 'My Blog', authorName: 'Author',
        siteAvatarUrl: '', footerText: '', metaDescription: '',
        metaKeywords: '', copyrightText: '', isActive: true
      },
      categories: (categoriesRes as any) || [],
      browserIcon: {
        id: 0,
        faviconImageId: null,
        faviconUrl: '',
        appleTouchIconImageId: null,
        appleTouchIconUrl: '',
        isActive: true
      }
    }
  } catch (error) {
    console.error('Fallback data fetch failed:', error)
  }
}

// 加载分类列表
const fetchCategories = async () => {
  try {
    const res = await request.get('/api/categories') as unknown as CategoryDto[]
    categories.value = res
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

// 加载合集列表
const fetchCollections = async () => {
  try {
    const res = await collectionsApi.list()
    collections.value = res
  } catch (error) {
    console.error('Failed to fetch collections:', error)
  }
}

// 切换 viewType
const switchViewType = (type: 'categories' | 'collections') => {
  viewType.value = type
  if (type === 'categories') {
    if (categories.value.length === 0) fetchCategories()
  } else {
    if (collections.value.length === 0) fetchCollections()
  }
}

// 加载某个分类的文章（分页）
const loadCategoryPosts = async (categorySlug: string, page = 0) => {
  const existing = categoryPostsMap.value.get(categorySlug)
  if (existing && existing.loading) return

  // 初始化或更新状态
  if (!existing) {
    categoryPostsMap.value.set(categorySlug, {
      slug: categorySlug,
      posts: [],
      page: 0,
      hasMore: true,
      loading: true
    })
  } else {
    existing.loading = true
  }

  try {
    // 每页显示 5 篇文章
    const size = 5
    const res = await request.get(`/api/posts/category/${categorySlug}`, {
      params: {page, size}
    }) as unknown as { content: PostSummaryDto[], totalElements: number, totalPages: number }
    const newPosts = res.content
    const current = categoryPostsMap.value.get(categorySlug)!
    const allPosts = page === 0 ? newPosts : [...current.posts, ...newPosts]
    categoryPostsMap.value.set(categorySlug, {
      ...current,
      posts: allPosts,
      page: page,
      hasMore: page + 1 < (res.totalPages || 0),
      loading: false
    })
  } catch (error) {
    console.error(`Failed to load posts for category ${categorySlug}`, error)
    const current = categoryPostsMap.value.get(categorySlug)
    if (current) current.loading = false
  }
}

// 加载某个合集的全部文章（一次获取全部）
const loadCollectionPosts = async (collectionId: number) => {
  const existing = collectionPostsMap.value.get(collectionId)
  if (existing && existing.loading) return

  if (!existing) {
    collectionPostsMap.value.set(collectionId, {
      id: collectionId,
      allPosts: [],
      visibleCount: 5,
      loading: true
    })
  } else {
    existing.loading = true
  }

  try {
    const collection = collections.value.find(c => c.id === collectionId)
    if (!collection) throw new Error('Collection not found')
    // 获取完整详情（包含所有文章）
    const detail = await collectionsApi.getBySlug(collection.slug)
    const allPosts = detail.posts || []
    collectionPostsMap.value.set(collectionId, {
      id: collectionId,
      allPosts,
      visibleCount: 5,
      loading: false
    })
  } catch (error) {
    console.error(`Failed to load posts for collection ${collectionId}`, error)
    const current = collectionPostsMap.value.get(collectionId)
    if (current) current.loading = false
  }
}

// 展开/折叠分类
const toggleCategory = (slug: string) => {
  if (expandedCategories.value.has(slug)) {
    expandedCategories.value.delete(slug)
  } else {
    expandedCategories.value.add(slug)
    // 未加载过文章则加载
    if (!categoryPostsMap.value.has(slug)) {
      loadCategoryPosts(slug, 0)
    }
  }
}

// 展开/折叠合集
const toggleCollection = (id: number) => {
  if (expandedCollections.value.has(id)) {
    expandedCollections.value.delete(id)
  } else {
    expandedCollections.value.add(id)
    if (!collectionPostsMap.value.has(id)) {
      loadCollectionPosts(id)
    }
  }
}

// 分类加载更多
const loadMoreCategory = (slug: string) => {
  const current = categoryPostsMap.value.get(slug)
  if (current && current.hasMore && !current.loading) {
    loadCategoryPosts(slug, current.page + 1)
  }
}

// 合集加载更多（增加 visibleCount）
const loadMoreCollection = (id: number) => {
  const current = collectionPostsMap.value.get(id)
  if (current && current.visibleCount < current.allPosts.length) {
    current.visibleCount = Math.min(current.visibleCount + 5, current.allPosts.length)
  }
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isPcCollapsed.value = false
  }
}

onMounted(() => {
  fetchSidebarData()
  fetchCategories()  // 预加载分类列表
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  if (abortController) abortController.abort()
  window.removeEventListener('resize', checkMobile)
})

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const handleLinkClick = () => {
  if (isMobile.value) {
    isSidebarOpen.value = false
  }
}
</script>

<template>
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

  <button
      v-if="!isMobile"
      @click="isPcCollapsed = !isPcCollapsed"
      class="fixed bottom-6 left-6 z-[60] p-2 flex items-center justify-center bg-white dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700 rounded-full shadow-sm hover:shadow group transition-all duration-300"
      title="Toggle Sidebar"
  >
    <div
        class="w-5 h-5 text-zinc-500 dark:text-zinc-400 group-hover:text-zinc-900 dark:group-hover:text-zinc-100 transition-transform duration-300"
        :class="isPcCollapsed ? 'i-carbon-chevron-right' : 'i-carbon-chevron-left'"
    ></div>
  </button>

  <div
      v-if="isMobile && isSidebarOpen"
      @click="toggleSidebar"
      class="fixed inset-0 bg-black/50 z-40"
  ></div>

  <aside
      :class="[
      'sidebar transition-all duration-500 ease-in-out z-50 flex-shrink-0',
      'fixed inset-y-0 left-0 w-64 shadow-lg',
      isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
      'md:sticky md:top-0 md:h-screen md:shadow-none md:translate-x-0',
      isPcCollapsed
        ? 'md:w-0 md:opacity-0 md:border-none'
        : 'md:w-64 md:opacity-100 md:border-r'
    ]"
      class="bg-white dark:bg-zinc-950 border-zinc-100 dark:border-zinc-700 overflow-hidden"
  >
    <div class="sidebar-scroll-container h-full overflow-y-auto p-6 pb-20 w-64">
      <!-- 用户信息 -->
      <div class="mb-8">
        <router-link to="/" @click="handleLinkClick" class="flex items-center gap-3 mb-6 group">
          <div
              class="w-10 h-10 rounded-lg bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center overflow-hidden"
          >
            <img
                v-if="siteConfig.siteAvatarUrl"
                :src="siteConfig.siteAvatarUrl"
                alt="Avatar"
                class="w-full h-full object-cover"
            />
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

      <!-- 切换按钮 -->
      <div class="flex border border-zinc-200 dark:border-zinc-700 mb-6">
        <button
            @click="switchViewType('categories')"
            :class="[
            'flex-1 py-2 text-xs font-bold uppercase tracking-widest transition-colors',
            viewType === 'categories'
              ? 'bg-zinc-900 text-white dark:bg-white dark:text-zinc-900'
              : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-white'
          ]"
        >
          Categories
        </button>
        <button
            @click="switchViewType('collections')"
            :class="[
            'flex-1 py-2 text-xs font-bold uppercase tracking-widest transition-colors',
            viewType === 'collections'
              ? 'bg-zinc-900 text-white dark:bg-white dark:text-zinc-900'
              : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-white'
          ]"
        >
          Collections
        </button>
      </div>

      <!-- 分类视图 -->
      <div v-if="viewType === 'categories'">
        <div v-if="loading && categories.length === 0" class="space-y-1">
          <div v-for="i in 3" :key="i" class="h-8 bg-zinc-100 dark:bg-zinc-800 animate-pulse rounded"></div>
        </div>
        <ul v-else class="space-y-3">
          <li v-for="category in categories" :key="category.id">
            <div
                class="flex items-center justify-between py-2 px-2 rounded-md group hover:bg-zinc-50 dark:hover:bg-zinc-900"
                @contextmenu.prevent="toggleCategory(category.slug)">
              <router-link :to="`/category/${category.slug}`" @click.stop
                           class="text-sm font-medium hover:underline flex-1">
                {{ category.name }}
              </router-link>
              <button
                  @click.stop="toggleCategory(category.slug)"
                  class="w-4 h-4 transition-transform cursor-pointer"
                  :class="expandedCategories.has(category.slug) ? 'rotate-90' : ''"
              >
                <div class="i-carbon-chevron-right"></div>
              </button>
            </div>
            <div v-if="expandedCategories.has(category.slug)"
                 class="ml-2 pl-3 border-l border-zinc-200 dark:border-zinc-800 mt-1 space-y-1">
              <!-- 文章列表 -->
              <div v-if="categoryPostsMap.get(category.slug)?.loading" class="text-xs text-zinc-400 pl-2">
                Loading...
              </div>
              <div v-else>
                <router-link
                    v-for="post in categoryPostsMap.get(category.slug)?.posts"
                    :key="post.id"
                    :to="`/post/${post.slug}`"
                    @click="handleLinkClick"
                    class="block text-sm text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-white py-1 pl-2 border-l-2 border-transparent hover:border-zinc-400 transition-colors"
                >
                  {{ post.title }}
                </router-link>
                <button
                    v-if="categoryPostsMap.get(category.slug)?.hasMore"
                    @click.stop="loadMoreCategory(category.slug)"
                    class="text-xs text-blue-500 hover:underline mt-1 pl-2"
                    :disabled="categoryPostsMap.get(category.slug)?.loading"
                >
                  more ⟶
                </button>
                <div
                    v-if="(!categoryPostsMap.get(category.slug)?.posts || categoryPostsMap.get(category.slug)?.posts.length === 0) && !categoryPostsMap.get(category.slug)?.loading"
                    class="text-xs text-zinc-400 pl-2">
                  No posts
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <!-- 合集视图 -->
      <div v-else>
        <div v-if="loading && collections.length === 0" class="space-y-1">
          <div v-for="i in 3" :key="i" class="h-8 bg-zinc-100 dark:bg-zinc-800 animate-pulse rounded"></div>
        </div>
        <ul v-else class="space-y-3">
          <li v-for="collection in collections" :key="collection.id">
            <div
                class="flex items-center justify-between py-2 px-2 rounded-md group hover:bg-zinc-50 dark:hover:bg-zinc-900"
                @contextmenu.prevent="toggleCollection(collection.id)">
              <router-link :to="`/collection/${collection.slug}`" @click.stop
                           class="text-sm font-medium hover:underline flex-1">
                {{ collection.name }}
              </router-link>
              <button
                  @click.stop="toggleCollection(collection.id)"
                  class="w-4 h-4 transition-transform cursor-pointer"
                  :class="expandedCollections.has(collection.id) ? 'rotate-90' : ''"
              >
                <div class="i-carbon-chevron-right"></div>
              </button>
            </div>
            <div v-if="expandedCollections.has(collection.id)" class="ml-4 mt-1 space-y-2">
              <div v-if="collectionPostsMap.get(collection.id)?.loading" class="text-xs text-zinc-400 pl-2">
                Loading...
              </div>
              <div v-else>
                <router-link
                    v-for="post in collectionPostsMap.get(collection.id)?.allPosts.slice(0, collectionPostsMap.get(collection.id)?.visibleCount)"
                    :key="post.id"
                    :to="`/post/${post.slug}`"
                    @click="handleLinkClick"
                    class="block text-sm text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-white py-1 pl-2 border-l-2 border-transparent hover:border-zinc-400 transition-colors"
                >
                  {{ post.title }}
                </router-link>
                <button
                    v-if="collectionPostsMap.get(collection.id) && collectionPostsMap.get(collection.id)!.visibleCount < collectionPostsMap.get(collection.id)!.allPosts.length"
                    @click.stop="loadMoreCollection(collection.id)"
                    class="text-xs text-blue-500 hover:underline mt-1 pl-2"
                    :disabled="collectionPostsMap.get(collection.id)?.loading"
                >
                  more ⟶
                </button>
                <div
                    v-if="(!collectionPostsMap.get(collection.id)?.allPosts || collectionPostsMap.get(collection.id)?.allPosts.length === 0) && !collectionPostsMap.get(collection.id)?.loading"
                    class="text-xs text-zinc-400 pl-2">
                  Empty collection
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div class="border-t border-zinc-100 dark:border-zinc-700 my-6"></div>

      <!-- 社交链接 -->
      <div v-if="!loading" class="mb-6">
        <h3 class="text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-3">
          Connect
        </h3>
        <SocialLinks variant="sidebar"/>
      </div>

      <!-- 外观切换 -->
      <div class="mb-6">
        <h3 class="text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-3">
          Appearance
        </h3>
        <button
            @click="themeStore.toggleTheme"
            class="w-full flex items-center justify-between px-3 py-2 text-sm border border-zinc-200 dark:border-zinc-700 rounded-md hover:bg-zinc-100 dark:hover:bg-zinc-900 transition-colors"
        >
          <span>{{ themeStore.isDark ? 'Dark Mode' : 'Light Mode' }}</span>
          <div :class="themeStore.isDark ? 'i-carbon-moon' : 'i-carbon-sun'" class="w-5 h-5"/>
        </button>
      </div>

      <div class="border-t border-zinc-100 dark:border-zinc-700 my-6"></div>

      <!-- 版权 -->
      <div class="text-xs text-zinc-500">
        <p v-if="siteConfig.copyrightText" class="mb-2">{{ siteConfig.copyrightText }}</p>
        <p v-else>© {{ new Date().getFullYear() }} {{ siteConfig.siteName }}</p>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-scroll-container {
  scrollbar-width: none;
  -ms-overflow-style: none;
  scroll-behavior: smooth;
}

.sidebar-scroll-container::-webkit-scrollbar {
  width: 4px;
  display: none;
}

.sidebar-scroll-container:hover::-webkit-scrollbar {
  display: block;
}

.sidebar-scroll-container::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.3);
  border-radius: 10px;
}

.sidebar-scroll-container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.5);
}
</style>