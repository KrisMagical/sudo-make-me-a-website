<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/themeStore'
import { useAuthStore } from '@/stores/authStore'
import Sidebar from '@/views/public/Sidebar.vue'
import BrowserIcons from '@/utils/BrowserIcons.vue'
const footerText = import.meta.env.VITE_FOOTER_TEXT || '© 2026 • Built with Vite + Vue'

const route = useRoute()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const isAdminArea = computed(() => route.path.startsWith('/admin'))

onMounted(() => {
  themeStore.applyTheme()
  if (authStore.isLoggedIn) {
    authStore.startExpirationCheck()
  }
})
</script>

<template>
  <div class="min-h-screen bg-white dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 font-sans transition-colors">
    <BrowserIcons />

    <div v-if="!isAdminArea" class="flex">
      <Sidebar />

      <!-- 主内容区域 -->
      <main class="flex-1 min-h-screen">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>

        <!-- 底部 -->
        <footer class="max-w-6xl mx-auto py-12 px-4 border-t border-zinc-100 dark:border-zinc-900">
          <div class="text-center text-xs text-zinc-500 dark:text-zinc-400 font-mono">
            {{ footerText }}
          </div>
        </footer>
      </main>
    </div>

    <!-- 后台页面 -->
    <div v-else>
      <main class="min-h-screen">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

main {
  padding-top: 2rem;
  padding-bottom: 2rem;
}

@media (min-width: 768px) {
  .flex {
    display: flex;
  }

  main {
    padding-left: 1rem;
    padding-right: 1rem;
  }
}

@media (max-width: 767px) {
  main {
    padding-left: 0.5rem;
    padding-right: 0.5rem;
  }
}
</style>