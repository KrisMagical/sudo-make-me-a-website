// src/services/api.ts
import axios from "axios";
import type {
  CategoryDto,
  CommentDto,
  CreateCommentRequest,
  LikeResponseDto,
  PostDetailDto,
  PostSummaryDto,
  PageDto,
  SocialDto,
  MovePageRequest,
  HomeProfileDto,
  HomeMediaDto,
} from "@/types/dtos";
import { getToken } from "./auth";

export type { PageDto } from "@/types/dtos";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---------- Categories ----------
export async function getAllCategories(): Promise<CategoryDto[]> {
  const { data } = await api.get("/api/categories");
  return data;
}
export async function createCategory(body: CategoryDto): Promise<CategoryDto> {
  const { data } = await api.post("/api/categories", { name: body.name, slug: body.slug });
  return data;
}

export async function updateCategoryByName(originalName: string, body: CategoryDto): Promise<CategoryDto> {
  const { data } = await api.put(`/api/categories/${encodeURIComponent(originalName)}`, {
    name: body.name,
    slug: body.slug,
  });
  return data;
}

export async function deleteCategoryByName(name: string): Promise<string> {
  const { data } = await api.delete(`/api/categories/${encodeURIComponent(name)}`);
  return data as string;
}

// ---------- Posts ----------
export async function getPostsByCategory(slug: string): Promise<PostSummaryDto[]> {
  const { data } = await api.get(`/api/posts/category/${encodeURIComponent(slug)}`);
  return data;
}
export async function getPostDetail(slug: string): Promise<PostDetailDto> {
  const { data } = await api.get(`/api/posts/${encodeURIComponent(slug)}`);
  return data;
}
export async function getLikeAndDislikeCount(slug: string): Promise<LikeResponseDto> {
  const { data } = await api.get(`/api/posts/${encodeURIComponent(slug)}/likes`);
  return data;
}
export async function postLike(postId: number, positive: boolean): Promise<LikeResponseDto> {
  const { data } = await api.post(`/api/posts/${postId}/like`, null, { params: { positive } });
  return data;
}

// ---------- Comments（匿名） ----------
export async function getComments(postId: number): Promise<CommentDto[]> {
  const { data } = await api.get(`/api/comments/post/${postId}`, {
    headers: { Authorization: "" },
  });
  return data;
}
export async function addComment(postId: number, body: CreateCommentRequest): Promise<CommentDto> {
  const { data } = await api.post(`/api/comments/post/${postId}`, body, {
    headers: { Authorization: "" },
  });
  return data;
}
export async function deleteComment(commentId: number, email: string): Promise<CommentDto> {
  const { data } = await api.delete(`/api/comments/${commentId}`, {
    params: { email },
    headers: { Authorization: "" },
  });
  return data;
}

// ---------- Auth ----------
export async function login(username: string, password: string): Promise<string> {
  const { data } = await api.post("/login", { username, password });
  return data;
}

// ---------- Create / Update Posts ----------
export async function createPost(body: Partial<PostDetailDto>, categorySlug: string): Promise<PostDetailDto> {
  const { data } = await api.post(`/api/posts/create`, body, { params: { categorySlug } });
  return data;
}

export async function createPostFromMd(file: File, categorySlug: string, slug: string, title?: string): Promise<PostDetailDto> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.post(`/api/posts/create-md`, fd, { params: { categorySlug, slug, title } });
  return data;
}

export async function updatePost(id: number, body: Partial<PostDetailDto>, categorySlug?: string): Promise<PostDetailDto> {
  const { data } = await api.put(`/api/posts/update/${id}`, body, { params: { categorySlug } });
  return data;
}
export async function updatePostFromMd(id: number, file: File, categorySlug?: string): Promise<PostDetailDto> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.put(`/api/posts/update-md/${id}`, fd, { params: { categorySlug } });
  return data;
}

// ---------- Uploads ----------
export async function uploadImage(file: File): Promise<string> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.post(`/api/posts/upload/image`, fd);
  return data;
}
export async function uploadVideo(file: File): Promise<string> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.post(`/api/posts/upload/video`, fd);
  return data;
}
export async function deletePostBySlug(slug: string): Promise<string> {
  const { data } = await api.delete(`/api/posts/${encodeURIComponent(slug)}`);
  return data as string;
}

// 公共 GET：不带 token，避免某些后端把 GET 也要求鉴权导致 401
const publicGetConfig = { headers: { Authorization: "" } };
// ========== Home ==========

// Home GET：建议也用 publicGetConfig（不带 token），和 pages 一致
export async function getHome(): Promise<HomeProfileDto> {
  const { data } = await api.get("/api/home", publicGetConfig);
  return data;
}

export async function updateHome(body: Partial<HomeProfileDto>): Promise<HomeProfileDto> {
  const { data } = await api.put("/api/home", body);
  return data;
}

// 上传图片：POST /api/home/media/images (multipart/form-data + query params)
export async function uploadHomeImage(file: File, caption?: string, orderIndex?: number): Promise<HomeMediaDto> {
  const fd = new FormData();
  fd.append("file", file);

  const { data } = await api.post("/api/home/media/images", fd, {
    params: { caption, orderIndex },
  });
  return data;
}

// 上传视频：POST /api/home/media/videos
export async function uploadHomeVideo(file: File, caption?: string, orderIndex?: number): Promise<HomeMediaDto> {
  const fd = new FormData();
  fd.append("file", file);

  const { data } = await api.post("/api/home/media/videos", fd, {
    params: { caption, orderIndex },
  });
  return data;
}

// 更新媒体：PATCH /api/home/media/{mediaId}
export async function updateHomeMedia(mediaId: number, body: Partial<HomeMediaDto>): Promise<HomeMediaDto> {
  const { data } = await api.patch(`/api/home/media/${mediaId}`, body);
  return data;
}

// 删除媒体：DELETE /api/home/media/{mediaId}
export async function deleteHomeMedia(mediaId: number): Promise<string> {
  const { data } = await api.delete(`/api/home/media/${mediaId}`);
  return data as string;
}


// ========== Pages ==========


export async function getPageBySlug(slug: string): Promise<PageDto> {
  const { data } = await api.get(`/api/pages/${encodeURIComponent(slug)}`, publicGetConfig);
  return data;
}

export async function getAllPages(): Promise<PageDto[]> {
  const { data } = await api.get("/api/pages", publicGetConfig);
  return data;
}

export async function createPage(body: Partial<PageDto>): Promise<PageDto> {
  const { data } = await api.post(`/api/pages`, body);
  return data;
}

export async function updatePageBySlug(slug: string, body: Partial<PageDto>): Promise<PageDto> {
  const { data } = await api.put(`/api/pages/${encodeURIComponent(slug)}`, body);
  return data;
}

export async function deletePageBySlug(slug: string): Promise<string> {
  const { data } = await api.delete(`/api/pages/${encodeURIComponent(slug)}`);
  return data as string;
}

export async function createPageFromMd(file: File, slug: string, title?: string): Promise<PageDto> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.post(`/api/pages/create-md`, fd, { params: { slug, title } });
  return data;
}

export async function updatePageFromMdBySlug(slug: string, file: File): Promise<PageDto> {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.put(`/api/pages/update-md/${encodeURIComponent(slug)}`, fd);
  return data;
}

export async function getPageBacklinks(slug: string): Promise<PageDto[]> {
  const { data } = await api.get(`/api/pages/${encodeURIComponent(slug)}/backlinks`, publicGetConfig);
  return data;
}

export async function getPageOutlinks(slug: string): Promise<PageDto[]> {
  // 注意：后端必须是 /{slug}/outlinks（不要拼成 {slug}outlinks）
  const { data } = await api.get(`/api/pages/${encodeURIComponent(slug)}/outlinks`, publicGetConfig);
  return data;
}

export async function movePage(slug: string, body: MovePageRequest): Promise<PageDto> {
  const { data } = await api.patch(`/api/pages/${encodeURIComponent(slug)}/move`, body);
  return data;
}

// ========== Socials ==========
function buildSocialFormData(data: SocialDto, iconFile?: File | null) {
  const fd = new FormData();
  fd.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
  if (iconFile) fd.append("iconFile", iconFile);
  return fd;
}

export async function listSocials(): Promise<SocialDto[]> {
  const { data } = await api.get("/api/socials");
  return data;
}

export async function createSocial(args: {
  data: SocialDto;
  iconFile?: File | null;
  externalIconUrl?: string | null;
}): Promise<SocialDto> {
  const fd = buildSocialFormData(args.data, args.iconFile ?? null);
  const { data } = await api.post("/api/socials/create", fd, {
    params: args.externalIconUrl ? { externalIconUrl: args.externalIconUrl } : undefined,
  });
  return data;
}

export async function updateSocial(
  id: number,
  args: { data: SocialDto; iconFile?: File | null; externalIconUrl?: string | null }
): Promise<SocialDto> {
  const fd = buildSocialFormData(args.data, args.iconFile ?? null);
  const { data } = await api.put(`/api/socials/update/${id}`, fd, {
    params: args.externalIconUrl ? { externalIconUrl: args.externalIconUrl } : undefined,
  });
  return data;
}

export async function deleteSocial(id: number): Promise<void> {
  await api.delete(`/api/socials/delete/${id}`);
}
