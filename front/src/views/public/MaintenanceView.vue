<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { maintenanceApi } from '@/api/maintenance';
import type { MaintenanceConfig } from '@/types/api';

const config = ref<MaintenanceConfig | null>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await maintenanceApi.getStatus();
    config.value = data;
  } catch (error) {
    console.error('Failed to load maintenance status', error);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-950 px-6 font-mono">
    <transition name="fade" appear>
      <div v-if="loading" class="text-center">
        <div class="animate-pulse text-xs tracking-widest text-zinc-400">INIT_SYSTEM_CHECK...</div>
      </div>

      <div v-else-if="config" class="max-w-xl w-full border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-12 relative overflow-hidden shadow-2xl">
        <!-- 装饰性元素：复古边角 -->
        <div class="absolute top-0 left-0 w-2 h-2 border-t border-l border-zinc-400"></div>
        <div class="absolute bottom-0 right-0 w-2 h-2 border-b border-r border-zinc-400"></div>

        <div class="flex flex-col items-center text-center space-y-8">
          <!-- 状态图标 -->
          <div class="p-4 rounded-full bg-zinc-100 dark:bg-zinc-800">
            <div v-if="config.mode === 'updating'" class="i-carbon-connection-signal w-10 h-10 text-zinc-800 dark:text-zinc-200"></div>
            <div v-else class="i-carbon-tools w-10 h-10 text-zinc-800 dark:text-zinc-200"></div>
          </div>

          <div class="space-y-4">
            <h1 class="text-2xl font-black uppercase tracking-[0.3em]">
              System {{ config.mode === 'updating' ? 'Updating' : 'Maintenance' }}
            </h1>
            <div class="h-1 w-12 bg-zinc-900 dark:bg-zinc-100 mx-auto"></div>
          </div>

          <p class="text-sm text-zinc-600 dark:text-zinc-400 leading-relaxed max-w-sm mx-auto">
            {{ config.mode === 'updating'
              ? 'DEPLOYMENT IN PROGRESS: The core services are being updated to a newer version. Normal operations will resume shortly.'
              : 'ROUTINE INSPECTION: We are performing necessary adjustments to ensure system stability. Access is temporarily restricted.' }}
          </p>

          <div class="pt-6 w-full border-t border-dashed border-zinc-200 dark:border-zinc-800">
            <div class="flex justify-between items-center text-[10px] text-zinc-400 uppercase tracking-widest">
              <span>Status: Offline</span>
              <span>Ref: ERR_SYS_04</span>
              <span class="animate-pulse text-zinc-500">Waiting for recovery...</span>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.fade-enter-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from {
  opacity: 0;
}
</style>