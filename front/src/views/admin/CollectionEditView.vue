<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { collectionsApi } from '@/api/collections'
import { postsApi } from '@/api/posts'
import type { PostGroupDto, PostSummaryDto } from '@/types/api'
import { notify } from '@/utils/feedback'

const DRAFT_SLUG = '00100000'

const route = useRoute()
const router = useRouter()

const isEditing = computed(() => route.name === 'admin-collection-edit')
const isDraftMode = ref(false)

const collection = ref<PostGroupDto>({
  id: 0,
  name: '',
  slug: '',
  description: '',
  coverImageId: null,
  coverImageUrl: '',
  images: [],
  posts: [],
  createdAt: '',
  updatedAt: ''
})
const loading = ref(false)
const saving = ref(false)

// 帖子搜索与添加
const postSearchKeyword = ref('')
const searchResults = ref<PostSummaryDto[]>([])
const searchingPosts = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 封面文件选择
const coverFile = ref<File | null>(null)
const coverPreview = ref<string>('')

const fetchCollection = async (slug: string) => {
  loading.value = true
  try {
    const data = await collectionsApi.getBySlug(slug)
    collection.value = data
    coverPreview.value = data.coverImageUrl
    isDraftMode.value = (slug === DRAFT_SLUG)

    if (isDraftMode.value) {
      if (collection.value.name === 'draft collection') collection.value.name = ''
      collection.value.slug = ''
    }
  } catch (error) {
    notify('Failed to load collection', 'error')
    throw error
  } finally {
    loading.value = false
  }
}

const initDraft = async () => {
  await fetchCollection(DRAFT_SLUG)
}

// 搜索帖子
const searchPosts = async () => {
  const kw = postSearchKeyword.value.trim()
  if (!kw) {
    searchResults.value = []
    return
  }
  searchingPosts.value = true
  try {
    const results = await postsApi.search(kw, 20)
    const existingIds = new Set(collection.value.posts.map(p => p.id))
    searchResults.value = results.filter(p => !existingIds.has(p.id))
  } catch (error) {
    notify('Failed to search posts', 'error')
  } finally {
    searchingPosts.value = false
  }
}

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(searchPosts, 400)
}

const addPostToCollection = async (post: PostSummaryDto) => {
  try {
    await collectionsApi.addPost(collection.value.id, post.id)
    collection.value.posts.push(post)
    searchResults.value = searchResults.value.filter(p => p.id !== post.id)
    notify('Post added to collection', 'success')
  } catch (error) {
    notify('Failed to add post', 'error')
  }
}

const removePostFromCollection = async (postId: number) => {
  if (!confirm('Remove this post from the collection?')) return
  try {
    await collectionsApi.removePost(collection.value.id, postId)
    collection.value.posts = collection.value.posts.filter(p => p.id !== postId)
    notify('Post removed', 'success')
  } catch (error) {
    notify('Failed to remove post', 'error')
  }
}

const movePost = (index: number, direction: number) => {
  const posts = [...collection.value.posts]
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= posts.length) return
  const currentItem = posts[index]
  const targetItem = posts[newIndex]
  if (currentItem && targetItem) {
    posts[index] = targetItem
    posts[newIndex] = currentItem
    collection.value.posts = posts
  }
}

const saveOrder = async () => {
  try {
    const orderedIds = collection.value.posts.map(p => p.id)
    await collectionsApi.reorderPosts(collection.value.id, orderedIds)
    notify('Order saved', 'success')
  } catch (error) {
    notify('Failed to save order', 'error')
  }
}

const handleCoverFileChange = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  coverFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    coverPreview.value = ev.target?.result as string
  }
  reader.readAsDataURL(file)
}

const uploadCoverIfNeeded = async () => {
  if (!coverFile.value) return
  try {
    const imageDto = await collectionsApi.uploadCover(collection.value.id, coverFile.value)
    collection.value.coverImageId = imageDto.id
    collection.value.coverImageUrl = imageDto.url
    coverFile.value = null
  } catch (error) {
    notify('Failed to upload cover', 'error')
  }
}

// 保存逻辑 —— 与 Post 完全一致
const save = async () => {
  if (!collection.value.name.trim()) {
    notify('Name is required', 'error')
    return
  }

  let targetSlug = collection.value.slug.trim()
  if (isDraftMode.value) {
    if (!targetSlug || targetSlug === DRAFT_SLUG) {
      targetSlug = DRAFT_SLUG
    }
  } else {
    if (!targetSlug) {
      targetSlug = collection.value.name
          .toLowerCase()
          .replace(/\s+/g, '-')
          .replace(/[^a-z0-9-]/g, '')
    }
  }

  const payload = {
    name: collection.value.name,
    slug: targetSlug,
    description: collection.value.description,
    coverImageId: collection.value.coverImageId
  }

  saving.value = true
  try {
    if (isDraftMode.value) {
      await collectionsApi.update(collection.value.id, payload as PostGroupDto)
      await uploadCoverIfNeeded()

      if (targetSlug !== DRAFT_SLUG) {
        notify('Collection published!', 'success')
        await router.replace({ name: 'admin-collection-edit', params: { slug: targetSlug } })
      } else {
        notify('Draft saved', 'success')
        await fetchCollection(DRAFT_SLUG)
      }
    } else if (isEditing.value) {
      await collectionsApi.update(collection.value.id, payload as PostGroupDto)
      await uploadCoverIfNeeded()
      notify('Collection updated', 'success')
      if (targetSlug !== collection.value.slug) {
        router.replace({ name: 'admin-collection-edit', params: { slug: targetSlug } })
      }
    } else {
      const created = await collectionsApi.create(payload as PostGroupDto)
      collection.value = created
      if (coverFile.value) await uploadCoverIfNeeded()
      router.push({ name: 'admin-collection-edit', params: { slug: created.slug } })
      notify('Collection created', 'success')
    }
  } catch (error: any) {
    const msg = error.response?.data?.message || 'Save failed'
    notify(msg, 'error')
  } finally {
    saving.value = false
  }
}

const discardChanges = async () => {
  if (isDraftMode.value) {
    if (confirm('Delete this draft? All unsaved changes will be lost.')) {
      try {
        await collectionsApi.delete(collection.value.id, false)
        router.push('/admin/collections')
      } catch (e) {
        notify('Failed to delete draft', 'error')
      }
    }
  } else {
    router.back()
  }
}

onMounted(() => {
  if (isEditing.value) {
    fetchCollection(route.params.slug as string)
  } else {
    initDraft()
  }
})
</script>

<template>
  <div class="space-y-8 max-w-7xl mx-auto">
    <!-- Header -->
    <div class="flex justify-between items-end border-b-2 border-zinc-800 dark:border-zinc-200 pb-4">
      <h2 class="text-3xl font-black tracking-tighter uppercase">
        {{ isEditing ? 'EDIT COLLECTION' : 'NEW COLLECTION' }}
      </h2>
      <div class="flex gap-3">
        <button
            @click="discardChanges"
            class="px-4 py-2 border-2 border-zinc-300 dark:border-zinc-700 hover:bg-zinc-100 dark:hover:bg-zinc-800 text-sm font-bold uppercase tracking-widest transition-colors"
        >
          Cancel
        </button>
        <button @click="save" :disabled="saving"
                class="px-6 py-2 bg-zinc-900 text-white dark:bg-white dark:text-zinc-900 text-sm font-bold uppercase tracking-widest hover:opacity-90 disabled:opacity-50 transition-opacity">
          {{ saving ? 'SAVING...' : 'SAVE' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="font-mono text-zinc-500 animate-pulse">Loading data...</div>

    <div v-else class="grid grid-cols-1 xl:grid-cols-3 gap-12">

      <!-- 左侧：基本信息 -->
      <div class="xl:col-span-2 space-y-8">
        <!-- 表单区域优化：增加块状感 -->
        <div class="space-y-6 bg-zinc-50 dark:bg-zinc-900/50 p-6 border border-zinc-200 dark:border-zinc-800">
          <div>
            <label class="block font-mono text-xs uppercase tracking-widest text-zinc-500 mb-2">Name</label>
            <input
                v-model="collection.name"
                type="text"
                class="w-full bg-white dark:bg-zinc-950 border border-zinc-300 dark:border-zinc-700 px-4 py-3 outline-none focus:border-zinc-900 dark:focus:border-zinc-100 text-xl font-bold transition-colors shadow-sm"
                placeholder="Collection name"
            />
          </div>
          <div>
            <label class="block font-mono text-xs uppercase tracking-widest text-zinc-500 mb-2">Slug</label>
            <input
                v-model="collection.slug"
                type="text"
                class="w-full bg-white dark:bg-zinc-950 border border-zinc-300 dark:border-zinc-700 px-4 py-3 outline-none focus:border-zinc-900 dark:focus:border-zinc-100 font-mono text-sm transition-colors shadow-sm"
                placeholder="collection-slug"
            />
          </div>
          <div>
            <label class="block font-mono text-xs uppercase tracking-widest text-zinc-500 mb-2">Description</label>
            <textarea
                v-model="collection.description"
                rows="4"
                class="w-full bg-white dark:bg-zinc-950 border border-zinc-300 dark:border-zinc-700 px-4 py-3 outline-none focus:border-zinc-900 dark:focus:border-zinc-100 font-serif transition-colors shadow-sm"
                placeholder="Optional description"
            ></textarea>
          </div>

          <!-- 封面图上传：更清晰的布局 -->
          <div>
            <label class="block font-mono text-xs uppercase tracking-widest text-zinc-500 mb-3">Cover Image</label>
            <div class="flex items-center gap-6">
              <div
                  class="w-40 h-24 bg-zinc-200 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700 flex items-center justify-center overflow-hidden relative group">
                <img
                    v-if="coverPreview"
                    :src="coverPreview"
                    alt="Cover preview"
                    class="w-full h-full object-cover"
                />
                <span v-else class="font-mono text-xs text-zinc-400">NO_COVER</span>
              </div>
              <div class="flex flex-col gap-2">
                <input
                    type="file"
                    accept="image/*"
                    @change="handleCoverFileChange"
                    class="block w-full text-sm text-zinc-500 file:mr-4 file:py-2 file:px-4 file:border-0 file:text-sm file:font-bold file:uppercase file:tracking-widest file:bg-zinc-200 file:text-zinc-700 hover:file:bg-zinc-300 dark:file:bg-zinc-800 dark:file:text-zinc-300 transition-colors cursor-pointer"
                />
                <p class="font-mono text-xs text-zinc-400">REC_SIZE: 1200×630px</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 已添加帖子管理：列表项改为代码行风格 -->
        <div>
          <div class="flex justify-between items-end mb-4">
            <label class="block font-mono text-xs uppercase tracking-widest text-zinc-500">
              Posts inside ({{ collection.posts.length }})
            </label>
            <button
                v-if="collection.posts.length > 0"
                @click="saveOrder"
                class="font-mono text-xs font-bold uppercase tracking-widest text-blue-600 dark:text-blue-400 hover:underline"
            >
              [ COMMIT_ORDER ]
            </button>
          </div>

          <div v-if="collection.posts.length === 0"
               class="p-8 border border-dashed border-zinc-300 dark:border-zinc-700 text-center">
            <span
                class="font-mono text-sm text-zinc-400">Directory is empty. Search and add posts from the right panel.</span>
          </div>

          <ul v-else class="flex flex-col gap-2">
            <li
                v-for="(post, index) in collection.posts"
                :key="post.id"
                class="flex items-center justify-between border-l-4 border-l-transparent hover:border-l-zinc-900 dark:hover:border-l-zinc-100 bg-zinc-50 dark:bg-zinc-900/50 border border-zinc-200 dark:border-zinc-800 p-3 transition-colors"
            >
              <div class="flex-1 flex items-center gap-3">
                <span class="font-mono text-xs text-zinc-400 w-6">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="font-bold text-sm truncate">{{ post.title }}</span>
                <span
                    class="px-2 py-0.5 bg-zinc-200 dark:bg-zinc-800 font-mono text-[10px] text-zinc-600 dark:text-zinc-300 uppercase">{{
                    post.categoryName
                  }}</span>
              </div>
              <div class="flex items-center gap-3 font-mono text-xs ml-4">
                <button
                    @click="movePost(index, -1)"
                    :disabled="index === 0"
                    class="text-zinc-500 hover:text-zinc-900 dark:hover:text-white disabled:opacity-20 transition-colors"
                >
                  [UP]
                </button>
                <button
                    @click="movePost(index, 1)"
                    :disabled="index === collection.posts.length - 1"
                    class="text-zinc-500 hover:text-zinc-900 dark:hover:text-white disabled:opacity-20 transition-colors"
                >
                  [DN]
                </button>
                <button @click="removePostFromCollection(post.id)"
                        class="text-red-500 hover:text-red-700 transition-colors ml-2">
                  [RM]
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <!-- 右侧：添加帖子搜索（命令面板风格） -->
      <div class="space-y-6">
        <div class="bg-zinc-900 text-white dark:bg-zinc-50 dark:text-zinc-900 p-1">
          <div class="border border-zinc-700 dark:border-zinc-300 p-5">
            <h4 class="font-mono text-xs uppercase tracking-widest text-zinc-400 dark:text-zinc-500 mb-4 flex items-center gap-2">
              <span class="w-2 h-2 bg-green-500 inline-block animate-pulse"></span>
              Search Engine
            </h4>

            <div class="relative mb-4">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 font-mono text-zinc-500">></span>
              <input
                  v-model="postSearchKeyword"
                  type="text"
                  placeholder="Query posts..."
                  class="w-full bg-zinc-800 dark:bg-white border border-zinc-700 dark:border-zinc-300 pl-8 pr-3 py-2 outline-none focus:border-zinc-500 dark:focus:border-zinc-400 font-mono text-sm text-white dark:text-black placeholder-zinc-500"
                  @input="onSearchInput"
                  @keyup.enter="searchPosts"
              />
            </div>

            <div class="h-80 overflow-y-auto pr-1 custom-scrollbar">
              <div v-if="searchingPosts" class="font-mono text-xs text-zinc-500 mt-4">Executing query...</div>

              <div v-else-if="searchResults.length > 0" class="space-y-2">
                <div
                    v-for="post in searchResults"
                    :key="post.id"
                    class="flex flex-col gap-2 p-3 bg-zinc-800 dark:bg-white border border-zinc-700 dark:border-zinc-300 hover:border-zinc-500 transition-colors"
                >
                  <div>
                    <div class="font-bold text-sm leading-tight mb-1">{{ post.title }}</div>
                    <div class="font-mono text-[10px] text-zinc-400 uppercase">{{ post.categoryName }}</div>
                  </div>
                  <button
                      @click="addPostToCollection(post)"
                      class="self-start px-3 py-1 font-mono text-[10px] uppercase font-bold border border-zinc-600 dark:border-zinc-400 hover:bg-white hover:text-black dark:hover:bg-black dark:hover:text-white transition-colors"
                  >
                    + APPEND
                  </button>
                </div>
              </div>

              <div v-else-if="postSearchKeyword.trim() && !searchingPosts" class="font-mono text-xs text-zinc-500 mt-4">
                0 results returned.
              </div>
            </div>
          </div>
        </div>

        <div v-if="collection.id && collection.slug">
          <router-link
              :to="`/collection/${collection.slug}`"
              target="_blank"
              class="block w-full text-center px-4 py-3 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 font-mono text-sm uppercase tracking-widest transition-colors"
          >
            ↗ VIEW_LIVE_ROUTE
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 针对右侧搜索面板的暗黑滚动条样式 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #555;
}

.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #ccc;
}
</style>