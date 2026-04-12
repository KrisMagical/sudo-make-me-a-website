<!-- src/components/admin/Editor.vue -->
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { MdEditor, NormalToolbar } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import request from '@/utils/request'
import { notify } from '@/utils/feedback'
import { parseVideoUrl } from '@/utils/videoParser'
import type { ImageDto } from '@/types/api'

const props = defineProps<{
  modelValue: string,
  ownerType: 'POST' | 'PAGE' | 'HOME',
  ownerId: string | number,
  ownerSlug?: string
}>()

const emit = defineEmits(['update:modelValue', 'image-uploaded'])

const editorRef = ref<InstanceType<typeof MdEditor>>()
const pendingImages = ref<{ file: File, tempUrl: string }[]>([])
const isUploading = ref(false)
const uploadProgress = ref(0)

// 图片压缩（复用原有逻辑）
const compressImage = (file: File, maxWidth = 1200, maxHeight = 1200, quality = 0.8): Promise<File> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string
      img.onload = () => {
        let width = img.width
        let height = img.height
        if (width > height) {
          if (width > maxWidth) {
            height *= maxWidth / width
            width = maxWidth
          }
        } else {
          if (height > maxHeight) {
            width *= maxHeight / height
            height = maxHeight
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)
        canvas.toBlob((blob) => {
          if (blob) {
            const compressedFile = new File([blob], file.name, { type: file.type })
            resolve(compressedFile)
          } else {
            reject(new Error('Compression failed'))
          }
        }, file.type, quality)
      }
      img.onerror = reject
    }
    reader.onerror = reject
  })
}

// 上传图片到服务器
const uploadImage = async (file: File, realOwnerId: number, realOwnerSlug?: string): Promise<string> => {
  const formData = new FormData()
  formData.append('file', file)

  let url = ''
  if (props.ownerType === 'POST') {
    url = `/api/posts/${realOwnerId}/images`
  } else if (props.ownerType === 'PAGE') {
    if (!realOwnerSlug) throw new Error('ownerSlug required for PAGE')
    url = `/api/pages/${realOwnerSlug}/images`
  } else {
    url = '/api/home/images'
  }

  const res = await request.post<any, ImageDto>(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.url
}

// md-editor-v3 图片上传回调
const onUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  const ownerIdNum = typeof props.ownerId === 'number' ? props.ownerId : parseInt(props.ownerId)
  const isValidId = typeof ownerIdNum === 'number' && ownerIdNum > 0

  const uploadedUrls: string[] = []

  for (const file of files) {
    let processedFile = file
    if (file.size > 2 * 1024 * 1024) {
      try {
        processedFile = await compressImage(file)
        notify('Large image compressed')
      } catch (err) {
        notify('Image compression failed, using original', 'error')
      }
    }

    if (isValidId) {
      try {
        const realUrl = await uploadImage(processedFile, ownerIdNum, props.ownerSlug)
        uploadedUrls.push(realUrl)
        emit('image-uploaded')
        notify('Image uploaded successfully')
      } catch (err) {
        notify('Image upload failed', 'error')
        uploadedUrls.push('')
      }
    } else {
      const tempUrl = URL.createObjectURL(processedFile)
      pendingImages.value.push({ file: processedFile, tempUrl })
      uploadedUrls.push(tempUrl)
      notify('Image will be uploaded after saving')
    }
  }

  callback(uploadedUrls)
}

// 插入视频
const addVideo = () => {
  const input = prompt('Enter video link (YouTube, Bilibili, Vimeo) or iframe code:')
  if (!input) return

  let videoHtml = ''

  if (input.trim().startsWith('<iframe')) {
    videoHtml = input
  } else {
    const videoInfo = parseVideoUrl(input)
    if (videoInfo) {
      videoHtml = `<div class="video-wrapper my-6 rounded-lg overflow-hidden shadow-lg"><iframe src="${videoInfo.embedUrl}" frameborder="0" allowfullscreen class="w-full aspect-video"></iframe></div>`
    } else {
      notify('Unable to parse video link, please insert the link directly', 'error')
      return
    }
  }

  const editor = editorRef.value as any
  if (editor) {
    let cursorPos = -1
    try {
      const selection = editor.getSelection?.()
      cursorPos = selection?.start ?? -1
    } catch (e) {
      cursorPos = -1
    }

    const currentText = props.modelValue
    let newText = ''
    if (cursorPos >= 0 && cursorPos <= currentText.length) {
      newText = currentText.slice(0, cursorPos) + videoHtml + currentText.slice(cursorPos)
    } else {
      newText = currentText + '\n\n' + videoHtml + '\n\n'
    }
    emit('update:modelValue', newText)
  } else {
    emit('update:modelValue', props.modelValue + '\n\n' + videoHtml + '\n\n')
  }
}

// 处理暂存图片的上传与替换
const processPendingUploads = async (realOwnerId: number, realOwnerSlug?: string) => {
  if (pendingImages.value.length === 0) return

  isUploading.value = true
  let completed = 0

  for (const pending of pendingImages.value) {
    try {
      const realUrl = await uploadImage(pending.file, realOwnerId, realOwnerSlug)
      const currentContent = props.modelValue
      const escapedTempUrl = pending.tempUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const regex = new RegExp(escapedTempUrl, 'g')
      const newContent = currentContent.replace(regex, realUrl)
      if (newContent !== currentContent) {
        emit('update:modelValue', newContent)
      }
      URL.revokeObjectURL(pending.tempUrl)
      completed++
      uploadProgress.value = (completed / pendingImages.value.length) * 100
    } catch (err) {
      notify('Some images failed to upload', 'error')
    }
  }

  pendingImages.value = []
  isUploading.value = false
  uploadProgress.value = 0
  emit('image-uploaded')
  notify('All images uploaded')
}

watch(() => props.modelValue, (val) => {
  const editor = editorRef.value as any
  if (editor && editor.getValue && editor.getValue() !== val) {
    editor.setValue(val)
  }
})

defineExpose({
  processPendingUploads
})

onBeforeUnmount(() => {
  pendingImages.value.forEach(p => URL.revokeObjectURL(p.tempUrl))
})
</script>

<template>
  <div class="editor-container">
    <MdEditor
        ref="editorRef"
        :model-value="modelValue"
        @update:model-value="(val) => emit('update:modelValue', val)"
        :toolbars="[
        0,  // 占位符，用于插入自定义工具栏
        'bold', 'underline', 'italic', 'strikeThrough', 'title', 'sub', 'sup',
        'quote', 'unorderedList', 'orderedList', 'task', 'codeRow', 'code',
        'link', 'image', 'table', 'mermaid', 'katex', 'revoke', 'next',
        'save', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview',
        'catalog', 'github'
      ]"
        :toolbars-exclude="['save']"
        :editor-id="'md-editor-' + ownerType + ownerId"
        :preview-theme="'github'"
        :theme="'light'"
        :code-theme="'github'"
        :on-upload-img="onUploadImg"
        class="border border-zinc-300 dark:border-zinc-700 rounded-md overflow-hidden"
    >
      <!-- 自定义工具栏插槽，对应 toolbars 中的 0 占位符 -->
      <template #defToolbars>
        <NormalToolbar title="Insert Video" @onClick="addVideo">
          <template #trigger>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="6" width="20" height="12" rx="2" />
              <path d="m9 10 4 2-4 2v-4z" />
            </svg>
          </template>
        </NormalToolbar>
      </template>
    </MdEditor>

    <!-- 上传进度条 -->
    <div v-if="isUploading" class="mt-2 p-2 border border-zinc-200 dark:border-zinc-800 rounded">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-zinc-600 dark:text-zinc-400">Uploading pending images...</span>
        <span class="text-zinc-500">{{ Math.round(uploadProgress) }}%</span>
      </div>
      <div class="w-full bg-zinc-200 dark:bg-zinc-700 h-1 rounded overflow-hidden">
        <div class="h-full bg-blue-600 transition-all duration-300" :style="{ width: `${uploadProgress}%` }"></div>
      </div>
    </div>

    <!-- 数学公式提示 -->
    <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
      <span class="mr-3">💡 Math formulas: inline $E=mc^2$, block $$\sum_{i=1}^n i^2$$</span>
    </div>
  </div>
</template>

<style scoped>
.md-editor-icon-btn {
  @apply flex items-center justify-center w-8 h-8 rounded-md transition-colors;
  @apply text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800;
}

:deep(.md-editor) {
  --md-color-bg: #ffffff;
  --md-color-bg-secondary: #f9fafb;
  --md-color-border: #e5e7eb;
}

.dark :deep(.md-editor) {
  --md-color-bg: #1f2937;
  --md-color-bg-secondary: #111827;
  --md-color-border: #374151;
  --md-color-text: #f3f4f6;
}

:deep(.md-editor-toolbar) {
  border-bottom: 1px solid var(--md-color-border);
  background-color: var(--md-color-bg-secondary);
}
:deep(.md-editor-preview) {
  background-color: var(--md-color-bg);
  color: var(--md-color-text);
}
:deep(.md-editor-input) {
  background-color: var(--md-color-bg);
  color: var(--md-color-text);
}
:deep(.video-wrapper) {
  background: #000;
  border-radius: 0.5rem;
  overflow: hidden;
  margin: 1rem 0;
}
:deep(.video-wrapper iframe) {
  width: 100%;
  aspect-ratio: 16 / 9;
}
</style>
