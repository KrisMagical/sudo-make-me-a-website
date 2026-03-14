import request from '@/utils/request';
import type {
 PostDetailDto, PostSummaryDto, PageDto, PageSummaryDto,
   HomeProfileDto, CategoryDto, CommentDto,
   CreateCommentRequest, LikeResponseDto
} from '@/types/api';

export const publicApi = {
  // 内容获取
  getHome: () => request.get<HomeProfileDto>('/api/home'),
  getPost: (slug: string) => request.get<PostDetailDto>(`/api/posts/${slug}`),
  getPage: (slug: string) => request.get<PageDto>(`/api/pages/${slug}`),
  getCategories: () => request.get<CategoryDto[]>('/api/categories'),
  getRecentPosts: (limit = 6) => request.get<PostSummaryDto[]>('/api/posts/recent', { params: { limit } }),
  getRecentPages: (limit = 6) => request.get<PageSummaryDto[]>('/api/pages/recent', { params: { limit } }),
  getPostsByCategory: (slug: string) => request.get<PostSummaryDto[]>(`/api/posts/category/${slug}`),
  searchPosts: (q: string) => request.get<PostSummaryDto[]>('/api/posts/searchPages', { params: { q } }),
  searchPages: (q: string) => request.get<PageSummaryDto[]>('/api/pages/searchPages', { params: { q } }),

  // 交互功能
  likePost: (postId: number, positive: boolean) =>
    request.post<LikeResponseDto>(`/api/posts/${postId}/like`, null, { params: { positive } }),
  getComments: (postId: number) => request.get<CommentDto[]>(`/api/comments/post/${postId}`),
  addComment: (postId: number, data: CreateCommentRequest) =>
    request.post<CommentDto>(`/api/comments/post/${postId}`, data)
};