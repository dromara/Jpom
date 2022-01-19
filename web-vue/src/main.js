import Vue from "vue";
import App from "./App.vue";

import "./components/lazy_antd";

import "./assets/reset.css";
const introJs = require("intro.js");
import "intro.js/introjs.css";
// import 'intro.js/themes/introjs-flattener.css';
import "@/assets/intro-custom-themes.css";

import router from "./router";
import store from "./store";
import "./router/auth";
import VueClipBoard from 'vue-clipboard2'

// debug routerBase
window.routerBase = window.routerBase === "<routerBase>" ? "" : window.routerBase;

// introJs 按钮中文
const intro = introJs();
intro.setOptions({
  prevLabel: "上一步",
  nextLabel: "下一步",
  // skipLabel: "跳过",
  doneLabel: "结束引导",
});

Vue.config.productionTip = false;
// Vue.prototype.$loading = Loading;
Vue.prototype.$introJs = intro;

// 全局 loading
Vue.prototype.$setLoading = function (props) {
  if (typeof props === "boolean") {
    props = { spinning: props };
  } else if (Object.prototype.toString.call(props) !== "[object Object]") {
    props = {};
  }
  // 计数
  const loadingCount = this.$app.globalLoadingProps.loadingCount || 0;
  if (props.spinning) {
    props.loadingCount = loadingCount + 1;
  } else {
    props.loadingCount = loadingCount - 1;
    props.loadingCount = Math.max(props.loadingCount, 0);
    props.spinning = props.loadingCount > 0;
  }
  //console.log(props);
  props.wrapperClassName = props.spinning ? "globalLoading" : "";
  this.$app.globalLoadingProps = { ...this.$app.globalLoadingProps, ...props };
};

Vue.use(VueClipBoard);

new Vue({
  router,
  store,
  render: (h) => h(App),
}).$mount("#app");
