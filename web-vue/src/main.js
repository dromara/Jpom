import Vue from 'vue';
import App from './App.vue';

import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/antd.css';
import './assets/reset.css';
import { Tree, Progress } from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

import router from './router';
import store from './store';
import './router/auth';

Vue.config.productionTip = false;
Vue.use(Antd);
Vue.use(Tree);
Vue.use(Progress);

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app')
