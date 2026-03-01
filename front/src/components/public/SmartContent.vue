<script setup lang="ts">
import { computed, ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import { parseVideoUrl } from '@/utils/videoParser'
import renderMathInElement from 'katex/contrib/auto-render'
import 'katex/dist/katex.min.css'

const props = defineProps<{ content: string }>()
const contentRef = ref<HTMLElement>()

// 1. Math formula Tokenizer (highest priority)
// Protect math formulas from being parsed by marked (e.g., _, *, etc.)
const mathExtension = {
  name: 'math',
  level: 'inline',
  start(src: string) {
    return src.indexOf('$')
  },
  tokenizer(src: string) {
    // First match block formulas $$...$$ (multiline)
    const blockRule = /^\$\$([\s\S]+?)\$\$/
    const blockMatch = blockRule.exec(src)
    if (blockMatch) {
      return {
        type: 'math',
        raw: blockMatch[0],
        text: blockMatch[1].trim(),
        displayMode: true
      }
    }

    // Then match inline formulas $...$
    // Use a stricter regex to ensure it's not escaped \$, and does not contain line breaks
    const inlineRule = /^\$([^$\n]+?)\$/
    const inlineMatch = inlineRule.exec(src)
    if (inlineMatch) {
      return {
        type: 'math',
        raw: inlineMatch[0],
        text: inlineMatch[1].trim(),
        displayMode: false
      }
    }

    return undefined
  },
  renderer(token: any) {
    // Return a marked span, KaTeX's auto-render will process it later
    // Use data-formula attribute to store the original formula for debugging
    const className = token.displayMode ? 'math-block' : 'math-inline'
    return `<span class="${className}" data-formula="${token.text.replace(/"/g, '&quot;')}">${token.raw}</span>`
  }
}

// 2. Wiki link extension [[slug]] or [[slug|label]]
const wikiLinkExtension = {
  name: 'wikiLink',
  level: 'inline',
  start(src: string) {
    return src.indexOf('[[')
  },
  tokenizer(src: string) {
    const rule = /^\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/
    const match = rule.exec(src)
    if (match) {
      return {
        type: 'wikiLink',
        raw: match[0],
        slug: match[1].trim(),
        text: match[2] ? match[2].trim() : match[1].trim()
      }
    }
    return undefined
  },
  renderer(token: any) {
    // Generate a route link pointing to the page
    // Note: In v-html, router-link does not work automatically, so use <a> tag
    // Combined with front-end route interception or direct navigation
    return `<a href="/page/${encodeURIComponent(token.slug)}" class="internal-link" data-slug="${token.slug}">${token.text}</a>`
  }
}

// 3. Video extension @[video](url) or ![](video.mp4)
const videoExtension = {
  name: 'video',
  level: 'inline',
  start(src: string) {
    // Match both @[video] and ![] video syntax
    const index1 = src.indexOf('@[')
    const index2 = src.indexOf('![')
    if (index1 === -1) return index2
    if (index2 === -1) return index1
    return Math.min(index1, index2)
  },
  tokenizer(src: string) {
    // Match @[video](url) syntax
    const videoRule = /^@\[video\]\(([^)]+)\)/
    const videoMatch = videoRule.exec(src)
    if (videoMatch) {
      return {
        type: 'video',
        raw: videoMatch[0],
        url: videoMatch[1].trim(),
        title: null
      }
    }

    // Match ![](url) syntax, but detect if it's a video file
    const imageRule = /^!\[([^\]]*)\]\(([^)]+)\)/
    const imageMatch = imageRule.exec(src)
    if (imageMatch) {
      const url = imageMatch[2].trim()
      // Check if it's a video file extension
      const videoExts = ['.mp4', '.webm', '.ogg', '.mov', '.m3u8']
      const isVideo = videoExts.some(ext => url.toLowerCase().endsWith(ext))
      if (isVideo) {
        return {
          type: 'video',
          raw: imageMatch[0],
          url: url,
          title: imageMatch[1].trim() || null
        }
      }
    }
    return undefined
  },
  renderer(token: any) {
    const videoInfo = parseVideoUrl(token.url)

    if (videoInfo) {
      // Platform video (YouTube, Bilibili, etc.)
      return `<div class="video-wrapper my-6 rounded-lg overflow-hidden shadow-lg"><iframe src="${videoInfo.embedUrl}" frameborder="0" allowfullscreen class="w-full aspect-video"></iframe></div>`
    } else if (token.url.match(/\.(mp4|webm|ogg|mov|m3u8)$/i)) {
      // Local video files
      return `
        <div class="video-wrapper my-6 rounded-lg overflow-hidden shadow-lg">
          <video controls class="w-full" ${token.title ? `poster="${token.title}"` : ''}>
            <source src="${token.url}" type="video/${token.url.split('.').pop()}">
            Your browser does not support the video tag.
          </video>
        </div>
      `
    }
    return `<a href="${token.url}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:underline">${token.title || token.url}</a>`
  }
}

// 4. Task list support (handling task lists output by TipTap)
const taskListExtension = {
  name: 'taskList',
  level: 'block',
  start(src: string) {
    return src.match(/^- \[[ x]\] /) ? 0 : -1
  },
  tokenizer(src: string) {
    const rule = /^(?:(- \[[ x]\] .*)(?:\n|$))+/gm
    const match = rule.exec(src)
    if (match) {
      const lines = match[0].split('\n').filter(l => l.trim())
      const items = lines.map(line => {
        const taskMatch = line.match(/^- \[([ x])\] (.*)/)
        return {
          checked: taskMatch?.[1] === 'x',
          text: taskMatch?.[2] || ''
        }
      })
      return {
        type: 'taskList',
        raw: match[0],
        items
      }
    }
    return undefined
  },
  renderer(token: any) {
    const itemsHtml = token.items.map((item: any) => `
      <li class="task-item flex items-start my-2">
        <input type="checkbox" ${item.checked ? 'checked' : ''} disabled class="mt-1 mr-3" />
        <span>${item.text}</span>
      </li>
    `).join('')
    return `<ul class="task-list">${itemsHtml}</ul>`
  }
}

// 5. Table support (optimized styles)
const tableExtension = {
  name: 'table',
  level: 'block',
  renderer(token: any) {
    // Use marked's default table rendering, but add custom classes
    const header = token.header.map((cell: string) => `<th class="px-4 py-2 bg-gray-50 dark:bg-gray-800">${cell}</th>`).join('')
    const rows = token.cells.map((row: string[]) =>
      `<tr>${row.map(cell => `<td class="px-4 py-2 border-t border-gray-200 dark:border-gray-700">${cell}</td>`).join('')}</tr>`
    ).join('')
    return `<table class="min-w-full border-collapse my-6"><thead><tr>${header}</tr></thead><tbody>${rows}</tbody></table>`
  }
}

// Configure marked
marked.use({
  extensions: [mathExtension, wikiLinkExtension, videoExtension, taskListExtension],
  gfm: true,
  breaks: true,
  pedantic: false,
  mangle: false,
  headerIds: false
})

// Parse Markdown to HTML
const parsedHtml = computed(() => {
  if (!props.content) return ''
  return marked.parse(props.content)
})

// Handle internal link clicks
const handleInternalLinkClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const link = target.closest('a.internal-link')
  if (link) {
    e.preventDefault()
    const slug = link.getAttribute('data-slug')
    if (slug) {
      // Emit custom event for parent component to handle navigation
      emit('navigate', slug)
    }
  }
}

// Post-render processing: initialize KaTeX and other dynamic effects
watch(parsedHtml, async () => {
  await nextTick()
  if (contentRef.value) {
    // Render math formulas
    renderMathInElement(contentRef.value, {
      delimiters: [
        { left: '$$', right: '$$', display: true },
        { left: '$', right: '$', display: false },
        // Compatible with old \(\) and \[\] formats
        { left: '\\(', right: '\\)', display: false },
        { left: '\\[', right: '\\]', display: true }
      ],
      throwOnError: false,
      output: 'html',
      trust: true,
      // Ignore already rendered elements
      ignoredTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code']
    })

    // Add responsive wrapper to all tables
    const tables = contentRef.value.querySelectorAll('table')
    tables.forEach(table => {
      if (!table.parentElement?.classList.contains('table-responsive')) {
        const wrapper = document.createElement('div')
        wrapper.className = 'table-responsive overflow-x-auto my-4'
        table.parentNode?.insertBefore(wrapper, table)
        wrapper.appendChild(table)
      }
    })

    // Add language markers to all code blocks
    const preBlocks = contentRef.value.querySelectorAll('pre')
    preBlocks.forEach(pre => {
      const code = pre.querySelector('code')
      if (code && code.className) {
        const langMatch = code.className.match(/language-(\w+)/)
        if (langMatch) {
          pre.setAttribute('data-language', langMatch[1])
        }
      }
    })
  }
}, { immediate: true })

// Add click event listener when component mounts
onMounted(() => {
  contentRef.value?.addEventListener('click', handleInternalLinkClick)
})

// Remove click event listener when component unmounts
onUnmounted(() => {
  contentRef.value?.removeEventListener('click', handleInternalLinkClick)
})

const emit = defineEmits<{
  (e: 'navigate', slug: string): void
}>()
</script>

<template>
  <div
    ref="contentRef"
    class="prose-vim max-w-none"
    v-html="parsedHtml"
  ></div>
</template>

<style scoped>
/* Base typography - carefully designed reading experience */
.prose-vim {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 16px;
  line-height: 1.7;
  color: #2c3e50;
}

.dark .prose-vim {
  color: #e5e7eb;
}

/* Paragraphs */
.prose-vim :deep(p) {
  margin-bottom: 1.5em;
  line-height: 1.8;
}

/* Headings */
.prose-vim :deep(h1),
.prose-vim :deep(h2),
.prose-vim :deep(h3),
.prose-vim :deep(h4) {
  margin-top: 2.5em;
  margin-bottom: 1em;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.prose-vim :deep(h1) {
  font-size: 2.5em;
  border-bottom: 2px solid #eaecef;
  padding-bottom: 0.3em;
}

.prose-vim :deep(h2) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.prose-vim :deep(h3) { font-size: 1.5em; }
.prose-vim :deep(h4) { font-size: 1.25em; }

.dark .prose-vim :deep(h1),
.dark .prose-vim :deep(h2) {
  border-bottom-color: #2d2d2d;
}

/* Lists */
.prose-vim :deep(ul),
.prose-vim :deep(ol) {
  margin-bottom: 1.5em;
  padding-left: 2em;
}

.prose-vim :deep(li) {
  margin-bottom: 0.5em;
}

.prose-vim :deep(li > ul),
.prose-vim :deep(li > ol) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

/* Task lists */
.prose-vim :deep(.task-list) {
  list-style: none;
  padding-left: 0;
}

.prose-vim :deep(.task-item) {
  display: flex;
  align-items: flex-start;
  margin: 0.75em 0;
}

.prose-vim :deep(.task-item input[type="checkbox"]) {
  width: 1.2em;
  height: 1.2em;
  margin-right: 0.75em;
  margin-top: 0.2em;
  cursor: default;
  accent-color: #3b82f6;
}

/* Blockquotes */
.prose-vim :deep(blockquote) {
  border-left: 4px solid #e4e4e7;
  padding: 0.5em 1em;
  font-style: italic;
  margin: 1.5em 0;
  color: #52525b;
  background-color: #f9fafb;
  border-radius: 0 4px 4px 0;
}

.dark .prose-vim :deep(blockquote) {
  border-left-color: #3f3f46;
  color: #a1a1aa;
  background-color: #1f2937;
}

/* Code */
.prose-vim :deep(code) {
  font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', Consolas, Monaco, 'Andale Mono', monospace;
  background: #f4f4f5;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-size: 0.875em;
  color: #e83e8c;
}

.dark .prose-vim :deep(code) {
  background: #2d2d2d;
  color: #f9a8d4;
}

/* Code blocks */
.prose-vim :deep(pre) {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1.25rem;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 0.875em;
  line-height: 1.6;
  margin: 1.5em 0;
  position: relative;
}

.prose-vim :deep(pre)::before {
  content: attr(data-language);
  position: absolute;
  top: 0;
  right: 1rem;
  color: #9ca3af;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.prose-vim :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  font-size: inherit;
}

/* Images */
.prose-vim :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 2rem auto;
  display: block;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

/* Tables */
.prose-vim :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5em 0;
  font-size: 0.9em;
}

.prose-vim :deep(th) {
  background-color: #f8fafc;
  font-weight: 600;
  text-align: left;
  border-bottom: 2px solid #e2e8f0;
}

.dark .prose-vim :deep(th) {
  background-color: #1e293b;
  border-bottom-color: #334155;
}

.prose-vim :deep(td) {
  border-bottom: 1px solid #e2e8f0;
}

.dark .prose-vim :deep(td) {
  border-bottom-color: #334155;
}

.prose-vim :deep(th),
.prose-vim :deep(td) {
  padding: 0.75rem 1rem;
}

.prose-vim :deep(tr:hover) {
  background-color: #fafafa;
}

.dark .prose-vim :deep(tr:hover) {
  background-color: #2d2d2d;
}

/* Video container */
.prose-vim :deep(.video-wrapper) {
  background: #0a0a0a;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  margin: 2rem 0;
}

.prose-vim :deep(.video-wrapper iframe) {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

.prose-vim :deep(.video-wrapper video) {
  width: 100%;
  display: block;
}

/* Wiki links */
.prose-vim :deep(.internal-link) {
  color: #3b82f6;
  text-decoration: none;
  border-bottom: 1px dashed #3b82f6;
  transition: all 0.2s ease;
  cursor: pointer;
}

.prose-vim :deep(.internal-link:hover) {
  color: #2563eb;
  border-bottom-style: solid;
}

/* Math formulas */
.prose-vim :deep(.math-block) {
  display: block;
  text-align: center;
  margin: 1.5em 0;
  overflow-x: auto;
  padding: 0.5em;
  background: #f9fafb;
  border-radius: 4px;
}

.dark .prose-vim :deep(.math-block) {
  background: #1f2937;
}

.prose-vim :deep(.math-inline) {
  display: inline-block;
  padding: 0 0.2em;
}

/* Horizontal rule */
.prose-vim :deep(hr) {
  border: none;
  border-top: 2px solid #eaecef;
  margin: 2.5em 0;
}

.dark .prose-vim :deep(hr) {
  border-top-color: #2d2d2d;
}

/* Links */
.prose-vim :deep(a:not(.internal-link)) {
  color: #3b82f6;
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: color 0.2s;
}

.prose-vim :deep(a:not(.internal-link):hover) {
  color: #2563eb;
}

/* Responsive table container */
.prose-vim :deep(.table-responsive) {
  overflow-x: auto;
  margin: 1.5em 0;
  border-radius: 8px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

/* Footnotes */
.prose-vim :deep(.footnotes) {
  margin-top: 3em;
  padding-top: 1em;
  border-top: 1px solid #eaecef;
  font-size: 0.875em;
  color: #6b7280;
}

.dark .prose-vim :deep(.footnotes) {
  border-top-color: #2d2d2d;
  color: #9ca3af;
}
</style>
