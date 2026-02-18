<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Youtube from '@tiptap/extension-youtube'
import { Node } from '@tiptap/core'
import { ref, watch, onBeforeUnmount } from 'vue'
import request from '@/utils/request'
import { notify } from '@/utils/feedback'
import { parseVideoUrl } from '@/utils/videoParser'
import type { ImageDto } from '@/types/api'

const addMath = () => {
  const formula = prompt('Enter LaTeX formula (e.g. E=mc^2)');
  if (!formula) return;
  const isInline = confirm('Inline formula? Click OK for inline, Cancel for display block.');
  const delimiter = isInline ? '\\(' : '\\[';
  const closing = isInline ? '\\)' : '\\]';
  const content = delimiter + formula + closing;
  editor.value?.chain().focus().insertContent({ type: 'text', text: content }).run();
};

const props = defineProps<{
  modelValue: string,
  ownerType: 'POST' | 'PAGE' | 'HOME',
  ownerId: string | number,
  ownerSlug?: string
}>()

const emit = defineEmits(['update:modelValue'])
const fileInput = ref<HTMLInputElement | null>(null)

// 暂存图片队列：当 ownerId 无效时，先不真正上传，而是生成 blob URL 预览
const pendingImages = ref<{ file: File, tempUrl: string }[]>([])

// 自定义 Iframe 扩展（原有）
const Iframe = Node.create({
  name: 'iframe',
  group: 'block',
  atom: true,
  addOptions() {
    return {
      HTMLAttributes: {
        class: 'w-full h-full border-0',
      },
    }
  },
  addAttributes() {
    return {
      src: { default: null },
      frameborder: { default: '0' },
      allowfullscreen: { default: 'true' },
    }
  },
  parseHTML() {
    return [{ tag: 'iframe' }]
  },
  renderHTML({ HTMLAttributes }) {
    return ['div', { class: 'video-wrapper my-6 relative aspect-video w-full' }, ['iframe', HTMLAttributes]]
  },
})

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Image.configure({ inline: true, HTMLAttributes: { class: 'max-w-full h-auto border border-zinc-200' } }),
    Link.configure({ openOnClick: false }),
    Youtube.configure({ width: 640, height: 360 }),
    Iframe,
  ],
  editorProps: {
    attributes: {
      class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl focus:outline-none min-h-[400px] p-4 border border-zinc-300 dark:border-zinc-700 font-mono'
    }
  },
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  }
})

watch(() => props.modelValue, (val) => {
  if (editor.value && editor.value.getHTML() !== val) {
    editor.value.commands.setContent(val, false)
  }
})

const triggerImageUpload = () => fileInput.value?.click()

// 修改后的图片上传处理
const handleFileUpload = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 生成临时预览 URL
  const tempUrl = URL.createObjectURL(file)

  // 插入编辑器（使用临时 URL）
  editor.value?.chain().focus().setImage({ src: tempUrl }).run()

  // 判断 ownerId 是否为有效数字（已保存的实体）
  const isValidId = typeof props.ownerId === 'number' && props.ownerId > 0

  if (isValidId) {
    // 已有真实 ID，立即上传
    try {
      const realUrl = await uploadImage(file, tempUrl, props.ownerId as number, props.ownerSlug)
      // 替换编辑器中的临时 URL 为真实 URL
      replaceImageUrl(tempUrl, realUrl)
      notify('Image uploaded successfully')
    } catch (err) {
      notify('Failed to upload image', 'error')
    }
  } else {
    // 暂存，等待保存后上传
    pendingImages.value.push({ file, tempUrl })
    notify('Image will be uploaded after saving')
  }
}

// 上传单个图片，返回真实 URL
const uploadImage = async (file: File, tempUrl: string, realOwnerId: number, realOwnerSlug?: string): Promise<string> => {
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

// 替换编辑器中的图片 URL
const replaceImageUrl = (oldUrl: string, newUrl: string) => {
  if (!editor.value) return
  const { state } = editor.value
  const tr = state.tr
  let replaced = false
  state.doc.descendants((node, pos) => {
    if (node.type.name === 'image' && node.attrs.src === oldUrl) {
      tr.setNodeMarkup(pos, undefined, { ...node.attrs, src: newUrl })
      replaced = true
    }
  })
  if (replaced) {
    editor.value.view.dispatch(tr)
  }
  // 释放 blob URL 内存
  URL.revokeObjectURL(oldUrl)
}

// 供父组件调用的方法：处理所有暂存图片
const processPendingUploads = async (realOwnerId: number, realOwnerSlug?: string) => {
  if (pendingImages.value.length === 0) return

  for (const pending of pendingImages.value) {
    try {
      const realUrl = await uploadImage(pending.file, pending.tempUrl, realOwnerId, realOwnerSlug)
      replaceImageUrl(pending.tempUrl, realUrl)
    } catch (err) {
      console.error('Failed to upload pending image:', err)
      notify('Some images failed to upload', 'error')
    }
  }
  pendingImages.value = []
}

defineExpose({
  processPendingUploads
})

const addVideo = () => {
  const input = prompt('Enter Video URL (YouTube, Bilibili, Vimeo) or iframe code:')
  if (!input) return

  if (input.trim().startsWith('<iframe')) {
    const srcMatch = input.match(/src=["'](.*?)["']/)
    if (srcMatch && srcMatch[1]) {
      editor.value?.chain().focus().insertContent({
        type: 'iframe',
        attrs: { src: srcMatch[1] }
      }).run()
    } else {
      editor.value?.chain().focus().insertContent(input).run()
    }
    return
  }

  const videoInfo = parseVideoUrl(input)

  if (videoInfo) {
    if (videoInfo.provider === 'youtube') {
      editor.value?.commands.setYoutubeVideo({ src: input })
    } else {
      editor.value?.chain().focus().insertContent({
        type: 'iframe',
        attrs: { src: videoInfo.embedUrl }
      }).run()
    }
  } else {
    notify('Could not parse video URL. Try inserting the link directly.', 'error')
  }
}

onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="editor-container">
    <div class="flex flex-wrap gap-2 mb-2 p-2 bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 text-xs">
      <button @click="editor?.chain().focus().toggleBold().run()" :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('bold') }" class="px-2 py-1 border border-zinc-400">BOLD</button>
      <button @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()" :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('heading') }" class="px-2 py-1 border border-zinc-400">H2</button>
      <button @click="editor?.chain().focus().toggleBulletList().run()" :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('bulletList') }" class="px-2 py-1 border border-zinc-400">LIST</button>
      <button @click="editor?.chain().focus().toggleCodeBlock().run()" :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('codeBlock') }" class="px-2 py-1 border border-zinc-400">CODE</button>
      <span class="border-r border-zinc-400 mx-1"></span>

      <button @click="triggerImageUpload" class="px-2 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800">ADD_IMAGE</button>
      <button @click="addVideo" class="px-2 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800">ADD_VIDEO</button>
      <button @click="addMath" class="px-2 py-1 border border-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-800">ADD_MATH</button>

      <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleFileUpload" />
    </div>

    <editor-content :editor="editor" />
  </div>
</template>

<style scoped>
:deep(.prose) {
  max-width: none;
}
:deep(.prose img) {
  margin: 1rem 0;
}
:deep(.video-wrapper) {
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}
</style>