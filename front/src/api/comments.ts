import request from '@/utils/request'
import type { CommentDto, CreateCommentRequest } from '@/types/api'

export const commentsApi = {
  // 删除评论（需要验证邮箱）
  delete: (commentId: number, email: string) =>
    request.delete<CommentDto>(`/api/comments/${commentId}`, { params: { email } }),

  // 后台添加评论/回复
  addAdminComment: (postId: number, data: CreateCommentRequest) =>
    request.post<CommentDto>(`/api/comments/admin/post/${postId}`, data),

  // 搜索评论（管理员专用）
  search: (keyword: string) =>
    request.get<CommentSearchResult[]>('/api/comments/search', { params: { q: keyword } })
}