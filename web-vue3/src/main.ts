// import '@/assets/style.css'
import '@/assets/reset.less'
import App from './App.vue'
import Antd from 'ant-design-vue'
import router from './router'
import {createPinia, Pinia} from 'pinia'
import { message, notification, Modal } from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css'
const pinia: Pinia = createPinia()

const app = createApp(App)

app.use(Antd).use(router).use(pinia)

// 注册全局的组件
app.config.globalProperties.$message = message
app.config.globalProperties.$notification = notification
//
app.config.globalProperties.$confirm = Modal.confirm
app.config.globalProperties.$info = Modal.info
app.config.globalProperties.$error = Modal.error
app.config.globalProperties.$warning = Modal.warning
app.config.globalProperties.$success = Modal.success

app.mount('#app')
