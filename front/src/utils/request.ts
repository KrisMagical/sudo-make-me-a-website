import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
});

// Request 拦截器
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }

    if (config.data instanceof FormData) {
      if (config.headers) {
         delete config.headers['Content-Type'];
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response 拦截器
service.interceptors.response.use(
  (response) => {
    // 核心逻辑：直接返回 data 数据部分
    // 这与您项目中的 API 代码 (如 sidebar.ts) 预期一致
    return response.data;
  },
  (error) => {
    // 处理 401 未授权
    if (error.response?.status === 401) {
      const authStore = useAuthStore();
      authStore.logout();
      
      // 防止在登录页重复跳转
      const currentPath = window.location.pathname;
      if (!currentPath.includes('/login')) {
        window.location.href = '/login'; 
      }
    }
    return Promise.reject(error);
  }
);

export default service;
