import request from '@/utils/request';
import type { MaintenanceConfig } from '@/types/api';

export const maintenanceApi = {
    // 获取当前状态
    getStatus: (): Promise<MaintenanceConfig> =>
        request.get('/api/maintenance/status') as unknown as Promise<MaintenanceConfig>,

    // 更新状态（需要验证 Root 用户名密码）
    updateStatus: (enabled: boolean, mode: string, username: string, password: string): Promise<MaintenanceConfig> =>
        request.put('/api/maintenance/update', { enabled, mode, username, password }) as unknown as Promise<MaintenanceConfig>
};