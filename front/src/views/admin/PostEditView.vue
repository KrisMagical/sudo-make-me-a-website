<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postsApi } from '@/api/posts'
import { categoriesApi } from '@/api/categories'
import type { PostDetailDto, CategoryDto } from '@/types/api'
import Editor from '@/components/admin/Editor.vue'
import MediaLibrary from '@/components/admin/MediaLibrary.vue'
import { notify } from '@/utils/feedback'

const DRAFT_SLUG = '00100000'

const route = useRoute()
const router = useRouter()
const categories = ref<CategoryDto[]>([])
const post = ref<PostDetailDto | null>(null)
const loading = ref(false)
const saving = ref(false)

const editorRef = ref<InstanceType<typeof Editor>>()
const mediaLibraryRef = ref<InstanceType<typeof MediaLibrary> | null>(null)

const onImageUploaded = () => {
  mediaLibraryRef.value?.fetchImages()
}

const isEditing = computed(() => route.name === 'admin-post-edit')

const fetchData = async () => {
  loading.value = true
  try {
    categories.value = await categoriesApi.list()

    // 如果没有分类且试图创建新文章，拦截
    if (!isEditing.value && categories.value.length === 0) {
      notify('Please create a Category first before writing a post', 'warning')
      router.push('/admin/categories')
      return
    }

    const slug = route.params.slug as string
    if (isEditing.value && slug) {
      post.value = await postsApi.getDetail(slug)
    } else {
      let draftPost: PostDetailDto | null = null

      // 尝试获取现存的草稿
      try {
        draftPost = await postsApi.getDetail(DRAFT_SLUG)
      } catch (e) {
        // 如果后端抛出404等异常，说明没有草稿记录，继续走到下一步创建
      }

      if (draftPost && draftPost.id) {
        post.value = draftPost
      } else {
        // 选用任意一个(第一个)分类作为占位
        const defaultCat = categories.value[0]
        const newDraft: any = {
          id: 0,
          title: DRAFT_SLUG,
          content: '',
          slug: DRAFT_SLUG,
          categoryName: defaultCat.name,
          likeCount: 0,
          dislikeCount: 0,
          viewCount: 0,
          comments: [],
          images: [],
          videos: []
        }
        post.value = await postsApi.create(defaultCat.slug, newDraft)
      }

      // 如果是刚创建或恢复的草稿，将展示给用户的标题和slug清空
      if (post.value) {
        if (post.value.title === DRAFT_SLUG) post.value.title = ''
        if (post.value.slug === DRAFT_SLUG) post.value.slug = ''
      }
    }
  } finally {
    loading.value = false
  }
}

const generateSlug = () => {
  if (!post.value?.title) return
  post.value.slug = post.value.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

const save = async () => {
  if (!post.value?.title || !post.value?.slug || post.value.slug === DRAFT_SLUG) {
    notify('Please enter a valid title to generate a slug', 'error')
    return
  }
  if (!post.value.categoryName) {
    notify('Please select a category', 'error')
    return
  }

  const selectedCategory = categories.value.find(c => c.name === post.value?.categoryName)
  const categorySlug = selectedCategory ? selectedCategory.slug : ''

  if (!categorySlug) {
    notify('Invalid category selected', 'error')
    return
  }

  saving.value = true
  try {
    const targetSlug = isEditing.value ? (route.params.slug as string) : DRAFT_SLUG

    // 第一次保存：将草稿转正或更新，获取真实 id
    await postsApi.update(post.value.id, categorySlug, post.value)
    // 注意：post.value 中可能包含临时图片 URL，但第一次保存是为了获取真实 id
    // 真实 id 已在 post.value 中，但为确保最新，可以从返回结果获取，但接口未返回，所以用原对象

    // 等待图片上传完成
    if (editorRef.value && post.value.id) {
      await editorRef.value.processPendingUploads(post.value.id)
    }

    // 第二次保存：将包含真实图片 URL 的内容提交到服务器
    const savedPost = await postsApi.update(post.value.id, categorySlug, post.value)
    post.value = savedPost

    notify(isEditing.value ? 'Post updated successfully' : 'Post created successfully', 'success')

    if (!isEditing.value) {
      await router.push({ name: 'admin-post-edit', params: { slug: savedPost.slug } })
    }
  } catch (error: any) {
    let message = isEditing.value ? 'Failed to update post' : 'Failed to create post'
    if (error.response?.data?.error) message = error.response.data.error
    else if (error.response?.data?.message) message = error.response.data.message
    else if (error.message) message = error.message
    notify(message, 'error')
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
        await postsApi.delete(DRAFT_SLUG)
        router.push('/admin/posts')
      } catch (e) {
        notify('Failed to delete draft post', 'error')
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
        {{ isEditing ? 'EDIT_POST' : 'NEW_POST' }}
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
          :disabled="saving || !post"
          class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
        >
          {{ saving ? 'Saving...' : isEditing ? 'Update Post' : 'Create Post' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading...</div>

    <div v-else-if="post" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div class="lg:col-span-2 space-y-6">
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Title</label>
          <input
            v-model="post.title"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500 text-xl font-bold"
            placeholder="Post Title"
            @blur="generateSlug"
          />
        </div>

        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Slug</label>
          <input
            v-model="post.slug"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500 font-mono"
            placeholder="post-slug"
          />
        </div>

        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Content</label>
          <Editor
            ref="editorRef"
            v-model="post.content"
            owner-type="POST"
            :owner-id="post.id || 'new'"
            @image-uploaded="onImageUploaded"
          />
        </div>

        <MediaLibrary
          v-if="post.id"
          ref="mediaLibraryRef"
          owner-type="POST"
          :owner-id="post.id"
        />
      </div>

      <div class="space-y-6">
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Category</label>
          <select
            v-model="post.categoryName"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
          >
            <option value="" disabled>Select a category</option>
            <option
              v-for="category in categories"
              :key="category.id"
              :value="category.name"
            >
              {{ category.name }}
            </option>
          </select>
        </div>

        <div v-if="isEditing" class="border border-zinc-200 dark:border-zinc-800 p-4">
          <h4 class="text-sm font-bold uppercase tracking-widest mb-3">Statistics</h4>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-zinc-500">Likes</span>
              <span>{{ post.likeCount }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-zinc-500">Dislikes</span>
              <span>{{ post.dislikeCount }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-zinc-500">Views</span>
              <span>{{ post.viewCount }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-zinc-500">Created</span>
              <span class="font-mono">{{ new Date(post.createdAt).toLocaleDateString() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>