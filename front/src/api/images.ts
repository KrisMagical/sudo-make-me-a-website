import request from '@/utils/request'
import type { ImageDto } from '@/types/api'

export const imagesApi = {
  // 获取指定资源的所有图片
  list: (ownerType: 'POST' | 'PAGE' | 'HOME' | 'SOCIAL' | 'SITE_AVATAR' | 'FAVICON' | 'APPLE_TOUCH_ICON', ownerId: number) =>
    request.get<ImageDto[]>(`/api/images/${ownerType}/${ownerId}`),

  // 删除图片
  delete: (ownerType: string, ownerId: number, imageId: number) =>
    request.delete(`/api/images/${ownerType}/${ownerId}/${imageId}`)
}