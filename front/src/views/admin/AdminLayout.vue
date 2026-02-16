<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';

const router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();

const navLinks = [
  { name: 'Posts', path: '/admin/posts' },
  { name: 'Pages', path: '/admin/pages' },
  { name: 'Categories', path: '/admin/categories' },
  { name: 'Social', path: '/admin/socials' },
  { name: 'Home', path: '/admin/home' },
  { name: 'Sidebar', path: '/admin/sidebar' },
  { name: 'Comments', path: '/admin/comments' },
];

const logout = () => {
  authStore.logout();
  router.push('/admin/login');
};
</script>

<template>
  <div class="min-h-screen">
    <header class="border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-zinc-950 sticky top-0 z-50">
      <div class="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
        <div class="flex items-center space-x-8">
          <span class="font-bold tracking-tighter text-lg">BLOG_ADMIN</span>
          <nav class="hidden md:flex space-x-6 text-sm">
            <router-link
              v-for="link in navLinks"
              :key="link.path"
              :to="link.path"
              class="hover:text-black dark:hover:text-white transition-colors"
              active-class="underline underline-offset-4 decoration-2"
            >
              {{ link.name }}
            </router-link>
          </nav>
        </div>

        <div class="flex items-center space-x-4 text-sm">
          <button @click="themeStore.toggleTheme" class="p-1 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded">
            <div v-if="themeStore.isDark" i-carbon-sun />
            <div v-else i-carbon-moon />
          </button>
          <span class="text-gray-400">|</span>
          <span>{{ authStore.username }}</span>
          <button @click="logout" class="text-red-500 hover:underline">Logout</button>
        </div>
      </div>
    </header>

    <main class="max-w-6xl mx-auto p-6">
      <router-view />
    </main>
  </div>
</template>