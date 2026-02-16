import request from '@/utils/request'
import type { PageDto, MovePageRequest } from '@/types/api'

export const pagesApi = {
  // 获取所有页面
  list: () => request.get<PageDto[]>('/api/pages'),

  // 获取页面详情
  get: (slug: string) => request.get<PageDto>(`/api/pages/${slug}`),

  // 创建页面
  create: (data: PageDto) => request.post<PageDto>('/api/pages/create', data),

  // 更新页面
  update: (slug: string, data: PageDto) => request.put<PageDto>(`/api/pages/update/${slug}`, data),

  // 删除页面
  delete: (slug: string) => request.delete(`/api/pages/${slug}`),

  // 移动页面
  move: (slug: string, data: MovePageRequest) => request.patch<PageDto>(`/api/pages/${slug}/move`, data),

  // 获取外链
  outlinks: (slug: string) => request.get<PageDto[]>(`/api/pages/${slug}/outlinks`),

  // 获取反链
  backlinks: (slug: string) => request.get<PageDto[]>(`/api/pages/${slug}/backlinks`)
}