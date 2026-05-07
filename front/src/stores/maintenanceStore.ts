import { defineStore } from 'pinia';
import { ref } from 'vue';
import { maintenanceApi } from '@/api/maintenance';
import type { MaintenanceConfig } from '@/types/api';

export const useMaintenanceStore = defineStore('maintenance', () => {
    const config = ref<MaintenanceConfig | null>(null);

    async function fetchStatus() {
        try {
            const data = await maintenanceApi.getStatus();
            config.value = data;
        } catch (error) {
            console.error('Failed to fetch maintenance status', error);
        }
    }

    return { config, fetchStatus };
});