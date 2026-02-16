import request from '@/utils/request'
import type { CommentDto, CreateCommentRequest } from '@/types/api'

export const commentsApi = {
  // 删除评论（需要验证邮箱）
  delete: (commentId: number, email: string) =>
    request.delete<CommentDto>(`/api/comments/${commentId}`, { params: { email } })
}