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

  const oldFaviconLinks = document.querySelectorAll("link[rel*='icon']")
  oldFaviconLinks.forEach(link => link.remove())

  const oldAppleLinks = document.querySelectorAll("link[rel*='apple-touch-icon']")
  oldAppleLinks.forEach(link => link.remove())

  if (browserIcon.value.faviconUrl && browserIcon.value.faviconUrl.trim() !== '') {
    const faviconLink = document.createElement('link')
    faviconLink.setAttribute('rel', 'icon')
    faviconLink.setAttribute('href', browserIcon.value.faviconUrl)
    faviconLink.setAttribute('type', 'image/x-icon')
    document.head.appendChild(faviconLink)

    const pngFaviconLink = document.createElement('link')
    pngFaviconLink.setAttribute('rel', 'icon')
    pngFaviconLink.setAttribute('href', browserIcon.value.faviconUrl)
    pngFaviconLink.setAttribute('type', 'image/png')
    document.head.appendChild(pngFaviconLink)
  }

  if (browserIcon.value.appleTouchIconUrl && browserIcon.value.appleTouchIconUrl.trim() !== '') {
    const appleTouchLink = document.createElement('link')
    appleTouchLink.setAttribute('rel', 'apple-touch-icon')
    appleTouchLink.setAttribute('href', browserIcon.value.appleTouchIconUrl)
    document.head.appendChild(appleTouchLink)
  }
}

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