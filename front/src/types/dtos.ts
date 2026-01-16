export interface CommentDto {
    id: number
    name: string
    content: string
    createdAt: string
}

export interface PostDetailDto {
    id?: number
    title?: string
    content?: string
    slug?: string
    createdAt?: string
    updateAt?: string
    likeCount?: number
    dislikeCount?: number
    viewCount?: number
    categoryName?: string
    comments?: CommentDto[]
}

export interface User {
    id?: number
    username?: string
    password?: string
    role?: string
}

export interface LikeResponseDto {
    likes: number
    dislikes: number
    message: string
}

export interface CreateCommentRequest {
    name: string
    email: string
    content: string
}

export interface PostSummaryDto {
    id: number
    title: string
    slug: string
    excerpt?: string
    createdAt: string
    likeCount?: number
    dislikeCount?: number
    viewCount?: number
    categoryName?: string
}

export interface CategoryDto {
    id: number
    name: string
    slug: string
}

export interface PageDto {
    id?: number
    slug: string
    title: string
    content?: string

    parentId?: number | null;
      orderIndex?: number;
}

export interface MovePageRequest {
  parentId?: number | null;
  orderIndex?: number;
}

export interface SocialDto {
  id?: number; // 创建时可能不传
  name: string;
  url: string;
  description?: string;
  iconUrl?: string;
}
// ===== Home =====
export type HomeMediaType = "IMAGE" | "VIDEO";

export interface HomeMediaDto {
  id?: number;
  type: "IMAGE" | "VIDEO";
  url: string;
  caption?: string;
  orderIndex?: number;
}

export interface HomeProfileDto {
  id?: number;
  title?: string;
  content?: string;
  coverImageUrl?: string;
  coverVideoUrl?: string;
  mediaDtoList?: HomeMediaDto[];
}
