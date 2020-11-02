import Vue from 'vue';
import App from './App.vue';

import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/antd.css';
import './assets/reset.css';

import router from './router';
import store from './store';
import './router/auth';

Vue.config.productionTip = false;
Vue.use(Antd);

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app')
