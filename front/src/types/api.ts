export interface LoginRequest {
  username?: string;
  password?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: string;
}

export interface ImageDto {
  id: number;
  ownerType: 'POST' | 'PAGE' | 'HOME' | 'SOCIAL' | 'SITE_AVATAR' | 'FAVICON' | 'APPLE_TOUCH_ICON';
  ownerId: number;
  originalFilename: string;
  contentType: string;
  size: number;
  createdAt: string;
  url: string;
}

export interface VideoDto {
  id: number;
  ownerType: 'POST' | 'PAGE' | 'HOME';
  ownerId: number;
  provider: string;
  sourceUrl: string;
  embedUrl: string;
  title: string;
  orderIndex: number;
  createdAt: string;
}

export interface CommentDto {
  id: number;
  name: string;
  content: string;
  createdAt: string;
}

export interface CategoryDto {
  id: number;
  name: string;
  slug: string;
}

export interface PostSummaryDto {
  id: number;
  title: string;
  slug: string;
  excerpt: string;
  createdAt: string;
  likeCount: number;
  dislikeCount: number;
  viewCount: number;
  categoryName: string;
}

export interface PostDetailDto {
  id: number;
  title: string;
  content: string;
  slug: string;
  createdAt: string;
  updateAt: string;
  likeCount: number;
  dislikeCount: number;
  viewCount: number;
  categoryName: string;
  comments: CommentDto[];
  images: ImageDto[];
  videos: VideoDto[];
}

export interface PageDto {
  id: number;
  slug: string;
  title: string;
  content: string;
  parentId: number | null;
  orderIndex: number;
  images: ImageDto[];
  videos: VideoDto[];
}

export interface HomeProfileDto {
  id: number;
  title: string;
  content: string;
  images: ImageDto[];
  videos: VideoDto[];
}

export interface SocialDto {
  id: number;
  name: string;
  url: string;
  description: string;
  iconUrl: string;
  iconImageId: number;
  externalIconUrl: string;
}

export interface LikeResponseDto {
  likes: number;
  dislikes: number;
  message: string;
}

export interface CreateCommentRequest {
  name: string;
  email: string;
  content: string;
}

export interface MovePageRequest {
  parentId: number | null;
  orderIndex: number;
}

export interface PageTreeNodeDto {
  id: number;
  slug: string;
  title: string;
  content: string;
  parentId: number | null;
  orderIndex: number;
  children?: PageTreeNodeDto[];
  hasChildren: boolean;
  depth: number;
}

export interface SidebarDto {
  siteConfig: SiteConfigDto;
  pages: PageTreeNodeDto[];
  categories: CategoryDto[];
  browserIcon: BrowserIconDto;
}

export interface SiteConfigDto {
  id: number;
  siteName: string;
  authorName: string;
  siteAvatarImageId: number | null;
  siteAvatarUrl: string;
  footerText: string;
  metaDescription: string;
  metaKeywords: string;
  copyrightText: string;
  isActive: boolean;
}

export interface BrowserIconDto {
  id: number;
  faviconImageId: number | null;
  faviconUrl: string;
  appleTouchIconImageId: number | null;
  appleTouchIconUrl: string;
  isActive: boolean;
}