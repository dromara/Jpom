import Vue from "vue";
import App from "./App.vue";
import i18n from "./locales";

import "./components/lazy_antd";

import "./assets/reset.less";

const introJs = require("intro.js");
import "intro.js/introjs.css";
import "@/assets/intro-custom-themes.css";

import router from "./router";
import store from "./store";
import "./router/auth";
import VueClipBoard from "vue-clipboard2";

// debug routerBase
window.routerBase = window.routerBase === "<routerBase>" ? "" : window.routerBase;

// introJs 按钮中文
const intro = introJs();
intro.setOptions({
  prevLabel: i18n.t("common.lastStep"),
  nextLabel: i18n.t("common.nextStep"),
  doneLabel: i18n.t("common.endGuide"),
});

Vue.config.productionTip = false;
// Vue.prototype.$loading = Loading;
Vue.prototype.$introJs = intro;

// 全局 loading
Vue.prototype.$setLoading = function (props) {
  let closeAll = false;
  if (typeof props === "boolean") {
    props = { spinning: props };
  } else if (typeof props === "string") {
    if (props === "closeAll") {
      // 关闭所有
      closeAll = true;
    }
    props = {};
  } else if (Object.prototype.toString.call(props) !== "[object Object]") {
    props = {};
  }
  // 计数
  const loadingCount = this.$app.globalLoadingProps.loadingCount || 0;
  if (props.spinning) {
    props.loadingCount = loadingCount + 1;
  } else {
    props.loadingCount = Math.max(loadingCount - 1, 0);
    props.spinning = props.loadingCount > 0;
  }
  props.spinning = props.spinning || closeAll;
  props.loadingCount = closeAll ? 0 : props.loadingCount;
  props.wrapperClassName = props.spinning ? "globalLoading" : "";
  this.$app.globalLoadingProps = { ...this.$app.globalLoadingProps, ...props };
};

Vue.use(VueClipBoard);

new Vue({
  router,
  store,
  i18n,
  render: (h) => h(App),
}).$mount("#app");
