<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { collectionsApi } from '@/api/collections'
import type { PostGroupDto } from '@/types/api'
import { notify } from '@/utils/feedback'

const collections = ref<PostGroupDto[]>([])
const loading = ref(false)
const searchKeyword = ref('')
let fetchTimer: ReturnType<typeof setTimeout> | null = null

const fetchCollections = async () => {
  loading.value = true
  try {
    const kw = searchKeyword.value.trim()
    if (kw) {
      collections.value = await collectionsApi.search(kw)
    } else {
      collections.value = await collectionsApi.list()
    }
  } catch (error) {
    notify('Failed to load collections', 'error')
  } finally {
    loading.value = false
  }
}

watch(searchKeyword, () => {
  if (fetchTimer) clearTimeout(fetchTimer)
  fetchTimer = setTimeout(fetchCollections, 300)
})

const handleDelete = async (collection: PostGroupDto) => {
  const deletePosts = confirm(
      `Delete collection "${collection.name}"? Press "OK" to also delete all posts, "Cancel" to keep posts.`
  )
  try {
    await collectionsApi.delete(collection.id, deletePosts)
    notify('Collection deleted', 'success')
    fetchCollections()
  } catch (error) {
    notify('Failed to delete collection', 'error')
  }
}

onUnmounted(() => {
  if (fetchTimer) clearTimeout(fetchTimer)
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">COLLECTIONS</h2>
      <router-link to="/admin/collections/new" class="btn-primary text-sm">
        + NEW COLLECTION
      </router-link>
    </div>

    <div class="flex gap-2">
      <input
          v-model="searchKeyword"
          type="text"
          placeholder="Search collections by name..."
          class="flex-1 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
      />
      <button
          v-if="searchKeyword.trim()"
          @click="searchKeyword = ''"
          class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 text-sm font-bold uppercase tracking-tighter"
      >
        CLEAR
      </button>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading...</div>

    <div v-else>
      <div
          v-if="collections.length === 0"
          class="py-12 text-center italic opacity-50 border border-dashed border-zinc-300 dark:border-zinc-700 rounded-2xl"
      >
        {{ searchKeyword.trim() ? 'No collections match your search.' : 'No collections yet.' }}
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
        <tr class="border-b border-zinc-200 dark:border-zinc-700 text-xs uppercase tracking-widest text-zinc-400">
          <th class="py-3 font-normal">Name</th>
          <th class="py-3 font-normal w-32">Slug</th>
          <th class="py-3 font-normal w-32">Posts</th>
          <th class="py-3 font-normal w-40">Date</th>
          <th class="py-3 font-normal w-24 text-right">Actions</th>
        </tr>
        </thead>
        <tbody class="text-sm">
        <tr
            v-for="collection in collections"
            :key="collection.id"
            class="border-b border-zinc-100 dark:border-zinc-700 group"
        >
          <td class="py-4">
            <div class="flex items-center gap-3">
              <img
                  v-if="collection.coverImageUrl"
                  :src="collection.coverImageUrl"
                  alt="Cover"
                  class="w-8 h-8 object-cover rounded"
              />
              <span class="font-bold group-hover:underline cursor-pointer">{{ collection.name }}</span>
            </div>
          </td>
          <td class="py-4 font-mono text-xs text-zinc-500">{{ collection.slug }}</td>
          <td class="py-4">{{ collection.posts?.length || 0 }}</td>
          <td class="py-4 text-zinc-500">{{ new Date(collection.createdAt).toLocaleDateString() }}</td>
          <td class="py-4 text-right space-x-4">
            <router-link
                :to="`/admin/collections/edit/${collection.slug}`"
                class="hover:text-black dark:hover:text-white"
            >
              EDIT
            </router-link>
            <button @click="handleDelete(collection)" class="text-red-500 hover:font-bold">DEL</button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>