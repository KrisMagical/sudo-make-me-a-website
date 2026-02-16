import { defineConfig } from 'vite'
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
    }
  }
})