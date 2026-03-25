<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { pagesApi } from '@/api/pages'
import type { PageDto } from '@/types/api'
import { buildIndentedList } from '@/utils/tree'
import { notify } from '@/utils/feedback'

const pages = ref<PageDto[]>([])
const loading = ref(false)
const searchKeyword = ref('') // 搜索关键词

// 原始树形数据（扁平，带层级）
const rawTree = computed(() => buildIndentedList(pages.value))

// 根据关键词获取需要显示的页面 ID 集合（匹配项 + 所有祖先 + 所有子孙）
const visibleIds = computed(() => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return null

  const lowerKeyword = keyword.toLowerCase()
  const matchedIds = new Set<number>()

  // 第一步：找出所有直接匹配的页面 (标题或 slug 包含关键词)
  for (const page of pages.value) {
    if (
      page.title.toLowerCase().includes(lowerKeyword) ||
      page.slug.toLowerCase().includes(lowerKeyword)
    ) {
      matchedIds.add(page.id)
    }
  }

  if (matchedIds.size === 0) return new Set<number>()

  // 第二步：向上收集所有祖先
  const ancestors = new Set<number>()
  for (const id of matchedIds) {
    let current = pages.value.find(p => p.id === id)
    while (current && current.parentId) {
      if (!matchedIds.has(current.parentId)) {
        ancestors.add(current.parentId)
      }
      current = pages.value.find(p => p.id === current.parentId)
    }
  }

  // 第三步：向下收集所有子孙
  const descendants = new Set<number>()
  const queue = [...matchedIds]
  while (queue.length) {
    const currentId = queue.shift()!
    for (const p of pages.value) {
      if (p.parentId === currentId && !descendants.has(p.id) && !matchedIds.has(p.id)) {
        descendants.add(p.id)
        queue.push(p.id)
      }
    }
  }

  return new Set([...matchedIds, ...ancestors, ...descendants])
})

// 过滤后的树形列表：无搜索时返回空数组；有搜索时按可见ID过滤
const filteredTree = computed(() => {
  if (!searchKeyword.value.trim()) return []
  if (!visibleIds.value) return []
  return rawTree.value.filter(node => visibleIds.value!.has(node.id))
})

const fetchPages = async () => {
  loading.value = true
  try {
    const list = await pagesApi.list()
    // 隐藏草稿数据 00100000
    pages.value = list.filter(p => p.slug !== '00100000')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (page: PageDto) => {
  if (!confirm(`Delete page "${page.title}" and all its children? This action cannot be undone.`)) return

  try {
    await pagesApi.delete(page.slug)
    notify('Page deleted successfully')
    fetchPages()
  } catch (error) {
    notify('Failed to delete page', 'error')
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
}

onMounted(fetchPages)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">ALL_PAGES</h2>
      <router-link
        to="/admin/pages/new"
        class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-700 text-sm font-bold uppercase"
      >
        + New Page
      </router-link>
    </div>

    <!-- 搜索框 -->
    <div class="flex gap-2">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="Search by title or slug..."
        class="flex-1 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
      />
      <button
        v-if="searchKeyword"
        @click="clearSearch"
        class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900"
      >
        Clear
      </button>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading pages...</div>

    <div v-else-if="pages.length === 0" class="text-center py-12 text-zinc-500">
      No pages created yet
    </div>

    <!-- 未搜索时的提示 -->
    <div v-else-if="!searchKeyword.trim()" class="text-center py-12 text-zinc-500">
      Enter keywords to search pages...
    </div>

    <!-- 有搜索但无结果 -->
    <div v-else-if="filteredTree.length === 0" class="text-center py-12 text-zinc-500">
      No pages found matching "{{ searchKeyword }}"
    </div>

    <!-- 搜索结果表格 -->
    <div v-else class="border border-zinc-200 dark:border-zinc-700">
      <table class="w-full text-left">
        <thead>
          <tr class="border-b border-zinc-200 dark:border-zinc-700 text-xs uppercase tracking-widest text-zinc-400">
            <th class="py-3 px-4 font-normal">Title</th>
            <th class="py-3 px-4 font-normal">Slug</th>
            <th class="py-3 px-4 font-normal text-center">Order</th>
            <th class="py-3 px-4 font-normal text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="page in filteredTree"
            :key="page.id"
            class="border-b border-zinc-100 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors"
          >
            <td class="py-4 px-4">
              <div :style="{ paddingLeft: page.level ? `${page.level * 24}px` : '0' }">
                <span
                  v-if="page.level > 0"
                  class="mr-2 text-zinc-400 border-l border-b border-zinc-400 w-3 h-3 inline-block relative -top-1"
                ></span>
                <span class="font-bold">{{ page.title }}</span>
              </div>
            </td>
            <td class="py-4 px-4 font-mono text-sm text-zinc-500">
              {{ page.slug }}
            </td>
            <td class="py-4 px-4 text-center font-mono text-xs text-zinc-400">
              {{ page.orderIndex }}
            </td>
            <td class="py-4 px-4 text-right space-x-4">
              <router-link
                :to="`/admin/pages/edit/${page.slug}`"
                class="hover:text-black dark:hover:text-white text-sm font-medium"
              >
                EDIT
              </router-link>
              <button
                @click="handleDelete(page)"
                class="text-red-500 hover:text-red-700 hover:font-bold text-sm"
              >
                DEL
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>