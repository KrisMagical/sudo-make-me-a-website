import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import UnoCSS from 'unocss/vite'
import detect from 'detect-port'
import fs from 'fs/promises'
import path from 'path'

const BACKEND_PORT_FILE = path.resolve('.backend-port')
const FRONTEND_PORT_FILE = path.resolve('.frontend-port')

export default defineConfig(async ({ command, mode }) => {
  let backendTarget = 'http://localhost:8080'
  try {
    const backendPortContent = await fs.readFile(BACKEND_PORT_FILE, 'utf-8')
    backendTarget = backendPortContent.trim()
    console.log(`Backend service address: ${backendTarget}`)
  } catch {
    console.log('Backend port file not found, using default proxy address: http://localhost:8080')
  }

  const DEFAULT_PORT = 3000
  const port = await detect(DEFAULT_PORT)
  if (port !== DEFAULT_PORT) {
    console.log(`\nPort ${DEFAULT_PORT} The port is already in use and has been automatically switched to ${port}\n`)
  }

  const frontendUrl = `http://localhost:${port}`
  await fs.writeFile(FRONTEND_PORT_FILE, frontendUrl)
  console.log(`The front-end address has been recorded: ${frontendUrl}`)

  return {
    plugins: [vue(), UnoCSS()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port,
      open: false,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
        }
      }
    },
    test: {
      environment: 'jsdom',
      globals: true
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined
            if (id.includes('md-editor-v3')) return 'md-editor'
            if (id.includes('@codemirror/language-data')) return 'codemirror-language-data'
            if (id.includes('@codemirror/lang-angular')) return 'codemirror-lang-web'
            if (id.includes('@codemirror/lang-html')) return 'codemirror-lang-web'
            if (id.includes('@codemirror/lang-javascript')) return 'codemirror-lang-web'
            if (id.includes('@codemirror/lang-vue')) return 'codemirror-lang-web'
            if (id.includes('@codemirror/lang-xml')) return 'codemirror-lang-web'
            if (id.includes('@codemirror/lang-css')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-json')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-less')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-sass')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-markdown')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-yaml')) return 'codemirror-lang-style'
            if (id.includes('@codemirror/lang-cpp')) return 'codemirror-lang-systems'
            if (id.includes('@codemirror/lang-go')) return 'codemirror-lang-systems'
            if (id.includes('@codemirror/lang-rust')) return 'codemirror-lang-systems'
            if (id.includes('@codemirror/lang-java')) return 'codemirror-lang-jvm'
            if (id.includes('@codemirror/lang-sql')) return 'codemirror-lang-jvm'
            if (id.includes('@codemirror/lang-php')) return 'codemirror-lang-script'
            if (id.includes('@codemirror/lang-python')) return 'codemirror-lang-script'
            if (id.includes('@codemirror/lang-jinja')) return 'codemirror-lang-template'
            if (id.includes('@codemirror/lang-liquid')) return 'codemirror-lang-template'
            if (id.includes('@codemirror/lang-wast')) return 'codemirror-lang-extra'
            if (id.includes('@codemirror/lang-')) return 'codemirror-lang-extra'
            if (id.includes('@codemirror/view')) return 'codemirror-view'
            if (id.includes('@codemirror/state')) return 'codemirror-state'
            if (id.includes('@codemirror/language')) return 'codemirror-language'
            if (id.includes('@codemirror/autocomplete')) return 'codemirror-tools'
            if (id.includes('@codemirror/commands')) return 'codemirror-tools'
            if (id.includes('@codemirror/search')) return 'codemirror-tools'
            if (id.includes('@codemirror/lint')) return 'codemirror-tools'
            if (id.includes('@codemirror') || id.includes('codemirror')) return 'codemirror-shell'
            if (id.includes('@lezer')) return 'lezer'
            if (id.includes('style-mod') || id.includes('w3c-keyname') || id.includes('crelt')) return 'codemirror-core'
            if (id.includes('markdown-it')) return 'markdown-it'
            if (id.includes('@tiptap') || id.includes('tiptap-markdown')) return 'tiptap'
            if (id.includes('katex')) return 'math'
            if (id.includes('marked')) return 'markdown'
            if (id.includes('@iconify')) return 'icons'
            if (id.includes('axios')) return 'http'
            if (id.includes('unocss') || id.includes('@unocss')) return 'uno'
            if (id.includes('vue') || id.includes('pinia')) return 'vue-vendor'
            return 'vendor'
          }
        }
      }
    }
  }
})
