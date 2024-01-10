/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import Vue from "vue";
import App from "./App.vue";

import "./components/lazy_antd";

import "./assets/reset.less";
const introJs = require("intro.js");
import "intro.js/introjs.css";
// import 'intro.js/themes/introjs-flattener.css';
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
