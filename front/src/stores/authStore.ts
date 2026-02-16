import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { notify } from '@/utils/feedback'; // 引入通知工具

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '');
  const username = ref(localStorage.getItem('username') || '');
  const role = ref(localStorage.getItem('role') || '');

  // 用于存储定时器 ID，以便清除
  let expirationTimer: number | null = null;

  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => role.value === 'ROOT');

  // 辅助函数：解析 JWT 获取 Payload
  function parseJwt(tokenStr: string) {
    try {
      const base64Url = tokenStr.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload);
    } catch (e) {
      return null;
    }
  }

  // 核心逻辑：启动过期检查
  function startExpirationCheck() {
    // 清除旧定时器
    if (expirationTimer) {
      clearTimeout(expirationTimer);
      expirationTimer = null;
    }

    if (!token.value) return;

    const payload = parseJwt(token.value);
    if (!payload || !payload.exp) return;

    // exp 是秒，转换为毫秒
    const expMs = payload.exp * 1000;
    const now = Date.now();

    // 提前 2 分钟 (120000ms) 提醒
    const warningBuffer = 5 * 60 * 1000;

    // 计算距离触发提醒还有多久
    const timeUntilWarning = expMs - now - warningBuffer;

    if (timeUntilWarning > 0) {
      // 如果还没到提醒时间，设置定时器
      expirationTimer = window.setTimeout(() => {
        notify('Your session will expire in 2 minutes. Please save your work.', 'error');
      }, timeUntilWarning);
    } else if (expMs > now) {
      notify('Your session is about to expire soon!', 'error');
    }
  }

  function setAuth(data: { token: string; username: string; role: string }) {
    token.value = data.token;
    username.value = data.username;
    role.value = data.role;
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role);

    // 登录成功后启动检查
    startExpirationCheck();
  }

  function logout() {
    token.value = '';
    username.value = '';
    role.value = '';
    localStorage.clear();

    // 登出时清除定时器
    if (expirationTimer) {
      clearTimeout(expirationTimer);
      expirationTimer = null;
    }
  }

  return {
    token,
    username,
    role,
    isLoggedIn,
    isAdmin,
    setAuth,
    logout,
    startExpirationCheck // 导出此方法供 App.vue 初始化使用
  };
});