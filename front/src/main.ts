import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { injectNotificationStyles } from '@/utils/feedback'
import 'virtual:uno.css'
import './style.css'
import App from './App.vue'
import router from './router'
import BrowserIcons from '@/utils/BrowserIcons.vue'
import { sidebarApi } from '@/api/sidebar'

async function initApp() {
  try {
    const siteConfig = await sidebarApi.getSiteConfig()
    if (siteConfig?.siteName) {
      document.title = siteConfig.siteName
    } else {
      document.title = 'My Blog'
    }
  } catch (error) {
    console.error('Failed to fetch site config for title:', error)
    document.title = 'My Blog'
  }
}
initApp()

const app = createApp(App)
const pinia = createPinia()

injectNotificationStyles()

app.use(pinia)
app.use(router)

app.component('BrowserIcons', BrowserIcons)

app.mount('#app')