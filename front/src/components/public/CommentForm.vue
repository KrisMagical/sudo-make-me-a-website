<script setup lang="ts">
import { ref } from 'vue'
import { publicApi } from '@/api/public'
import type { CreateCommentRequest } from '@/types/api'

const props = defineProps<{
  postId: number
}>()

const emit = defineEmits(['success'])

const form = ref<CreateCommentRequest>({
  name: '',
  email: '',
  content: ''
})

const loading = ref(false)
const error = ref('')
const success = ref(false)

const submit = async () => {
  if (!form.value.name || !form.value.email || !form.value.content) {
    error.value = 'All fields are required'
    return
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    error.value = 'Invalid email format'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await publicApi.addComment(props.postId, form.value)
    success.value = true
    form.value = { name: '', email: '', content: '' }
    emit('success')
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to submit comment'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="border border-zinc-200 dark:border-zinc-800 p-6">
    <h3 class="text-sm font-bold uppercase tracking-widest mb-4">Post Comment</h3>

    <div v-if="success" class="mb-4 p-3 bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-sm">
      Comment submitted successfully. It will appear after moderation.
    </div>

    <div v-if="error" class="mb-4 p-3 bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
      {{ error }}
    </div>

    <form @submit.prevent="submit" class="space-y-4">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">Name</label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            :disabled="loading"
          />
        </div>
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">Email</label>
          <input
            v-model="form.email"
            type="email"
            required
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            :disabled="loading"
          />
          <p class="text-xs text-zinc-500 mt-1">Your email will not be published.</p>
        </div>
      </div>

      <div>
        <label class="block text-xs uppercase tracking-widest mb-1">Comment</label>
        <textarea
          v-model="form.content"
          required
          rows="4"
          class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
          :disabled="loading"
        ></textarea>
      </div>

      <div class="flex justify-end">
        <button
          type="submit"
          :disabled="loading"
          class="px-6 py-2 text-white text-sm font-bold uppercase tracking-tighter transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          :style="{
            backgroundColor: loading ? undefined : '#18181b',
          }"
          @mouseenter="$event.target.style.backgroundColor = '#27272a'"
          @mouseleave="$event.target.style.backgroundColor = '#18181b'"
        >
          {{ loading ? 'Submitting...' : 'Submit Comment' }}
        </button>
      </div>
    </form>
  </div>
</template>