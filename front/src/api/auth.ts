import request from '@/utils/request';
import type { AdminMeResponse, LoginRequest, LoginResponse } from '@/types/api';

export const loginApi = (data: LoginRequest) => {
  return request.post<LoginResponse>('/login', data);
};

export const meApi = () => {
  return request.get<AdminMeResponse>('/api/admin/auth/me');
};
