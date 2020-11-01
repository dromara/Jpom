import axios from 'axios';
import Qs from 'qs';

// axios.defaults.baseURL = 'http://localhost:2122'

// 请求拦截器
axios.interceptors.request.use(config => {
  // 处理数据
  config.data = Qs.stringify(config.data); 
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
axios.interceptors.response.use(response => {
  // 判断返回值，权限等...
  return response.data;
}, error => {
  console.log('网络开了小差！请重试...');
  return Promise.reject(error);
});

export default axios
