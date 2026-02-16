<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { loginApi } from '@/api/auth';

const router = useRouter();
const authStore = useAuthStore();

const form = ref({ username: '', password: '' });
const loading = ref(false);
const errorMsg = ref('');

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) return;

  loading.value = true;
  errorMsg.value = '';

  try {
    const res = await loginApi(form.value);
    authStore.setAuth(res);
    router.push('/admin/posts');
  } catch (err: any) {
    errorMsg.value = 'Authentication failed. Please check your credentials.';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[80vh]">
    <div class="w-full max-w-sm border border-gray-300 dark:border-gray-700 p-8 shadow-sm bg-white dark:bg-zinc-900">
      <h1 class="text-xl font-bold mb-6 border-b pb-2 tracking-tight">ADMIN_LOGIN</h1>

      <div v-if="errorMsg" class="mb-4 text-red-500 text-sm italic">
        > {{ errorMsg }}
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">Username</label>
          <input
            v-model="form.username"
            type="text"
            class="w-full bg-transparent border border-gray-300 dark:border-gray-700 px-3 py-2 outline-none focus:border-zinc-500"
          />
        </div>
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">Password</label>
          <input
            v-model="form.password"
            type="password"
            class="w-full bg-transparent border border-gray-300 dark:border-gray-700 px-3 py-2 outline-none focus:border-zinc-500"
          />
        </div>
        <button
          type="submit"
          :disabled="loading"
          class="w-full mt-4 btn-primary disabled:opacity-50"
        >
          {{ loading ? 'Authenticating...' : 'Login' }}
        </button>
      </form>
    </div>
    <div class="mt-8 text-xs text-gray-400">
      <router-link to="/" class="hover:underline">← Back to site</router-link>
    </div>
  </div>
</template>