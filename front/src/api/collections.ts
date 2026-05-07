import request from '@/utils/request'
import type {PostGroupDto, ImageDto, PostSummaryDto, PageResponse} from '@/types/api'

export const collectionsApi = {
    // 列表
    list: (): Promise<PostGroupDto[]> =>
        request.get('/api/collections') as unknown as Promise<PostGroupDto[]>,

    // 根据 slug 获取详情
    getBySlug: (slug: string): Promise<PostGroupDto> =>
        request.get(`/api/collections/${slug}`) as unknown as Promise<PostGroupDto>,

    // 创建
    create: (data: PostGroupDto): Promise<PostGroupDto> =>
        request.post('/api/collections', data) as unknown as Promise<PostGroupDto>,

    // 更新 (使用 id)
    update: (id: number, data: PostGroupDto): Promise<PostGroupDto> =>
        request.put(`/api/collections/${id}`, data) as unknown as Promise<PostGroupDto>,

    // 删除 (deletePosts 参数)
    delete: (id: number, deletePosts = false) =>
        request.delete(`/api/collections/${id}`, {params: {deletePosts}}),

    // 添加帖子到合集
    addPost: (collectionId: number, postId: number, orderIndex?: number) =>
        request.post(`/api/collections/${collectionId}/posts/${postId}`, null, {
            params: {orderIndex},
        }),

    // 从合集移除帖子
    removePost: (collectionId: number, postId: number) =>
        request.delete(`/api/collections/${collectionId}/posts/${postId}`),

    // 重排帖子顺序
    reorderPosts: (collectionId: number, orderedPostIds: number[]) =>
        request.put(`/api/collections/${collectionId}/posts/reorder`, orderedPostIds),

    // 上传封面
    uploadCover: (collectionId: number, file: File): Promise<ImageDto> => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post(`/api/collections/${collectionId}/cover`, formData, {
            headers: {'Content-Type': 'multipart/form-data'},
        }) as unknown as Promise<ImageDto>
    },

    // 搜索合集
    search: (keyword: string): Promise<PostGroupDto[]> =>
        request.get('/api/collections/search', {
            params: {q: keyword},
        }) as unknown as Promise<PostGroupDto[]>,

    searchPostsInCollection: (slug: string, q: string, page = 0, size = 10): Promise<PageResponse<PostSummaryDto>> =>
        request.get(`/api/collections/${slug}/search`, {
            params: {
                q,
                page,
                size
            }
        }) as unknown as Promise<PageResponse<PostSummaryDto>>,

    getPostsByCollection: (slug: string, page = 0, size = 10): Promise<PageResponse<PostSummaryDto>> =>
        request.get(`/api/collections/${slug}/posts`, {
            params: {page, size}
        }) as unknown as Promise<PageResponse<PostSummaryDto>>,
}