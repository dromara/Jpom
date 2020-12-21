import axios from './config';

// 监控列表
export function getMonitorList(params) {
  return axios({
    url: '/monitor/getMonitorList',
    method: 'post',
    data: params
  })
}