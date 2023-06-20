// import '@/assets/style.css'
import '@/assets/reset.less'
import App from './App.vue'
import Antd from 'ant-design-vue'
import router from './router'
import i18n from '@/locales'
import 'ant-design-vue/dist/antd.css'
import '@/router/auth'
const pinia = createPinia()

const app = createApp(App)

app
  .use(Antd)
  .use(router)
  .use(pinia)
  .use(i18n)

app.mount('#app')
