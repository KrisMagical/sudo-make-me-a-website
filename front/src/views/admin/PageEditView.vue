<!-- src/views/admin/PageEditView.vue -->
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { pagesApi } from '@/api/pages'
import type { PageDto } from '@/types/api'
import Editor from '@/components/admin/Editor.vue'
import MediaLibrary from '@/components/admin/MediaLibrary.vue'
import { buildIndentedList } from '@/utils/tree'
import { notify } from '@/utils/feedback'

const DRAFT_SLUG = '00100000'

const route = useRoute()
const router = useRouter()
const page = ref<PageDto | null>(null)
const allPages = ref<PageDto[]>([])
const loading = ref(false)
const saving = ref(false)

// 父页面搜索关键词（仍保留父页面选择功能）
const parentSearch = ref('')

const editorRef = ref<InstanceType<typeof Editor>>()
const mediaLibraryRef = ref<InstanceType<typeof MediaLibrary> | null>(null)

const onImageUploaded = () => {
  mediaLibraryRef.value?.fetchImages()
}

const isEditing = computed(() => route.name === 'admin-page-edit')

const createdAtLocal = computed({
  get: () => {
    const val = page.value?.createdAt;
    if (!val) return '';
    let date: Date;
    if (Array.isArray(val)) {
      date = new Date(val[0], val[1] - 1, val[2], val[3] || 0, val[4] || 0, val[5] || 0);
    } else {
      date = new Date(val);
    }
    if (isNaN(date.getTime())) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  },
  set: (val: string) => {
    if (page.value) {
      page.value.createdAt = val ? `${val}:00` : '';
    }
  }
})

const currentDbSlug = computed(() => isEditing.value ? (route.params.slug as string) : DRAFT_SLUG)

const indentedPages = computed(() => buildIndentedList(allPages.value, page.value?.id))

const collectAncestors = (pageId: number, pagesList: PageDto[]): Set<number> => {
  const ancestors = new Set<number>()
  let current = pagesList.find(p => p.id === pageId)
  while (current && current.parentId) {
    ancestors.add(current.parentId)
    current = pagesList.find(p => p.id === current.parentId)
  }
  return ancestors
}

const filteredParentOptions = computed(() => {
  const kw = parentSearch.value.trim()
  if (!kw) {
    if (page.value?.parentId) {
      return indentedPages.value.filter(item => item.id === page.value!.parentId)
    }
    return []
  }
  const lowerKw = kw.toLowerCase()
  const matchedIds = new Set<number>()
  for (const p of allPages.value) {
    if (
        p.id !== page.value?.id &&
        (p.title?.toLowerCase().includes(lowerKw) || p.slug?.toLowerCase().includes(lowerKw))
    ) {
      matchedIds.add(p.id)
    }
  }
  if (matchedIds.size === 0) return []
  const ancestorIds = new Set<number>()
  for (const id of matchedIds) {
    const ancestors = collectAncestors(id, allPages.value)
    ancestors.forEach(a => ancestorIds.add(a))
  }
  const visibleIds = new Set([...matchedIds, ...ancestorIds])
  return indentedPages.value.filter(item => visibleIds.has(item.id))
})

const fetchData = async () => {
  loading.value = true
  try {
    const pagesList = await pagesApi.list()
    allPages.value = pagesList.filter(p => p.slug !== DRAFT_SLUG)

    if (isEditing.value && route.params.slug) {
      page.value = await pagesApi.get(route.params.slug as string)
    } else {
      const draftExists = pagesList.find(p => p.slug === DRAFT_SLUG)
      if (draftExists) {
        page.value = await pagesApi.get(DRAFT_SLUG)
      } else {
        const newDraft: PageDto = {
          id: 0,
          slug: DRAFT_SLUG,
          title: DRAFT_SLUG,
          content: '',
          parentId: null,
          orderIndex: 0,
          images: [],
          videos: []
        }
        const rootPages = pagesList.filter(p => p.parentId === null && p.slug !== DRAFT_SLUG)
        if (rootPages.length > 0) {
          const maxOrder = Math.max(...rootPages.map(p => p.orderIndex))
          newDraft.orderIndex = maxOrder + 1
        }
        page.value = await pagesApi.create(newDraft)
      }
      if (page.value) {
        if (page.value.title === DRAFT_SLUG) page.value.title = ''
        if (page.value.slug === DRAFT_SLUG) page.value.slug = ''
      }
    }
  } catch (error) {
    notify('Failed to load data', 'error')
  } finally {
    loading.value = false
  }
}

const handleParentChange = () => {
  if (!page.value) return
  const siblings = allPages.value.filter(
      p => p.parentId === page.value!.parentId && p.id !== page.value!.id
  )
  page.value.orderIndex = siblings.length > 0
      ? Math.max(...siblings.map(p => p.orderIndex)) + 1
      : 0
}

const generateSlug = () => {
  if (!page.value?.title || (page.value.slug && isEditing.value)) return
  page.value.slug = page.value.title
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
}

const save = async () => {
  if (!page.value?.title || !page.value?.slug || page.value.slug === DRAFT_SLUG) {
    notify('Please enter a valid title to generate a slug', 'error')
    return
  }
  saving.value = true
  try {
    const targetSlug = isEditing.value ? (route.params.slug as string) : DRAFT_SLUG
    const savedPage = await pagesApi.update(targetSlug, page.value)
    page.value = savedPage

    if (editorRef.value) {
      await editorRef.value.processPendingUploads(savedPage.id, savedPage.slug)
    }

    const finalPage = await pagesApi.update(savedPage.slug, page.value)
    page.value = finalPage

    notify(isEditing.value ? 'Page updated & structure synced' : 'Page created & structure synced', 'success')

    if (!isEditing.value) {
      await router.push({ name: 'admin-page-edit', params: { slug: finalPage.slug } })
    } else if (route.params.slug !== finalPage.slug) {
      await router.replace({ name: 'admin-page-edit', params: { slug: finalPage.slug } })
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      notify('Session expired. Please login again.', 'error')
      router.push('/admin/login')
    } else {
      let message = isEditing.value ? 'Failed to update page' : 'Failed to create page'
      if (error.response?.data?.error) message = error.response.data.error
      else if (error.response?.data?.message) message = error.response.data.message
      else if (error.message) message = error.message
      notify(message, 'error')
    }
  } finally {
    saving.value = false
  }
}

const discardChanges = async () => {
  const msg = isEditing.value ? 'Discard all unsaved changes?' : 'Cancel and delete this draft?'
  if (confirm(msg)) {
    if (isEditing.value) {
      fetchData()
    } else {
      try {
        await pagesApi.delete(DRAFT_SLUG)
        router.push('/admin/pages')
      } catch (e) {
        notify('Failed to delete draft page', 'error')
      }
    }
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="space-y-8">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">
        {{ isEditing ? 'EDIT PAGE' : 'NEW PAGE' }}
      </h2>
      <div class="flex gap-2">
        <button
            @click="discardChanges"
            class="px-3 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 text-sm font-bold uppercase tracking-tighter"
        >
          {{ isEditing ? 'Discard' : 'Cancel' }}
        </button>
        <button
            @click="save"
            :disabled="saving || !page"
            class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 text-sm font-bold uppercase tracking-tighter"
        >
          {{ saving ? 'Saving...' : isEditing ? 'Update Page' : 'Create Page' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading...</div>

    <div v-else-if="page" class="grid grid-cols-1 lg:grid-cols-4 gap-8">
      <div class="lg:col-span-3 space-y-6">
        <input
            v-model="page.title"
            class="w-full bg-transparent border-b border-zinc-300 dark:border-zinc-700 px-0 py-2 text-3xl font-bold outline-none"
            placeholder="Page Title"
            @blur="generateSlug"
        />

        <div class="flex items-center space-x-2 text-sm text-zinc-500 font-mono">
          <span>/pages/</span>
          <input
              v-model="page.slug"
              class="bg-transparent border-b border-zinc-300 dark:border-zinc-700 outline-none flex-1"
              placeholder="auto-generated"
          />
        </div>

        <!-- Parent Page Selection with Search -->
        <div class="space-y-2">
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Parent Page</label>
          <input
              v-model="parentSearch"
              type="text"
              placeholder="Search parent page..."
              class="w-full mb-2 px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-700 bg-transparent focus:outline-none focus:border-zinc-500 transition-colors"
          />
          <select
              v-model="page.parentId"
              @change="handleParentChange"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none appearance-none"
          >
            <option :value="null">-- ROOT PAGE --</option>
            <option v-if="!parentSearch && !page.parentId" disabled value="">
              [ Type to search parent page ]
            </option>
            <option
                v-for="item in filteredParentOptions"
                :key="item.id"
                :value="item.id"
            >
              {{ '\u00A0'.repeat(item.level * 4) }}{{ item.level > 0 ? '└ ' : '' }}{{ item.title }}
            </option>
          </select>
          <p class="text-[10px] text-zinc-400 mt-1">
            Note: Links in content (e.g. [[slug]]) will override this on save.
          </p>
        </div>

        <Editor
            v-if="page"
            ref="editorRef"
            v-model="page.content"
            owner-type="PAGE"
            :owner-id="page.id || 0"
            :owner-slug="currentDbSlug"
            @image-uploaded="onImageUploaded"
        />

        <MediaLibrary
            v-if="page.id && page.id > 0"
            ref="mediaLibraryRef"
            owner-type="PAGE"
            :owner-id="page.id"
            :owner-slug="currentDbSlug"
        />
      </div>

      <div class="space-y-8">
        <div class="border border-zinc-200 dark:border-zinc-800 p-4 bg-zinc-50 dark:bg-zinc-900/50">
          <h4 class="text-xs font-bold uppercase tracking-widest mb-4 text-zinc-500">Page Settings</h4>
          <div class="space-y-4">
            <div>
              <label class="block text-xs uppercase text-zinc-400 mb-1">Order Index</label>
              <input
                  type="number"
                  v-model.number="page.orderIndex"
                  min="0"
                  class="w-full p-2 border border-zinc-300 dark:border-zinc-700 bg-transparent text-sm"
              />
              <p class="text-[10px] text-zinc-400 mt-1">
                Sort order among siblings (0 = first).
              </p>
            </div>
          </div>
        </div>

        <div class="space-y-2">
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Created Date</label>
          <input
              type="datetime-local"
              v-model="createdAtLocal"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500 font-mono text-sm"
          />
        </div>

        <div v-if="isEditing && page.slug" class="space-y-4">
          <router-link
              :to="`/page/${page.slug}`"
              target="_blank"
              class="block w-full text-center px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors text-sm font-bold uppercase tracking-tighter"
          >
            View Live Page
          </router-link>
          <div class="text-xs text-zinc-500">
            URL: <code class="font-mono">/page/{{ page.slug }}</code>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
