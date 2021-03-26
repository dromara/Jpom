import Vue from 'vue';
import axios from 'axios';
import Qs from 'qs';
import store from '../store';

import { notification } from 'ant-design-vue';

// axios.defaults.baseURL = 'http://localhost:2122'
const domain = document.getElementById('domainPath').value;
let $global_loading;
let startTime;

const request = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
  },
  responseType: 'json'
})

// 请求拦截器
request.interceptors.request.use(config => {
  $global_loading = Vue.prototype.$loading.service({
    lock: true,
    text: '加载数据中，请稍候...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  });
  startTime = new Date().getTime();
  // 处理数据
  if (domain) {
    // 防止 url 出现 //
    config.url = (domain + config.url).replaceAll('//', '/');
  }
  if (config.headers['Content-Type'].indexOf('application/x-www-form-urlencoded') !== -1) {
    config.data = Qs.stringify(config.data); 
  }
  config.headers['JPOM-USER-TOKEN'] = store.getters.getToken;
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
request.interceptors.response.use(response => {
  const endTime = new Date().getTime();
  if (endTime - startTime < 1000) {
    setTimeout(() => {
      $global_loading.close();
    }, 300);
  } else {
    $global_loading.close();
  }
  // 如果 responseType 是 blob 表示是下载文件
  if (response.request.responseType === 'blob') {
    return response.data;
  }
  // 判断返回值，权限等...
  const res = response.data;
  // 如果 headers 里面配置了 tip: no 就不用弹出提示信息
  if (response.config.headers['tip']) {
    return res;
  }
  if (res.code !== 200) {
    notification.error({
      message: res.msg,
      description: response.config.url,
      duration: 2
    });
  }
  // 如果是登录信息失效
  if (res.code === 800) {
    store.dispatch('logOut').then(() => {
      location.reload();
    })
  }
  return res;
}, error => {
  $global_loading.close();
  const { status, statusText } = error;
  if (!status) {
    notification.error({
      message: 'Network Error',
      description: '网络开了小差！请重试...',
      duration: 2
    });
  } else {
    notification.error({
      message: status,
      description: statusText,
      duration: 2
    });
  }
  return Promise.reject(error);
});

export default request
