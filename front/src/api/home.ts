import request from '@/utils/request'
import type { HomeProfileDto } from '@/types/api'

export const homeApi = {
  // 获取主页信息
  get: () => request.get<HomeProfileDto>('/api/home'),

  // 更新主页
  update: (data: HomeProfileDto) => request.put<HomeProfileDto>('/api/home/update', data)
}