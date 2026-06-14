<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="min-h-[70vh] flex flex-col items-center justify-center px-4">
    <div class="text-center">
      <h1 class="text-6xl font-bold tracking-tighter mb-4">404</h1>
      <h2 class="text-2xl font-bold mb-6">Page Not Found</h2>
      <p class="text-zinc-600 dark:text-zinc-400 mb-8 max-w-md mx-auto">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <button
          @click="goHome"
          class="px-6 py-3 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors text-sm font-bold uppercase tracking-tighter"
        >
          Go Home
        </button>
        <button
          @click="$router.go(-1)"
          class="px-6 py-3 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors text-sm font-bold uppercase tracking-tighter"
        >
          Go Back
        </button>
      </div>
    </div>
  </div>
</template>