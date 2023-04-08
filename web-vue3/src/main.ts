import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import Antd from 'ant-design-vue'
import router from './router'

import { createPinia } from 'pinia'

const pinia = createPinia()

import 'ant-design-vue/dist/antd.css'

const app = createApp(App)

app.use(Antd).use(router).use(pinia).mount('#app')
