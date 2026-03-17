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
const searchTerm = ref('')

const editorRef = ref<InstanceType<typeof Editor>>()
const mediaLibraryRef = ref<InstanceType<typeof MediaLibrary> | null>(null)

const onImageUploaded = () => {
  mediaLibraryRef.value?.fetchImages()
}

const isEditing = computed(() => route.name === 'admin-page-edit')

// 关键修复：获取当前数据库里真实存在的 slug。
const currentDbSlug = computed(() => isEditing.value ? (route.params.slug as string) : DRAFT_SLUG)

const indentedPages = computed(() => buildIndentedList(allPages.value, page.value?.id))

const availablePagesToLink = computed(() => {
  if (!allPages.value) return []
  return allPages.value.filter(p =>
    p.id !== page.value?.id &&
    p.slug !== DRAFT_SLUG && // 隐藏草稿页
    (searchTerm.value === '' ||
      p.title.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
      p.slug.includes(searchTerm.value))
  )
})

const fetchData = async () => {
  loading.value = true
  try {
    const pagesList = await pagesApi.list()
    // 过滤掉列表里的草稿
    allPages.value = pagesList.filter(p => p.slug !== DRAFT_SLUG)

    if (isEditing.value && route.params.slug) {
      page.value = await pagesApi.get(route.params.slug as string)
    } else {
      // 检查是否存在草稿
      const draftExists = pagesList.find(p => p.slug === DRAFT_SLUG)

      if (draftExists) {
        page.value = await pagesApi.get(DRAFT_SLUG)
      } else {
        // 创建空白草稿
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

      // 仅用于输入框展示，不影响底层接口使用的 currentDbSlug
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

const insertPageLink = (targetPage: PageDto) => {
  if (!page.value) return
  const linkText = ` [${targetPage.title}](/pages/${targetPage.slug}) `
  const textarea = document.querySelector('textarea')
  if (textarea && textarea.selectionStart !== undefined) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = page.value.content || ''
    page.value.content = text.substring(0, start) + linkText + text.substring(end)
    setTimeout(() => {
      textarea.focus()
      textarea.selectionStart = textarea.selectionEnd = start + linkText.length
    }, 0)
  } else {
    page.value.content = (page.value.content || '') + '\n' + linkText
  }
  notify(`Link inserted. Structure will update on save.`)
}

const save = async () => {
  if (!page.value?.title || !page.value?.slug || page.value.slug === DRAFT_SLUG) {
    notify('Please enter a valid title to generate a slug', 'error')
    return
  }
  saving.value = true
  try {
    const targetSlug = isEditing.value ? (route.params.slug as string) : DRAFT_SLUG

    // 第一次保存：将草稿转正或更新，获取真实 id
    const savedPage = await pagesApi.update(targetSlug, page.value)
    page.value = savedPage // 更新前端状态

    // 等待图片上传完成（此时编辑器内容已被替换为真实 URL）
    if (editorRef.value) {
      await editorRef.value.processPendingUploads(savedPage.id, savedPage.slug)
    }

    // 第二次保存：将包含真实图片 URL 的内容提交到服务器
    const finalPage = await pagesApi.update(savedPage.slug, page.value)
    page.value = finalPage

    notify(isEditing.value ? 'Page updated & structure synced' : 'Page created & structure synced', 'success')

    if (!isEditing.value) {
      await router.push({ name: 'admin-page-edit', params: { slug: finalPage.slug } })
    } else if (route.params.slug !== finalPage.slug) {
      // 若修改了 Slug，需要替换 URL
      await router.replace({ name: 'admin-page-edit', params: { slug: finalPage.slug } })
    }

    // 注意：不再调用 fetchData()，因为 page.value 已经是最新数据
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

        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Parent Page</label>
          <select
            v-model="page.parentId"
            @change="handleParentChange"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none appearance-none"
          >
            <option :value="null">-- ROOT PAGE --</option>
            <option
              v-for="item in indentedPages"
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

        <div class="border border-zinc-200 dark:border-zinc-800 p-4">
          <h4 class="text-xs font-bold uppercase tracking-widest mb-2 text-zinc-500">Insert Sub-Page</h4>
          <p class="text-xs text-zinc-400 mb-4">
            Click a page to insert it into content. It will automatically become a <strong>child page</strong> upon save.
          </p>
          <input
            v-model="searchTerm"
            placeholder="Search pages..."
            class="w-full mb-3 px-2 py-1 text-sm border border-zinc-300 dark:border-zinc-700 bg-transparent"
          />
          <div class="max-h-60 overflow-y-auto space-y-1 pr-1 scrollbar-thin">
            <button
              v-for="p in availablePagesToLink"
              :key="p.id"
              @click="insertPageLink(p)"
              class="w-full text-left px-3 py-2 text-sm hover:bg-zinc-100 dark:hover:bg-zinc-800 flex justify-between group transition-colors"
            >
              <span class="truncate">{{ p.title }}</span>
              <span class="opacity-0 group-hover:opacity-100 text-[10px] uppercase border border-zinc-400 px-1 rounded">Insert</span>
            </button>
            <div v-if="availablePagesToLink.length === 0" class="text-xs text-zinc-400 text-center py-2">
              No pages found.
            </div>
          </div>
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