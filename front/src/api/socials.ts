import request from '@/utils/request';
import type { SocialDto } from '@/types/api';

export const socialsApi = {
  list: () => request.get<SocialDto[]>('/api/socials'),

  create: (data: Partial<SocialDto>, iconFile?: File, externalIconUrl?: string) => {
    const formData = new FormData();

    const socialData: any = {};
    if (data.name !== undefined) socialData.name = data.name;
    if (data.url !== undefined) socialData.url = data.url;
    if (data.description !== undefined) socialData.description = data.description;

    formData.append('data', new Blob([JSON.stringify(socialData)], { type: 'application/json' }));

    if (iconFile) {
      formData.append('iconFile', iconFile);
    }

    const params: any = {};
    if (externalIconUrl) {
      params.externalIconUrl = externalIconUrl;
    }

    return request.post<SocialDto>('/api/socials/create', formData, {
      params
    });
  },

  update: (id: number, data: Partial<SocialDto>, iconFile?: File, externalIconUrl?: string) => {
    const formData = new FormData();

    const socialData: any = {};
    if (data.name !== undefined) socialData.name = data.name;
    if (data.url !== undefined) socialData.url = data.url;
    if (data.description !== undefined) socialData.description = data.description;

    formData.append('data', new Blob([JSON.stringify(socialData)], { type: 'application/json' }));

    if (iconFile) {
      formData.append('iconFile', iconFile);
    }

    const params: any = {};
    if (externalIconUrl !== undefined) {
      params.externalIconUrl = externalIconUrl;
    }

    return request.put<SocialDto>(`/api/socials/update/${id}`, formData, {
      params
    });
  },

  delete: (id: number) => request.delete(`/api/socials/delete/${id}`)
};