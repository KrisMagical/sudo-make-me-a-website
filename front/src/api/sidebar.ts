import request from '@/utils/request'
import type {SiteConfigDto, BrowserIconDto, ImageDto} from '@/types/api'

export const sidebarApi = {
    // 获取网站配置
    getSiteConfig: (): Promise<SiteConfigDto> =>
        request.get('/api/sidebar/site-config') as unknown as Promise<SiteConfigDto>,

    // 更新网站配置
    updateSiteConfig: (data: SiteConfigDto): Promise<SiteConfigDto> =>
        request.put('/api/sidebar/site-config', data) as unknown as Promise<SiteConfigDto>,

    getBrowserIcon: async (): Promise<BrowserIconDto> => {
        try {
            const res = await request.get('/api/sidebar/browser-icon') as unknown as BrowserIconDto
            if (res.faviconUrl && !res.faviconUrl.startsWith('http')) {
                res.faviconUrl = window.location.origin + res.faviconUrl
            }
            if (res.appleTouchIconUrl && !res.appleTouchIconUrl.startsWith('http')) {
                res.appleTouchIconUrl = window.location.origin + res.appleTouchIconUrl
            }
            return res
        } catch (error) {
            console.error('Failed to fetch browser icon:', error)
            return {
                id: 0,
                faviconImageId: null,
                faviconUrl: '',
                appleTouchIconImageId: null,
                appleTouchIconUrl: '',
                isActive: true
            }
        }
    },

    // 更新浏览器图标
    updateBrowserIcon: (data: BrowserIconDto): Promise<BrowserIconDto> =>
        request.put('/api/sidebar/browser-icon', data) as unknown as Promise<BrowserIconDto>,

    // 上传网站头像图片
    uploadSiteAvatar: (file: File): Promise<ImageDto> => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post('/api/site-config/avatar', formData, {
            headers: {'Content-Type': 'multipart/form-data'}
        }) as unknown as Promise<ImageDto>
    },

    // 上传favicon图片
    uploadFavicon: (file: File): Promise<ImageDto> => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post('/api/browser-icon/favicon', formData, {
            headers: {'Content-Type': 'multipart/form-data'}
        }) as unknown as Promise<ImageDto>
    },

    // 上传apple touch icon图片
    uploadAppleTouchIcon: (file: File): Promise<ImageDto> => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post('/api/browser-icon/apple-touch-icon', formData, {
            headers: {'Content-Type': 'multipart/form-data'}
        }) as unknown as Promise<ImageDto>
    }
}