import request from '@/utils/request';
import type { PostDetailDto, PostSummaryDto } from '@/types/api';

export const postsApi = {
  // 获取最近文章列表
  listRecent: (limit = 10) =>
    request.get<PostSummaryDto[]>('/api/posts/recent', { params: { limit } }),

  // 获取文章详情
  getDetail: (slug: string) =>
    request.get<PostDetailDto>(`/api/posts/${slug}`),

  // 创建文章
  create: (categorySlug: string, data: PostDetailDto) =>
    request.post<PostDetailDto>('/api/posts/create', data, { params: { categorySlug } }),

  // 更新文章
  update: (id: number, categorySlug: string, data: PostDetailDto) =>
    request.put<PostDetailDto>(`/api/posts/update/${id}`, data, { params: { categorySlug } }),

  // 删除文章
  delete: (slug: string) =>
    request.delete(`/api/posts/${slug}`)
};