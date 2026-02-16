import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { injectNotificationStyles } from '@/utils/feedback'
import 'virtual:uno.css'
import './style.css'
import App from './App.vue'
import router from './router'
import BrowserIcons from '@/utils/BrowserIcons.vue'

const app = createApp(App)
const pinia = createPinia()

injectNotificationStyles()

app.use(pinia)
app.use(router)

app.component('BrowserIcons', BrowserIcons)

app.mount('#app')