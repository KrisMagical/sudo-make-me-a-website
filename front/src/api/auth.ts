import request from '@/utils/request';

// 定义类型 (对应 components/schemas/LoginRequest)
export interface LoginRequest {
  username?: string;
  password?: string;
}

// 定义类型 (对应 components/schemas/LoginResponse)
export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: string;
}

export const loginApi = (data: LoginRequest) => {
  return request.post<any, LoginResponse>('/login', data);
};