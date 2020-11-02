import axios from 'axios';
import Qs from 'qs';

import { notification } from 'ant-design-vue';

// axios.defaults.baseURL = 'http://localhost:2122'

const request = axios.create({
  timeout: 3000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
  },
  responseType: 'json'
})

// 请求拦截器
request.interceptors.request.use(config => {
  // 处理数据
  config.data = Qs.stringify(config.data); 
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
request.interceptors.response.use(response => {
  // 判断返回值，权限等...
  const res = response.data;
  if (res.code !== 200) {
    notification.error({
      message: res.msg,
      description: response.config.url,
      duration: 2
    });
  }
  return res;
}, error => {
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
