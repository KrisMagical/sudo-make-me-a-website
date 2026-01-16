import api from "./api";
import type { SocialDto } from "../types/dtos";

type CreateOrUpdateArgs = {
  data: SocialDto;
  iconFile?: File | null;
  externalIconUrl?: string | null;
};

function buildSocialFormData(data: SocialDto, iconFile?: File | null) {
  const fd = new FormData();
  // 关键：把 data 作为 application/json 的 Blob 传递（Spring @RequestPart("data") 最常见写法）
  fd.append(
    "data",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );
  if (iconFile) fd.append("iconFile", iconFile);
  return fd;
}

export async function listSocials(): Promise<SocialDto[]> {
  const res = await api.get<SocialDto[]>("/api/socials");
  return res.data;
}

export async function createSocial(args: CreateOrUpdateArgs): Promise<SocialDto> {
  const { data, iconFile, externalIconUrl } = args;
  const fd = buildSocialFormData(data, iconFile);

  const res = await api.post<SocialDto>("/api/socials/create", fd, {
    params: externalIconUrl ? { externalIconUrl } : undefined,
    // 不要手动写 Content-Type，让 axios 自动带 boundary
  });

  return res.data;
}

export async function updateSocial(
  id: number,
  args: CreateOrUpdateArgs
): Promise<SocialDto> {
  const { data, iconFile, externalIconUrl } = args;
  const fd = buildSocialFormData(data, iconFile);

  const res = await api.put<SocialDto>(`/api/socials/update/${id}`, fd, {
    params: externalIconUrl ? { externalIconUrl } : undefined,
  });

  return res.data;
}

export async function deleteSocial(id: number): Promise<void> {
  await api.delete(`/api/socials/delete/${id}`);
}
