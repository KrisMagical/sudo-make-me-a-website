<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { maintenanceApi } from '@/api/maintenance';
import { useAuthStore } from '@/stores/authStore';
import { useMaintenanceStore } from '@/stores/maintenanceStore';
import { notify } from '@/utils/feedback';

const authStore = useAuthStore();
const maintenanceStore = useMaintenanceStore();

const enabled = ref(false);
const mode = ref<'updating' | 'maintenance'>('maintenance');
const username = ref('');
const password = ref('');
const saving = ref(false);
const loading = ref(true);

const loadConfig = async () => {
  loading.value = true;
  try {
    const config = await maintenanceApi.getStatus();
    enabled.value = config.enabled;
    mode.value = config.mode;
    username.value = authStore.username || '';
  } catch (error) {
    notify('Failed to load maintenance config', 'error');
  } finally {
    loading.value = false;
  }
};

const saveConfig = async () => {
  if (!username.value.trim()) {
    notify('Please enter your username', 'error');
    return;
  }
  if (!password.value.trim()) {
    notify('Please enter your password', 'error');
    return;
  }
  saving.value = true;
  try {
    const updated = await maintenanceApi.updateStatus(enabled.value, mode.value, username.value, password.value);
    await maintenanceStore.fetchStatus();
    notify(`Maintenance mode ${updated.enabled ? 'enabled' : 'disabled'}`, 'success');
    password.value = '';
  } catch (error: any) {
    const msg = error.response?.data?.message || 'Failed to update maintenance mode';
    notify(msg, 'error');
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  loadConfig();
});
</script>

<template>
  <div class="space-y-10 prose-vim"> <!-- 引入 prose-vim 风格 -->
    <div class="flex justify-between items-end border-b-2 border-zinc-900 dark:border-zinc-100 pb-4">
      <h2 class="text-3xl font-black tracking-tighter uppercase">Maintenance / Control</h2>
      <div v-if="enabled" class="text-xs px-2 py-1 bg-red-900/10 text-red-600 border border-red-600 animate-pulse">
        LIVE: RESTRICTED ACCESS
      </div>
    </div>

    <transition name="fade" mode="out-in"> <!-- 使用 style.css 的 fade 动画 -->
      <div v-if="loading" class="font-mono italic opacity-50">
        [SYSTEM] Initializing configuration...
      </div>

      <div v-else class="space-y-8">
        <!-- 状态切换区 -->
        <section class="border border-zinc-200 dark:border-zinc-800 p-8 bg-zinc-50/50 dark:bg-zinc-900/20">
          <div class="flex items-center gap-6">
            <label class="relative flex items-center gap-3 cursor-pointer group">
              <input type="checkbox" v-model="enabled" class="sr-only peer" />
              <div class="w-10 h-5 bg-zinc-300 dark:bg-zinc-700 peer-checked:bg-zinc-900 dark:peer-checked:bg-zinc-100 transition-colors"></div>
              <span class="text-sm font-bold uppercase tracking-widest group-hover:underline">System Maintenance Mode</span>
            </label>
          </div>

          <div class="mt-8 grid grid-cols-1 md:grid-cols-2 gap-8 border-t border-zinc-200 dark:border-zinc-800 pt-8">
            <div class="space-y-4">
              <label class="block text-[10px] uppercase tracking-[0.2em] text-zinc-500 font-bold">Display Mode Selection</label>
              <div class="flex gap-6">
                <label class="flex items-center gap-2 cursor-pointer group">
                  <input type="radio" value="maintenance" v-model="mode" class="accent-zinc-900" />
                  <span class="text-sm font-mono group-hover:text-zinc-600">01. MAINTENANCE</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer group">
                  <input type="radio" value="updating" v-model="mode" class="accent-zinc-900" />
                  <span class="text-sm font-mono group-hover:text-zinc-600">02. UPDATING</span>
                </label>
              </div>
            </div>
            <div class="text-xs text-zinc-400 font-mono leading-relaxed">
              * Mode 01: Standard downtime for DB migration or system fixes.<br>
              * Mode 02: Short-term deployment and asset compilation.
            </div>
          </div>
        </section>

        <!-- 管理员验证区 -->
        <section class="space-y-6 px-2">
          <h3 class="text-xs font-black uppercase tracking-[0.3em] text-zinc-400">Security Verification</h3>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="block text-[10px] uppercase tracking-widest text-zinc-500">Admin Identifier</label>
              <input
                  v-model="username"
                  type="text"
                  class="w-full bg-transparent border-b border-zinc-300 dark:border-zinc-700 py-2 font-mono outline-none focus:border-zinc-900 dark:focus:border-zinc-100 transition-colors"
              />
            </div>
            <div class="space-y-2">
              <label class="block text-[10px] uppercase tracking-widest text-zinc-500">Access Token / PW</label>
              <input
                  v-model="password"
                  type="password"
                  class="w-full bg-transparent border-b border-zinc-300 dark:border-zinc-700 py-2 font-mono outline-none focus:border-zinc-900 dark:focus:border-zinc-100 transition-colors"
              />
            </div>
          </div>
        </section>

        <div class="flex justify-end pt-4">
          <button
              @click="saveConfig"
              :disabled="saving"
              class="group relative px-10 py-3 bg-zinc-900 text-white dark:bg-white dark:text-zinc-900 text-xs font-black uppercase tracking-[0.2em] overflow-hidden hover:invert transition-all disabled:opacity-30"
          >
            <span class="relative z-10">{{ saving ? 'Executing...' : 'Commit Changes' }}</span>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>