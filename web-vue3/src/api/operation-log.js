import axios from './config';

// 操作日志列表
export function getOperationLogList(params) {
  return axios({
    url: '/user/log/list_data.json',
    method: 'post',
    data: params
  })
}