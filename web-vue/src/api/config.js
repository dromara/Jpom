import Vue from "vue";
import axios from "axios";
import Qs from "qs";
import store from "../store";
import router from "../router";
import { NO_NOTIFY_KEY, NO_LOADING_KEY, TOKEN_HEADER_KEY, CACHE_WORKSPACE_ID, LOADING_TIP } from "@/utils/const";
import { refreshToken } from "./user";

import { notification } from "ant-design-vue";

// axios.defaults.baseURL = 'http://localhost:2122'
let startTime;
//
const delTimeout = 20 * 1000;
//
const apiTimeout = window.apiTimeout === "<apiTimeout>" ? delTimeout : window.apiTimeout;

const request = axios.create({
  timeout: apiTimeout || delTimeout,
  headers: {
    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
  },
  responseType: "json",
});

const pro = process.env.NODE_ENV === "production";

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 如果 headers 里面配置了 loading: no 就不用 loading
    if (!config.headers[NO_LOADING_KEY]) {
      Vue.prototype.$setLoading({
        spinning: true,
        tip: config.headers[LOADING_TIP] || "加载数据中，请稍候...",
      });
      startTime = new Date().getTime();
    }
    delete config.headers[LOADING_TIP];
    // 处理数据
    if (window.routerBase) {
      // 防止 url 出现 //
      config.url = (window.routerBase + config.url).replace(new RegExp("//", "gm"), "/");
    }
    if (config.headers["Content-Type"].indexOf("application/x-www-form-urlencoded") !== -1) {
      config.data = Qs.stringify(config.data);
    }
    config.headers[TOKEN_HEADER_KEY] = store.getters.getToken;
    config.headers[CACHE_WORKSPACE_ID] = getWid();
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

function getWid() {
  let wid = router.app.$route.query.wid;
  if (!wid) {
    wid = getHashVars().wid;
  }
  return wid ? wid : store.getters.getWorkspaceId;
}

function getHashVars() {
  var vars = {};
  location.hash.replace(/[?&]+([^=&]+)=([^&]*)/gi, function (m, key, value) {
    vars[key] = value;
  });
  return vars;
}

// 响应拦截器
request.interceptors.response.use(
  async (response) => {
    // 如果 headers 里面配置了 loading: no 就不用 loading
    if (!response.config?.headers[NO_LOADING_KEY]) {
      const endTime = new Date().getTime();
      const waitTime = endTime - startTime < 1000 ? 300 : 0;
      // 时间过短延迟一定时间
      await waitTimePromise(waitTime, () => {
        Vue.prototype.$setLoading(false);
      });
      return wrapResult(response);
    } else {
      return wrapResult(response);
    }
  },
  (error) => {
    if (!error.response) {
      // 网络异常
      Vue.prototype.$setLoading(false);
      notification.error({
        message: "Network Error",
        description: "网络开了小差！请重试...:" + error,
      });
      return Promise.reject(error);
    }
    // 如果 headers 里面配置了 loading: no 就不用 loading
    if (!error.response.config.headers[NO_LOADING_KEY]) {
      Vue.prototype.$setLoading(false);
    }
    // 如果 headers 里面配置了 tip: no 就不用弹出提示信息
    if (!error.response.config.headers[NO_NOTIFY_KEY]) {
      const { status, statusText, data } = error.response;
      if (!status) {
        notification.error({
          message: "Network Error",
          description: "网络开了小差！请重试...:" + error,
        });
      } else {
        notification.error({
          message: "状态码错误 " + status,
          description: (statusText || "") + (data || ""),
        });
      }
    }
    return Promise.reject(error);
  }
);

// 等待 x ms
function waitTimePromise(time, fn) {
  return new Promise((resolve) => {
    setTimeout(() => {
      fn && fn();
      resolve();
    }, time);
  });
}

// 判断结果
function wrapResult(response) {
  // 如果 responseType 是 blob 表示是下载文件
  if (response.request.responseType === "blob") {
    return response.data;
  }
  // 判断返回值，权限等...
  const res = response.data;

  // 先判断 jwt token 状态
  if (res.code === 800 || res.code === 801) {
    return checkJWTToken(res, response);
  }

  // 禁止访问
  if (res.code === 999) {
    notification.error({
      message: "禁止访问",
      description: "禁止访问,当前IP限制访问",
    });
    router.push("/system/ipAccess");
    return false;
  }

  // 其他情况
  if (res.code !== 200) {
    // 如果 headers 里面配置了 tip: no 就不用弹出提示信息
    if (!response.config.headers[NO_NOTIFY_KEY]) {
      notification.error({
        message: "提示信息 " + (pro ? "" : response.config.url),
        description: res.msg,
      });
      console.error(response.config.url, res);
    }
  }

  return res;
}

// 判断 jwt token 状态
function checkJWTToken(res, response) {
  // 如果是登录信息失效
  if (res.code === 800) {
    notification.warn({
      message: "提示信息 " + (pro ? "" : response.config.url),
      description: res.msg,
    });
    console.error(response.config.url, res);
    store.dispatch("logOut").then(() => {
      router.push("/login");
      location.reload();
    });
    return false;
  }
  // 如果 jwt token 还可以续签
  if (res.code === 801) {
    notification.close();
    notification.info({
      message: "登录信息过期，尝试自动续签...",
      description: "如果不需要自动续签，请修改配置文件。该续签将不会影响页面。",
    });
    // 续签且重试请求
    return redoRequest(response.config);
  }
}

// 刷新 jwt token 并且重试上次请求
function redoRequest(config) {
  return new Promise((resolve) => {
    Promise.resolve(refreshToken()).then((result) => {
      if (result.code === 200) {
        // 调用 store action 存储当前登录的用户名和 token
        store.dispatch("login", result.data);
        resolve();
      }
    });
  }).then(() => {
    // 重试原来的请求
    return request(config);
  });
}

export default request;

//
export function loadRouterBase(url, params) {
  const paramsObj = params || {};
  paramsObj[CACHE_WORKSPACE_ID] = getWid();
  const paramsQuery = Qs.stringify(paramsObj);
  return `${((window.routerBase || "") + url).replace(new RegExp("//", "gm"), "/")}?${paramsQuery}`;
}
