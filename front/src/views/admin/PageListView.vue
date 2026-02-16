<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { pagesApi } from '@/api/pages'
import type { PageDto } from '@/types/api'
import { buildIndentedList } from '@/utils/tree'
import { notify } from '@/utils/feedback'

const pages = ref<PageDto[]>([])
const loading = ref(false)

// Use the computed property to ensure the list is always rebuilt when 'pages' changes
const treePages = computed(() => buildIndentedList(pages.value))

const fetchPages = async () => {
  loading.value = true
  try {
    pages.value = await pagesApi.list()
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

    <div v-if="loading" class="italic opacity-50">Loading pages...</div>

    <div v-else class="border border-zinc-200 dark:border-zinc-800">
      <table class="w-full text-left">
        <thead>
          <tr class="border-b border-zinc-200 dark:border-zinc-800 text-xs uppercase tracking-widest text-zinc-400">
            <th class="py-3 px-4 font-normal">Title</th>
            <th class="py-3 px-4 font-normal">Slug</th>
            <th class="py-3 px-4 font-normal text-center">Order</th>
            <th class="py-3 px-4 font-normal text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="page in treePages"
            :key="page.id"
            class="border-b border-zinc-100 dark:border-zinc-900 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors"
          >
            <td class="py-4 px-4">
              <div :style="{ paddingLeft: page.level ? `${page.level * 24}px` : '0' }">
                <span v-if="page.level > 0" class="mr-2 text-zinc-400 border-l border-b border-zinc-400 w-3 h-3 inline-block relative -top-1"></span>
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

    <div v-if="!loading && pages.length === 0" class="text-center py-12 text-zinc-500">
      No pages created yet
    </div>
  </div>
</template>