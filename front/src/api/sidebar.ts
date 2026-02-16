import request from '@/utils/request'
import type { SidebarDto, SiteConfigDto, BrowserIconDto, ImageDto } from '@/types/api'

export const sidebarApi = {
  // 获取完整的侧边栏数据
  getSidebarData: () => request.get<SidebarDto>('/api/sidebar'),

  // 获取网站配置
  getSiteConfig: () => request.get<SiteConfigDto>('/api/sidebar/site-config'),

  // 更新网站配置
  updateSiteConfig: (data: SiteConfigDto) =>
    request.put<SiteConfigDto>('/api/sidebar/site-config', data),

  // 获取浏览器图标
  getBrowserIcon: async (): Promise<BrowserIconDto> => {
      try {
        const response = await request.get<BrowserIconDto>('/api/sidebar/browser-icon')
        if (response.faviconUrl && !response.faviconUrl.startsWith('http')) {
          response.faviconUrl = window.location.origin + response.faviconUrl
        }
        if (response.appleTouchIconUrl && !response.appleTouchIconUrl.startsWith('http')) {
          response.appleTouchIconUrl = window.location.origin + response.appleTouchIconUrl
        }
        return response
      } catch (error) {
        console.error('Failed to fetch browser icon:', error)
        return {
          id: 0,
          faviconImageId: null,
          faviconUrl: '/favicon.ico',
          appleTouchIconImageId: null,
          appleTouchIconUrl: '/apple-touch-icon.png',
          isActive: true
        }
      }
    },

  // 更新浏览器图标
  updateBrowserIcon: (data: BrowserIconDto) =>
    request.put<BrowserIconDto>('/api/sidebar/browser-icon', data),

  // 上传网站头像图片
  uploadSiteAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ImageDto>('/api/site-config/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 上传favicon图片
  uploadFavicon: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ImageDto>('/api/browser-icon/favicon', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 上传apple touch icon图片
  uploadAppleTouchIcon: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<ImageDto>('/api/browser-icon/apple-touch-icon', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}