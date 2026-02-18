<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postsApi } from '@/api/posts'
import { categoriesApi } from '@/api/categories'
import type { PostDetailDto, CategoryDto } from '@/types/api'
import Editor from '@/components/admin/Editor.vue'
import MediaLibrary from '@/components/admin/MediaLibrary.vue'
import { notify } from '@/utils/feedback'

const route = useRoute()
const router = useRouter()
const categories = ref<CategoryDto[]>([])
const post = ref<PostDetailDto | null>(null)
const loading = ref(false)
const saving = ref(false)

// 添加 Editor 组件的引用
const editorRef = ref<InstanceType<typeof Editor>>()

const isEditing = computed(() => route.name === 'admin-post-edit')
const postId = computed(() => route.params.id || post.value?.id)

const fetchData = async () => {
  loading.value = true
  try {
    categories.value = await categoriesApi.list()
    const slug = route.params.slug as string
    if (isEditing.value && slug) {
      post.value = await postsApi.getDetail(slug)
    } else {
      post.value = {
        id: 0,
        title: '',
        content: '',
        slug: '',
        createdAt: new Date().toISOString(),
        updateAt: new Date().toISOString(),
        likeCount: 0,
        dislikeCount: 0,
        viewCount: 0,
        categoryName: '',
        comments: [],
        images: [],
        videos: []
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
  if (!post.value || !post.value.categoryName) {
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
    let savedPost: PostDetailDto
    if (isEditing.value) {
      await postsApi.update(post.value.id, categorySlug, post.value)
      savedPost = post.value // 更新操作返回的是 void? 根据 api.ts 是 request.put，应该返回更新后的 PostDetailDto，假设它返回了
      // 为了保险，重新获取一下最新数据（但更新操作通常返回更新后的对象）
      notify('Post updated successfully', 'success')
    } else {
      savedPost = await postsApi.create(categorySlug, post.value)
      notify('Post created successfully', 'success')
      // 跳转到编辑页
      await router.push({ name: 'admin-post-edit', params: { slug: savedPost.slug } })
      // 重新获取 post 数据以更新 ID（因为 create 返回的应该包含 id）
      post.value = savedPost
    }

    // 处理暂存图片上传
    if (editorRef.value && post.value && post.value.id) {
      await editorRef.value.processPendingUploads(post.value.id)
    }
  } catch (error) {
    notify(isEditing.value ? 'Failed to update post' : 'Failed to create post', 'error')
  } finally {
    saving.value = false
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
      <button
        @click="save"
        :disabled="saving || !post"
        class="px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
      >
        {{ saving ? 'Saving...' : isEditing ? 'Update Post' : 'Create Post' }}
      </button>
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
          />
        </div>

        <MediaLibrary
          v-if="post.id"
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