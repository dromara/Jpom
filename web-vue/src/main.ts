import '@/assets/style.less'
// 暂时不引入
// import '@/assets/reset.less'
import App from './App.vue'
import router from './router'
import '@/router/auth'
const pinia = createPinia()

const app = createApp(App)

app.use(router).use(pinia)

app.mount('#app')
