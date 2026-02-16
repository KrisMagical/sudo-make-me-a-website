<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { socialsApi } from '@/api/socials'
import type { SocialDto } from '@/types/api'
import { notify } from '@/utils/feedback'
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const socials = ref<SocialDto[]>([])
const loading = ref(false)
const newSocial = ref({
  name: '',
  url: '',
  description: '',
  externalIconUrl: ''
})
const iconFile = ref<File | null>(null)

const fetchSocials = async () => {
  loading.value = true
  try {
    socials.value = await socialsApi.list()
  } catch (error: any) {
    console.error('Failed to fetch social links:', error)
    notify('Failed to fetch social links', 'error')
  } finally {
    loading.value = false
  }
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files[0]) {
    iconFile.value = target.files[0]
    // 选了本地文件，清空外部 URL
    newSocial.value.externalIconUrl = ''
  }
}

const handleExternalUrlChange = () => {
  // 如果用户输入了外部 URL，清除本地文件
  if (newSocial.value.externalIconUrl.trim() !== '') {
    iconFile.value = null
    // 清空文件输入框
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement | null
    if (fileInput) fileInput.value = ''
  }
}

const validateForm = () => {
  if (!newSocial.value.name.trim()) {
    notify('Platform name is required', 'error')
    return false
  }
  if (!newSocial.value.url.trim()) {
    notify('URL is required', 'error')
    return false
  }

  // 验证 URL 格式
  try {
    new URL(newSocial.value.url)
  } catch {
    notify('Please enter a valid URL', 'error')
    return false
  }

  return true
}

const handleCreate = async () => {
  // 首先检查用户是否已登录
  if (!authStore.isLoggedIn) {
    notify('Please login first', 'error')
    router.push('/admin/login')
    return
  }

  if (!validateForm()) return

  // 必须提供图标：本地文件 或 外部 URL
  if (!iconFile.value && !newSocial.value.externalIconUrl.trim()) {
    notify('Please upload an icon or provide an external icon URL', 'error')
    return
  }

  try {
    const socialData = {
      name: newSocial.value.name.trim(),
      url: newSocial.value.url.trim(),
      description: newSocial.value.description.trim()
    }

    await socialsApi.create(
      socialData,
      iconFile.value || undefined,
      newSocial.value.externalIconUrl.trim() || undefined
    )

    notify('Social link created successfully', 'success')

    // 重置表单
    newSocial.value = { name: '', url: '', description: '', externalIconUrl: '' }
    iconFile.value = null
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement | null
    if (fileInput) fileInput.value = ''

    fetchSocials()
  } catch (error: any) {
    console.error('Create social link error:', error)
    if (error.response?.status === 401) {
      notify('Authentication failed. Please login again.', 'error')
      router.push('/admin/login')
    } else if (error.response?.status === 400) {
      notify(error.response.data?.message || 'Invalid request', 'error')
    } else if (error.response?.status === 409) {
      notify('Social link with this name already exists', 'error')
    } else {
      notify('Failed to create social link', 'error')
    }
  }
}

const handleDelete = async (id: number) => {
  if (!authStore.isLoggedIn) {
    notify('Please login first', 'error')
    router.push('/admin/login')
    return
  }

  if (!confirm('Delete this social link?')) return

  try {
    await socialsApi.delete(id)
    notify('Social link deleted successfully', 'success')
    fetchSocials()
  } catch (error: any) {
    console.error('Delete social link error:', error)
    if (error.response?.status === 401) {
      notify('Authentication failed. Please login again.', 'error')
      router.push('/admin/login')
    } else {
      notify('Failed to delete social link', 'error')
    }
  }
}

onMounted(() => {
  fetchSocials()
})
</script>

<template>
  <div class="space-y-8">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">SOCIAL_LINKS</h2>
    </div>

    <!-- 创建表单 -->
    <div class="border border-zinc-200 dark:border-zinc-800 p-6 space-y-6">
      <h3 class="text-sm font-bold uppercase tracking-widest">Add Social Link</h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Platform Name *</label>
          <input
            v-model="newSocial.name"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            placeholder="Twitter, GitHub, etc."
            required
          />
        </div>
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">URL *</label>
          <input
            v-model="newSocial.url"
            type="url"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            placeholder="https://..."
            required
          />
        </div>
        <div>
          <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">Description</label>
          <input
            v-model="newSocial.description"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            placeholder="Optional description"
          />
        </div>
      </div>

      <!-- 图标选择：二选一 -->
      <div class="p-4 border border-dashed border-zinc-300 dark:border-zinc-800">
        <p class="text-xs font-bold mb-3 uppercase">Icon Source (Choose One) *</p>
        <div class="space-y-4">
          <!-- 本地上传 -->
          <div>
            <label class="block text-xs text-zinc-500 mb-1">Upload Icon</label>
            <input
              type="file"
              accept="image/*"
              @change="handleFileChange"
              class="w-full text-xs"
            />
            <div v-if="iconFile" class="mt-1 text-xs text-zinc-500">
              Selected: {{ iconFile.name }}
            </div>
          </div>

          <!-- 分隔线 -->
          <div class="relative">
            <div class="absolute inset-0 flex items-center">
              <span class="w-full border-t border-zinc-100 dark:border-zinc-900"></span>
            </div>
            <div class="relative flex justify-center text-[10px] uppercase">
              <span class="bg-white dark:bg-zinc-950 px-2 text-zinc-400">OR</span>
            </div>
          </div>

          <!-- 外部 URL -->
          <div>
            <label class="block text-xs text-zinc-500 mb-1">External Icon URL</label>
            <input
              v-model="newSocial.externalIconUrl"
              @input="handleExternalUrlChange"
              type="url"
              placeholder="https://example.com/icon.svg"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-1 outline-none focus:border-zinc-500 text-xs"
            />
          </div>
        </div>
      </div>

      <div class="flex justify-end">
        <button
          @click="handleCreate"
          :disabled="!newSocial.name || !newSocial.url"
          class="btn-primary"
        >
          Add Social Link
        </button>
      </div>
    </div>

    <!-- 社交链接列表 -->
    <div v-if="loading" class="italic opacity-50">Loading social links...</div>

    <div v-else-if="socials.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="social in socials"
        :key="social.id"
        class="border border-zinc-200 dark:border-zinc-800 p-4 hover:border-zinc-300 dark:hover:border-zinc-700 transition-colors"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center space-x-3">
            <div v-if="social.iconUrl" class="w-8 h-8">
              <img :src="social.iconUrl" :alt="social.name" class="w-full h-full object-contain" />
            </div>
            <div>
              <h4 class="font-bold">{{ social.name }}</h4>
              <div class="text-xs text-zinc-500 font-mono truncate max-w-[200px]">
                {{ social.url }}
              </div>
            </div>
          </div>
          <button
            @click="handleDelete(social.id)"
            class="text-red-500 hover:text-red-700 hover:font-bold text-sm"
          >
            ×
          </button>
        </div>

        <p v-if="social.description" class="text-sm text-zinc-600 dark:text-zinc-400 mb-3">
          {{ social.description }}
        </p>

        <a
          :href="social.url"
          target="_blank"
          rel="noopener noreferrer"
          class="text-xs font-bold uppercase tracking-tighter hover:underline"
        >
          Visit →
        </a>
      </div>
    </div>

    <div v-else class="text-center py-12 text-zinc-500">
      No social links configured
    </div>
  </div>
</template>