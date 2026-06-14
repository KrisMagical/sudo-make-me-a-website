import request from '@/utils/request'
import type { CommentDto, CommentSearchResult, CreateCommentRequest } from '@/types/api'

export const commentsApi = {
  delete: (commentId: number) =>
    request.delete<CommentDto>(`/api/comments/admin/${commentId}`),

  updateStatus: (commentId: number, status: 'PENDING' | 'APPROVED' | 'REJECTED') =>
    request.put<CommentDto>(`/api/comments/admin/${commentId}/status`, null, { params: { status } }),

  addAdminComment: (postId: number, data: CreateCommentRequest) =>
    request.post<CommentDto>(`/api/comments/admin/post/${postId}`, data),

  search: (keyword: string) =>
    request.get<CommentSearchResult[]>('/api/comments/search', { params: { q: keyword } })
}
