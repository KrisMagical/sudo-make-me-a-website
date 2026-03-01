<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Youtube from '@tiptap/extension-youtube'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'

import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { Markdown } from 'tiptap-markdown'
// Math formula support
import { Node } from '@tiptap/core'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { h } from 'vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'

import { ref, watch, onBeforeUnmount } from 'vue'
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

const emit = defineEmits(['update:modelValue'])
const fileInput = ref<HTMLInputElement | null>(null)

// Pending image queue
const pendingImages = ref<{ file: File, tempUrl: string }[]>([])

// Custom math inline node
const MathNode = Node.create({
  name: 'mathInline',
  group: 'inline',
  inline: true,
  atom: true,

  addAttributes() {
    return {
      formula: {
        default: '',
        parseHTML: element => element.getAttribute('data-formula'),
        renderHTML: attributes => ({
          'data-formula': attributes.formula,
          class: 'math-inline'
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span.math-inline',
        getAttrs: element => ({
          formula: element.getAttribute('data-formula')
        })
      }
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', { class: 'math-inline', 'data-formula': HTMLAttributes.formula }, HTMLAttributes.formula]
  },

  addNodeView() {
    return ({ node }) => {
      const dom = document.createElement('span')
      dom.className = 'math-inline-render'

      try {
        katex.render(node.attrs.formula, dom, {
          throwOnError: false,
          displayMode: false
        })
      } catch (error) {
        dom.textContent = `$${node.attrs.formula}$`
        dom.className = 'math-inline-error'
      }

      return {
        dom,
      }
    }
  },
})

// Custom block math node
const MathBlockNode = Node.create({
  name: 'mathBlock',
  group: 'block',
  atom: true,

  addAttributes() {
    return {
      formula: {
        default: '',
        parseHTML: element => element.getAttribute('data-formula'),
        renderHTML: attributes => ({
          'data-formula': attributes.formula,
          class: 'math-block'
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div.math-block',
        getAttrs: element => ({
          formula: element.getAttribute('data-formula')
        })
      }
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', { class: 'math-block', 'data-formula': HTMLAttributes.formula }, HTMLAttributes.formula]
  },

  addNodeView() {
    return ({ node }) => {
      const dom = document.createElement('div')
      dom.className = 'math-block-render my-4 p-2 bg-gray-50 dark:bg-gray-800 rounded'

      try {
        katex.render(node.attrs.formula, dom, {
          throwOnError: false,
          displayMode: true
        })
      } catch (error) {
        dom.textContent = `$$\n${node.attrs.formula}\n$$`
        dom.className = 'math-block-error'
      }

      return {
        dom,
      }
    }
  },
})

// Custom Iframe extension
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

// --- Image compression logic ---
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

// --- Editor initialization ---
const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      // Configure StarterKit for better Markdown support
      heading: {
        levels: [1, 2, 3, 4, 5, 6],
      },
      code: {
        HTMLAttributes: {
          class: 'bg-gray-100 dark:bg-gray-800 rounded px-1 py-0.5 font-mono text-sm',
        },
      },
      codeBlock: {
        HTMLAttributes: {
          class: 'bg-gray-100 dark:bg-gray-800 rounded p-4 font-mono text-sm overflow-x-auto',
        },
      },
    }),
    Image.configure({
      inline: true,
      HTMLAttributes: {
        class: 'max-w-full h-auto border border-zinc-200 rounded my-4'
      }
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        class: 'text-blue-600 hover:underline cursor-pointer',
      },
    }),
    Youtube.configure({
      width: 640,
      height: 360,
      HTMLAttributes: {
        class: 'my-4 rounded',
      },
    }),
    Iframe,
    TaskList.configure({
      HTMLAttributes: {
        class: 'task-list',
      },
    }),
    TaskItem.configure({
      nested: true,
      HTMLAttributes: {
        class: 'task-list-item',
      },
    }),
    Table.configure({
      resizable: true,
      HTMLAttributes: {
        class: 'border-collapse table-auto w-full my-4',
      },
    }),
    TableRow,
    TableHeader.configure({
      HTMLAttributes: {
        class: 'border border-gray-300 bg-gray-100 dark:bg-gray-800 px-4 py-2 text-left font-bold',
      },
    }),
    TableCell.configure({
      HTMLAttributes: {
        class: 'border border-gray-300 px-4 py-2',
      },
    }),
    // Math nodes
    MathNode,
    MathBlockNode,
    Markdown.configure({
      html: true, // Allow HTML
      transformPastedText: true, // Transform pasted Markdown
      transformCopiedText: true, // Transform copied Markdown
    }),
  ],
  editorProps: {
    attributes: {
      class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl focus:outline-none min-h-[500px] p-6 border border-zinc-300 dark:border-zinc-700 rounded-b-md bg-white dark:bg-gray-900'
    },
    handlePaste: (view, event) => {
      // Handle pasted text, automatically detect math formulas
      const text = event.clipboardData?.getData('text/plain')
      if (text) {
        // Detect inline formulas $...$
        const inlineMathRegex = /\$(.+?)\$/g
        // Detect block formulas $$...$$
        const blockMathRegex = /\$\$(.+?)\$\$/gs

        if (blockMathRegex.test(text) || inlineMathRegex.test(text)) {
          // Prevent default paste behavior, let Markdown plugin handle it
          return false
        }
      }
      return false
    },
  },
  onUpdate: ({ editor }) => {
    // Export Markdown for backend storage
    const markdownContent = editor.storage.markdown.getMarkdown()
    emit('update:modelValue', markdownContent)
  }
})

// Watch external value changes, sync to editor
watch(() => props.modelValue, (val) => {
  if (editor.value) {
    const currentContent = editor.value.storage.markdown.getMarkdown()
    if (currentContent !== val) {
      editor.value.commands.setContent(val, false)
    }
  }
})

// Trigger image upload
const triggerImageUpload = () => fileInput.value?.click()

// Handle file upload
const handleFileUpload = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  let processedFile = file
  if (file.size > 2 * 1024 * 1024) {
    try {
      processedFile = await compressImage(file)
      notify('Large image compressed')
    } catch (err) {
      notify('Image compression failed, using original', 'error')
    }
  }

  const tempUrl = URL.createObjectURL(processedFile)

  // Insert image
  editor.value?.chain().focus().setImage({ src: tempUrl }).run()

  const isValidId = typeof props.ownerId === 'number' && props.ownerId > 0

  if (isValidId) {
    try {
      const realUrl = await uploadImage(processedFile, tempUrl, props.ownerId as number, props.ownerSlug)
      replaceImageUrl(tempUrl, realUrl)
      notify('Image uploaded successfully')
    } catch (err) {
      notify('Image upload failed', 'error')
    }
  } else {
    pendingImages.value.push({ file: processedFile, tempUrl })
    notify('Image will be uploaded after saving')
  }
}

// Upload image
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

// Replace image URL
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
  URL.revokeObjectURL(oldUrl)
}

// Process pending uploads
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

// Add video
const addVideo = () => {
  const input = prompt('Enter video link (YouTube, Bilibili, Vimeo) or iframe code:')
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
    notify('Unable to parse video link, please insert the link directly', 'error')
  }
}

// Add table
const addTable = () => {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

// Expose methods
defineExpose({
  processPendingUploads
})

onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="editor-container">
    <!-- Toolbar -->
    <div class="flex flex-wrap gap-1 p-2 bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-t-md text-sm">
      <!-- Text formatting -->
      <button @click="editor?.chain().focus().toggleBold().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('bold') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800 font-bold"
        title="Bold">
        Bold
      </button>
      <button @click="editor?.chain().focus().toggleItalic().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('italic') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800 italic"
        title="Italic">
        Italic
      </button>
      <button @click="editor?.chain().focus().toggleStrike().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('strike') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800 line-through"
        title="Strikethrough">
        Strikethrough
      </button>

      <span class="border-r border-zinc-300 dark:border-zinc-600 mx-1"></span>

      <!-- Headings -->
      <button @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('heading', { level: 1 }) }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Heading 1">
        H1
      </button>
      <button @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('heading', { level: 2 }) }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Heading 2">
        H2
      </button>
      <button @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('heading', { level: 3 }) }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Heading 3">
        H3
      </button>

      <span class="border-r border-zinc-300 dark:border-zinc-600 mx-1"></span>

      <!-- Lists -->
      <button @click="editor?.chain().focus().toggleBulletList().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('bulletList') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Bullet List">
        List
      </button>
      <button @click="editor?.chain().focus().toggleOrderedList().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('orderedList') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Ordered List">
        Numbered
      </button>
      <button @click="editor?.chain().focus().toggleTaskList().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('taskList') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Task List">
        Task
      </button>

      <span class="border-r border-zinc-300 dark:border-zinc-600 mx-1"></span>

      <!-- Code -->
      <button @click="editor?.chain().focus().toggleCode().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('code') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800 font-mono"
        title="Inline Code">
        &lt;/&gt;
      </button>
      <button @click="editor?.chain().focus().toggleCodeBlock().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('codeBlock') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Code Block">
        Code block
      </button>

      <span class="border-r border-zinc-300 dark:border-zinc-600 mx-1"></span>

      <!-- Media -->
      <button @click="triggerImageUpload"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Insert Image">
        Image
      </button>
      <button @click="addVideo"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Insert Video">
        Video
      </button>
      <button @click="addTable"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Insert Table">
        Table
      </button>

      <!-- Blockquote and horizontal rule -->
      <button @click="editor?.chain().focus().toggleBlockquote().run()"
        :class="{ 'bg-zinc-300 dark:bg-zinc-700': editor?.isActive('blockquote') }"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Blockquote">
        Quote
      </button>
      <button @click="editor?.chain().focus().setHorizontalRule().run()"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Horizontal Rule">
        —
      </button>

      <!-- Undo/Redo -->
      <span class="border-r border-zinc-300 dark:border-zinc-600 mx-1"></span>
      <button @click="editor?.chain().focus().undo().run()"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Undo">
        ↶
      </button>
      <button @click="editor?.chain().focus().redo().run()"
        class="px-3 py-1.5 border border-zinc-300 dark:border-zinc-600 rounded hover:bg-zinc-200 dark:hover:bg-zinc-800"
        title="Redo">
        ↷
      </button>

      <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleFileUpload" />
    </div>

    <!-- Editor area -->
    <editor-content :editor="editor" class="rounded-b-md" />

    <!-- Math formula quick input hint -->
    <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
      <span class="mr-3">💡 Math formulas: inline formulas use <code class="bg-gray-100 dark:bg-gray-800 px-1 rounded">$E=mc^2$</code>, block formulas use <code class="bg-gray-100 dark:bg-gray-800 px-1 rounded">$$\sum_{i=1}^n i^2$$</code></span>
    </div>
  </div>
</template>

<style scoped>
/* Ensure the editor content looks like the final page */
:deep(.ProseMirror) {
  outline: none;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
}

/* Restore reset styles */
:deep(.ProseMirror h1) {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 1.5rem 0 1rem;
  line-height: 1.2;
}

:deep(.ProseMirror h2) {
  font-size: 2rem;
  font-weight: 600;
  margin: 1.25rem 0 0.75rem;
  line-height: 1.3;
}

:deep(.ProseMirror h3) {
  font-size: 1.75rem;
  font-weight: 600;
  margin: 1rem 0 0.5rem;
  line-height: 1.4;
}

:deep(.ProseMirror p) {
  margin: 0.75rem 0;
  line-height: 1.6;
}

:deep(.ProseMirror ul) {
  list-style-type: disc;
  padding-left: 2rem;
  margin: 0.75rem 0;
}

:deep(.ProseMirror ol) {
  list-style-type: decimal;
  padding-left: 2rem;
  margin: 0.75rem 0;
}

:deep(.ProseMirror blockquote) {
  border-left: 4px solid #e5e7eb;
  padding-left: 1rem;
  margin: 1rem 0;
  font-style: italic;
  color: #4b5563;
}

:deep(.ProseMirror code) {
  background-color: #f3f4f6;
  border-radius: 0.25rem;
  padding: 0.2rem 0.4rem;
  font-family: monospace;
  font-size: 0.9em;
}

:deep(.ProseMirror pre) {
  background-color: #1f2937;
  color: #f9fafb;
  border-radius: 0.5rem;
  padding: 1rem;
  overflow-x: auto;
  margin: 1rem 0;
}

:deep(.ProseMirror pre code) {
  background-color: transparent;
  color: inherit;
  padding: 0;
  font-size: 0.9rem;
}

:deep(.ProseMirror a) {
  color: #2563eb;
  text-decoration: underline;
  cursor: pointer;
}

:deep(.ProseMirror a:hover) {
  color: #1d4ed8;
}

/* Table styles */
:deep(.ProseMirror table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1rem 0;
}

:deep(.ProseMirror th) {
  background-color: #f9fafb;
  font-weight: 600;
  border: 1px solid #d1d5db;
  padding: 0.75rem;
}

:deep(.ProseMirror td) {
  border: 1px solid #d1d5db;
  padding: 0.75rem;
}

/* Task list styles */
:deep(.task-list) {
  list-style: none;
  padding-left: 0;
}

:deep(.task-list-item) {
  display: flex;
  align-items: flex-start;
  margin: 0.5rem 0;
}

:deep(.task-list-item label) {
  margin-right: 0.75rem;
  margin-top: 0.25rem;
}

/* Image styles */
:deep(.ProseMirror img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.375rem;
  margin: 1rem 0;
}

/* Video wrapper */
:deep(.video-wrapper) {
  background: #000;
  border-radius: 0.5rem;
  overflow: hidden;
  margin: 1rem 0;
}

/* Math formula styles */
:deep(.math-inline-render) {
  display: inline-block;
  vertical-align: middle;
}

:deep(.math-block-render) {
  overflow-x: auto;
  text-align: center;
}

:deep(.math-inline-error),
:deep(.math-block-error) {
  color: #ef4444;
  background-color: #fee2e2;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-family: monospace;
}

/* Dark mode adaptations */
:deep(.dark .ProseMirror) {
  color: #f3f4f6;
}

:deep(.dark .ProseMirror blockquote) {
  border-left-color: #4b5563;
  color: #9ca3af;
}

:deep(.dark .ProseMirror code) {
  background-color: #374151;
  color: #e5e7eb;
}

:deep(.dark .ProseMirror pre) {
  background-color: #111827;
}

:deep(.dark .ProseMirror th) {
  background-color: #1f2937;
  border-color: #374151;
}

:deep(.dark .ProseMirror td) {
  border-color: #374151;
}

:deep(.dark .math-block-render) {
  background-color: #1f2937;
}

:deep(.dark .math-inline-error),
:deep(.dark .math-block-error) {
  background-color: #7f1d1d;
  color: #fecaca;
}
</style>
