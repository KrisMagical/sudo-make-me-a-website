<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { sidebarApi } from '@/api/sidebar'
import type { BrowserIconDto } from '@/types/api'

const browserIcon = ref<BrowserIconDto | null>(null)

const loadBrowserIcons = async () => {
  try {
    browserIcon.value = await sidebarApi.getBrowserIcon()
    updateBrowserIcons()
  } catch (error) {
    console.error('Failed to load browser icons:', error)
  }
}

const updateBrowserIcons = () => {
  if (!browserIcon.value) return

  // 移除旧的favicon链接
  const oldFaviconLinks = document.querySelectorAll("link[rel*='icon']")
  oldFaviconLinks.forEach(link => link.remove())

  const oldAppleLinks = document.querySelectorAll("link[rel*='apple-touch-icon']")
  oldAppleLinks.forEach(link => link.remove())

  // 更新favicon
  if (browserIcon.value.faviconUrl) {
    const faviconLink = document.createElement('link')
    faviconLink.setAttribute('rel', 'icon')
    faviconLink.setAttribute('href', browserIcon.value.faviconUrl)
    faviconLink.setAttribute('type', 'image/x-icon')
    document.head.appendChild(faviconLink)

    // 添加额外的favicon类型
    const pngFaviconLink = document.createElement('link')
    pngFaviconLink.setAttribute('rel', 'icon')
    pngFaviconLink.setAttribute('href', browserIcon.value.faviconUrl)
    pngFaviconLink.setAttribute('type', 'image/png')
    document.head.appendChild(pngFaviconLink)
  }

  // 更新apple touch icon
  if (browserIcon.value.appleTouchIconUrl) {
    const appleTouchLink = document.createElement('link')
    appleTouchLink.setAttribute('rel', 'apple-touch-icon')
    appleTouchLink.setAttribute('href', browserIcon.value.appleTouchIconUrl)
    document.head.appendChild(appleTouchLink)
  }
}

// 监听browserIcon变化
watch(browserIcon, () => {
  updateBrowserIcons()
})

onMounted(() => {
  loadBrowserIcons()
})
</script>

<template>
  <!-- 这个组件不渲染任何内容，只负责更新浏览器图标 -->
</template>