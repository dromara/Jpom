import { createApp } from 'vue'
import '@/assets/style.css'
import '@/assets/reset.less'
import App from './App.vue'
import Antd from 'ant-design-vue'
import router from './router'
import globalComponent from '@/components/lazy_antd'

import { createPinia } from 'pinia'

const pinia = createPinia()

import 'ant-design-vue/dist/antd.css'

const app = createApp(App)

app.use(Antd).use(router).use(pinia)

// 注册全局的组件 （对所有需要注册的组件进行遍历并注册）
for (const componentItme in globalComponent) {
  app.component(componentItme, globalComponent[componentItme])
}
app.mount('#app')
