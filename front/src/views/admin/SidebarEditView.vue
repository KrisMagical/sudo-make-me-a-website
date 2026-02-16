<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { sidebarApi } from '@/api/sidebar'
import type { SiteConfigDto, BrowserIconDto, ImageDto } from '@/types/api'
import { notify } from '@/utils/feedback'

const loading = ref(false)
const saving = ref(false)

// 网站配置
const siteConfig = ref<SiteConfigDto | null>(null)
// 浏览器图标配置
const browserIcon = ref<BrowserIconDto | null>(null)

// 文件输入引用
const siteAvatarFileInput = ref<HTMLInputElement | null>(null)
const faviconFileInput = ref<HTMLInputElement | null>(null)
const appleTouchIconFileInput = ref<HTMLInputElement | null>(null)

// 网站配置表单
const siteConfigForm = ref({
  siteName: '',
  authorName: '',
  siteAvatarImageId: null as number | null,
  siteAvatarUrl: '',
  footerText: '',
  metaDescription: '',
  metaKeywords: '',
  copyrightText: '',
  isActive: true
})

// 浏览器图标表单
const browserIconForm = ref({
  faviconImageId: null as number | null,
  faviconUrl: '',
  appleTouchIconImageId: null as number | null,
  appleTouchIconUrl: '',
  isActive: true
})

// 预览文件
const siteAvatarPreview = ref<string | null>(null)
const faviconPreview = ref<string | null>(null)
const appleTouchIconPreview = ref<string | null>(null)

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [configData, iconData] = await Promise.all([
      sidebarApi.getSiteConfig(),
      sidebarApi.getBrowserIcon()
    ])

    siteConfig.value = configData
    browserIcon.value = iconData

    // 初始化表单
    if (configData) {
      Object.assign(siteConfigForm.value, configData)
      siteAvatarPreview.value = configData.siteAvatarUrl || null
    }
    if (iconData) {
      Object.assign(browserIconForm.value, iconData)
      faviconPreview.value = iconData.faviconUrl || null
      appleTouchIconPreview.value = iconData.appleTouchIconUrl || null
    }
  } catch (error) {
    notify('Failed to load sidebar data', 'error')
  } finally {
    loading.value = false
  }
}

// 处理网站头像文件选择
const handleSiteAvatarFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 预览
  const reader = new FileReader()
  reader.onload = (e) => {
    siteAvatarPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)

  // 上传文件
  uploadSiteAvatar(file)
}

// 处理favicon文件选择
const handleFaviconFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 预览
  const reader = new FileReader()
  reader.onload = (e) => {
    faviconPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)

  // 上传文件
  uploadFavicon(file)
}

// 处理apple touch icon文件选择
const handleAppleTouchIconFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 预览
  const reader = new FileReader()
  reader.onload = (e) => {
    appleTouchIconPreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)

  // 上传文件
  uploadAppleTouchIcon(file)
}

// 上传网站头像
const uploadSiteAvatar = async (file: File) => {
  saving.value = true
  try {
    const imageDto = await sidebarApi.uploadSiteAvatar(file)
    siteConfigForm.value.siteAvatarImageId = imageDto.id
    await updateSiteConfig()
    notify('Site avatar uploaded successfully', 'success')
  } catch (error) {
    notify('Failed to upload site avatar', 'error')
    if (siteConfig.value?.siteAvatarUrl) {
      siteAvatarPreview.value = siteConfig.value.siteAvatarUrl
    }
  } finally {
    saving.value = false
  }
}

// 上传favicon
const uploadFavicon = async (file: File) => {
  saving.value = true
  try {
    const imageDto = await sidebarApi.uploadFavicon(file)
    browserIconForm.value.faviconImageId = imageDto.id
    await updateBrowserIcon()
    notify('Favicon uploaded successfully', 'success')
  } catch (error) {
    notify('Failed to upload favicon', 'error')
  } finally {
    saving.value = false
  }
}

// 上传apple touch icon
const uploadAppleTouchIcon = async (file: File) => {
  saving.value = true
  try {
    const imageDto = await sidebarApi.uploadAppleTouchIcon(file)
    browserIconForm.value.appleTouchIconImageId = imageDto.id
    await updateBrowserIcon()
    notify('Apple touch icon uploaded successfully', 'success')
  } catch (error) {
    notify('Failed to upload apple touch icon', 'error')
  } finally {
    saving.value = false
  }
}

// 更新网站配置
const updateSiteConfig = async () => {
  saving.value = true
  try {
    const updated = await sidebarApi.updateSiteConfig(siteConfigForm.value)
    siteConfig.value = updated
    if (updated.siteAvatarUrl) {
      siteAvatarPreview.value = updated.siteAvatarUrl
    }
    notify('Site configuration updated successfully', 'success')
  } catch (error) {
    notify('Failed to update site configuration', 'error')
  } finally {
    saving.value = false
  }
}

// 更新浏览器图标
const updateBrowserIcon = async () => {
  saving.value = true
  try {
    const updated = await sidebarApi.updateBrowserIcon(browserIconForm.value)
    browserIcon.value = updated
    if (updated.faviconUrl) faviconPreview.value = updated.faviconUrl
    if (updated.appleTouchIconUrl) appleTouchIconPreview.value = updated.appleTouchIconUrl
    notify('Browser icon updated successfully', 'success')
  } catch (error) {
    notify('Failed to update browser icon', 'error')
  } finally {
    saving.value = false
  }
}

// 删除网站头像
const deleteSiteAvatar = async () => {
  if (!confirm('Delete site avatar?')) return

  siteConfigForm.value.siteAvatarImageId = null
  siteConfigForm.value.siteAvatarUrl = ''
  siteAvatarPreview.value = null

  await updateSiteConfig()
  notify('Site avatar removed', 'success')
}

// 删除favicon
const deleteFavicon = async () => {
  if (!confirm('Delete favicon?')) return

  browserIconForm.value.faviconImageId = null
  browserIconForm.value.faviconUrl = ''
  faviconPreview.value = null

  await updateBrowserIcon()
  notify('Favicon removed', 'success')
}

// 删除apple touch icon
const deleteAppleTouchIcon = async () => {
  if (!confirm('Delete apple touch icon?')) return

  browserIconForm.value.appleTouchIconImageId = null
  browserIconForm.value.appleTouchIconUrl = ''
  appleTouchIconPreview.value = null

  await updateBrowserIcon()
  notify('Apple touch icon removed', 'success')
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-8">
    <div class="border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">SIDEBAR_SETTINGS</h2>
      <p class="text-sm text-zinc-500 mt-1">
        Configure site information and browser icons
      </p>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading sidebar data...</div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 网站配置 -->
      <div class="space-y-6">
        <h3 class="text-lg font-bold border-b border-zinc-200 dark:border-zinc-800 pb-2">
          Site Configuration
        </h3>

        <div class="space-y-4">
          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Site Name
            </label>
            <input
              v-model="siteConfigForm.siteName"
              type="text"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
              placeholder="My Blog"
            />
          </div>

          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Author Name
            </label>
            <input
              v-model="siteConfigForm.authorName"
              type="text"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
              placeholder="John Doe"
            />
          </div>

          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Site Avatar
            </label>
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 rounded-lg overflow-hidden bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center">
                <img
                  v-if="siteAvatarPreview"
                  :src="siteAvatarPreview"
                  alt="Site Avatar"
                  class="w-full h-full object-cover"
                />
                <div v-else class="text-2xl text-zinc-400">
                  {{ siteConfigForm.siteName?.charAt(0) || 'A' }}
                </div>
              </div>
              <div class="flex-1 space-y-2">
                <div class="flex gap-2">
                  <button
                    @click="siteAvatarFileInput?.click()"
                    class="px-3 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800 text-sm"
                  >
                    Upload Avatar
                  </button>
                  <button
                    v-if="siteAvatarPreview"
                    @click="deleteSiteAvatar"
                    class="px-3 py-1 border border-red-400 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm"
                  >
                    Remove
                  </button>
                </div>
                <p class="text-xs text-zinc-500">
                  Avatar image displayed in sidebar
                </p>
              </div>
              <input
                type="file"
                ref="siteAvatarFileInput"
                class="hidden"
                accept="image/*"
                @change="handleSiteAvatarFileChange"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Footer Text
            </label>
            <textarea
              v-model="siteConfigForm.footerText"
              rows="2"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
              placeholder="Additional footer text"
            ></textarea>
          </div>

          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Meta Description
            </label>
            <textarea
              v-model="siteConfigForm.metaDescription"
              rows="2"
              class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
              placeholder="Site description for SEO"
            ></textarea>
          </div>

          <button
            @click="updateSiteConfig"
            :disabled="saving"
            class="w-full px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
          >
            {{ saving ? 'Saving...' : 'Update Site Config' }}
          </button>
        </div>
      </div>

      <!-- 浏览器图标配置 -->
      <div class="space-y-6">
        <h3 class="text-lg font-bold border-b border-zinc-200 dark:border-zinc-800 pb-2">
          Browser Icons
        </h3>

        <div class="space-y-6">
          <!-- Favicon -->
          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Favicon
            </label>
            <div class="flex items-center gap-4">
              <div class="w-12 h-12 rounded overflow-hidden bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center">
                <img
                  v-if="faviconPreview"
                  :src="faviconPreview"
                  alt="Favicon"
                  class="w-full h-full object-contain"
                />
                <div v-else class="text-sm text-zinc-400">F</div>
              </div>
              <div class="flex-1 space-y-2">
                <div class="flex gap-2">
                  <button
                    @click="faviconFileInput?.click()"
                    class="px-3 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800 text-sm"
                  >
                    Upload Favicon
                  </button>
                  <button
                    v-if="faviconPreview"
                    @click="deleteFavicon"
                    class="px-3 py-1 border border-red-400 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm"
                  >
                    Remove
                  </button>
                </div>
                <p class="text-xs text-zinc-500">
                  Typically 16x16 or 32x32 .ico or .png file
                </p>
              </div>
              <input
                type="file"
                ref="faviconFileInput"
                class="hidden"
                accept="image/*"
                @change="handleFaviconFileChange"
              />
            </div>
          </div>

          <!-- Apple Touch Icon -->
          <div>
            <label class="block text-xs uppercase tracking-widest text-zinc-500 mb-2">
              Apple Touch Icon
            </label>
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 rounded-lg overflow-hidden bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center">
                <img
                  v-if="appleTouchIconPreview"
                  :src="appleTouchIconPreview"
                  alt="Apple Touch Icon"
                  class="w-full h-full object-contain"
                />
                <div v-else class="text-sm text-zinc-400">A</div>
              </div>
              <div class="flex-1 space-y-2">
                <div class="flex gap-2">
                  <button
                    @click="appleTouchIconFileInput?.click()"
                    class="px-3 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800 text-sm"
                  >
                    Upload Apple Icon
                  </button>
                  <button
                    v-if="appleTouchIconPreview"
                    @click="deleteAppleTouchIcon"
                    class="px-3 py-1 border border-red-400 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 text-sm"
                  >
                    Remove
                  </button>
                </div>
                <p class="text-xs text-zinc-500">
                  Typically 180x180 .png file for iOS devices
                </p>
              </div>
              <input
                type="file"
                ref="appleTouchIconFileInput"
                class="hidden"
                accept="image/*"
                @change="handleAppleTouchIconFileChange"
              />
            </div>
          </div>

          <div class="flex items-center gap-2">
            <input
              v-model="browserIconForm.isActive"
              type="checkbox"
              id="iconActive"
              class="w-4 h-4"
            />
            <label for="iconActive" class="text-sm text-zinc-500">
              Enable browser icons
            </label>
          </div>

          <button
            @click="updateBrowserIcon"
            :disabled="saving"
            class="w-full px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
          >
            {{ saving ? 'Saving...' : 'Save Browser Icons' }}
          </button>
        </div>

        <!-- 预览区域 -->
        <div v-if="faviconPreview || appleTouchIconPreview"
             class="border border-zinc-200 dark:border-zinc-800 p-4 mt-6">
          <h4 class="text-sm font-bold uppercase tracking-widest mb-3">Preview</h4>
          <div class="flex items-center gap-6">
            <div v-if="faviconPreview" class="text-center">
              <div class="w-8 h-8 mx-auto mb-2 bg-zinc-100 dark:bg-zinc-800 rounded overflow-hidden">
                <img :src="faviconPreview" alt="Favicon" class="w-full h-full object-contain">
              </div>
              <div class="text-xs text-zinc-500">Favicon</div>
            </div>
            <div v-if="appleTouchIconPreview" class="text-center">
              <div class="w-12 h-12 mx-auto mb-2 bg-zinc-100 dark:bg-zinc-800 rounded-lg overflow-hidden">
                <img :src="appleTouchIconPreview" alt="Apple Touch Icon" class="w-full h-full object-contain">
              </div>
              <div class="text-xs text-zinc-500">Apple Touch</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>