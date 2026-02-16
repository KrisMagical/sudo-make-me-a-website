<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { homeApi } from '@/api/home'
import type { HomeProfileDto } from '@/types/api'
import Editor from '@/components/admin/Editor.vue'
import MediaLibrary from '@/components/admin/MediaLibrary.vue'
import { notify } from '@/utils/feedback'

const home = ref<HomeProfileDto | null>(null)
const loading = ref(false)
const saving = ref(false)

const fetchHome = async () => {
  loading.value = true
  try {
    home.value = await homeApi.get()
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!home.value) return
  saving.value = true
  try {
    await homeApi.update(home.value)
    notify('Home page updated successfully', 'success')
  } catch (error) {
    notify('Failed to update home page', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(fetchHome)
</script>

<template>
  <div class="space-y-8">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">HOME_PAGE</h2>
      <button
        @click="save"
        :disabled="saving || !home"
        class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
      >
        {{ saving ? 'Saving...' : 'Save Changes' }}
      </button>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading home page...</div>

    <div v-else-if="home" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div class="lg:col-span-2 space-y-6">
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Title</label>
          <input
            v-model="home.title"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500 text-lg font-bold"
            placeholder="Site Title"
          />
        </div>

        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Content</label>
          <Editor
            v-model="home.content"
            owner-type="HOME"
            :owner-id="home.id"
          />
        </div>

        <MediaLibrary
          v-if="home.id"
          owner-type="HOME"
          :owner-id="home.id"
        />
      </div>

      <div class="space-y-6">
        <div class="text-xs text-zinc-400">
           Use the "ADD_VIDEO" button in the editor toolbar to insert YouTube or Bilibili videos.
        </div>
      </div>
    </div>

    <div v-else class="text-center py-12 text-zinc-500">
      Unable to load home page configuration
    </div>
  </div>
</template>