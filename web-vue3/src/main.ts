// import '@/assets/style.css'
import '@/assets/reset.less'
import App from './App.vue'
import Antd from 'ant-design-vue'
import router from './router'
import 'ant-design-vue/dist/antd.css'
const pinia = createPinia()

const app = createApp(App)

app.use(Antd).use(router).use(pinia)

app.mount('#app')
