import { defineConfig, presetUno, presetIcons } from 'unocss'
import { presetWebFonts } from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
    presetWebFonts({
      fonts: {
        sans: 'Inter',
        mono: 'Fira Code',
      },
    }),
  ],
  theme: {
    colors: {
      zinc: {
        50: '#fafafa',
        100: '#f4f4f5',
        200: '#e4e4e7',
        300: '#d4d4d8',
        400: '#a1a1aa',
        500: '#71717a',
        600: '#52525b',
        700: '#3f3f46',
        800: '#27272a',
        900: '#18181b',
        950: '#09090b',
      }
    }
  },
  shortcuts: {
    'btn-primary': 'px-4 py-2 !bg-zinc-900 dark:!bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors text-sm font-bold uppercase tracking-tighter',
    'btn-secondary': 'border border-zinc-300 dark:border-zinc-700 px-4 py-2 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors text-sm font-bold uppercase tracking-tighter',
    'prose-vim': 'font-mono leading-relaxed tracking-tight',
  }
})