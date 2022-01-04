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
  props.wrapperClassName = props.spinning ? "globalLoading" : "";
  this.$app.globalLoadingProps = { ...this.$app.globalLoadingProps, ...props };
};

new Vue({
  router,
  store,
  render: (h) => h(App),
}).$mount("#app");
