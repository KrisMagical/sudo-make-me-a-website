import request from '@/utils/request'
import type {
  AdminCommentPageResponse,
  BulkCommentAction,
  BulkCommentResponse,
  CommentDto,
  CommentSearchResult,
  CommentStatsDto,
  CommentStatusFilter,
  CreateCommentRequest
} from '@/types/api'

export interface AdminCommentListParams {
  status?: CommentStatusFilter;
  keyword?: string;
  postId?: number;
  page?: number;
  size?: number;
  sort?: 'createdAt desc' | 'createdAt asc';
}

export const commentsApi = {
  list: (params: AdminCommentListParams) =>
    request.get<AdminCommentPageResponse>('/api/comments/admin', { params }),

  stats: () =>
    request.get<CommentStatsDto>('/api/comments/admin/stats'),

  bulk: (commentIds: number[], action: BulkCommentAction) =>
    request.post<BulkCommentResponse>('/api/comments/admin/bulk', { commentIds, action }),

  delete: (commentId: number) =>
    request.delete<CommentDto>(`/api/comments/admin/${commentId}`),

  updateStatus: (commentId: number, status: 'PENDING' | 'APPROVED' | 'REJECTED') =>
    request.put<CommentDto>(`/api/comments/admin/${commentId}/status`, null, { params: { status } }),

  addAdminComment: (postId: number, data: CreateCommentRequest) =>
    request.post<CommentDto>(`/api/comments/admin/post/${postId}`, data),

  search: (keyword: string) =>
    request.get<CommentSearchResult[]>('/api/comments/search', { params: { q: keyword } })
}
