import request from '@/utils/request'
import type { CategoryDto } from '@/types/api'

export const categoriesApi = {
  // 获取所有分类
  list: () => request.get<CategoryDto[]>('/api/categories'),

  // 创建分类
  create: (data: CategoryDto) => request.post<CategoryDto>('/api/categories', data),

  // 更新分类
  update: (name: string, data: CategoryDto) => request.put<CategoryDto>(`/api/categories/${name}`, data),

  // 删除分类
  delete: (name: string) => request.delete(`/api/categories/${name}`)
}