<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Youtube from '@tiptap/extension-youtube'
import { Node } from '@tiptap/core' // Import Node for custom Iframe extension
import { ref, watch, onBeforeUnmount } from 'vue'
import request from '@/utils/request'
import { notify } from '@/utils/feedback'
import { parseVideoUrl } from '@/utils/videoParser' // Import parser

const addMath = () => {
  const formula = prompt('Enter LaTeX formula (e.g. E=mc^2)');
  if (!formula) return;
  // 简单用 confirm 区分行内/行间：确定=行内，取消=行间
  const isInline = confirm('Inline formula? Click OK for inline, Cancel for display block.');
  const delimiter = isInline ? '\\(' : '\\[';
  const closing = isInline ? '\\)' : '\\]';
  const content = delimiter + formula + closing;
  // 在光标位置插入文本
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

// 1. Define a custom Iframe extension for Bilibili/Other embeds
const Iframe = Node.create({
  name: 'iframe',
  group: 'block',
  atom: true, // It's a single unit, not containing text
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
    // Wrap in a div for aspect ratio styling if needed, matching SmartContent style
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
    Iframe, // Add the custom extension
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

// 监听外部内容变化
watch(() => props.modelValue, (val) => {
  if (editor.value && editor.value.getHTML() !== val) {
    editor.value.commands.setContent(val, false)
  }
})

// 图片上传逻辑
const triggerImageUpload = () => fileInput.value?.click()

const handleFileUpload = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    let url = ''
    if (props.ownerType === 'POST') {
      url = `/api/posts/${props.ownerId}/images`
    } else if (props.ownerType === 'PAGE') {
      if (!props.ownerSlug) {
        throw new Error('ownerSlug is required for PAGE type')
      }
      url = `/api/pages/${props.ownerSlug}/images`
    } else {
      url = '/api/home/images'
    }

    const res = await request.post<any, { url: string }>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    editor.value?.chain().focus().setImage({ src: res.url }).run()
    notify('Image uploaded successfully')
  } catch (err) {
    notify('Failed to upload image', 'error')
  }
}

// 2. Unified Video Insertion Logic
const addVideo = () => {
  const input = prompt('Enter Video URL (YouTube, Bilibili, Vimeo) or iframe code:')
  if (!input) return

  // Check if user pasted raw iframe code
  if (input.trim().startsWith('<iframe')) {
    // Basic extraction of src from iframe tag (simple regex)
    const srcMatch = input.match(/src=["'](.*?)["']/)
    if (srcMatch && srcMatch[1]) {
      editor.value?.chain().focus().insertContent({
        type: 'iframe',
        attrs: { src: srcMatch[1] }
      }).run()
    } else {
       // Fallback: try to insert raw HTML if it's complex, though insertContent usually prefers nodes
       editor.value?.chain().focus().insertContent(input).run()
    }
    return
  }

  // Parse URL
  const videoInfo = parseVideoUrl(input)

  if (videoInfo) {
    if (videoInfo.provider === 'youtube') {
      // Use Tiptap's native YouTube extension for best compatibility
      editor.value?.commands.setYoutubeVideo({ src: input })
    } else {
      // Use our custom Iframe node for Bilibili/Vimeo
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
/* Style for the custom iframe node in the editor */
:deep(.video-wrapper) {
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}
</style>