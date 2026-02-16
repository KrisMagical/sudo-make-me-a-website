import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') === 'dark');

  function toggleTheme() {
    isDark.value = !isDark.value;
    const theme = isDark.value ? 'dark' : 'light';
    localStorage.setItem('theme', theme);
    applyTheme();
  }

  function applyTheme() {
    if (isDark.value) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  return { isDark, toggleTheme, applyTheme };
});