import axios from './config';

// 日志列表
export function getLogList() {
  return axios({
    url: '/system/log_data.json',
    method: 'post'
  })
}

/**
 * 下载日志
 * @param {nodeId, path} params
 */
export const downloadLogUri = '/system/log_download'

/**
 * 删除日志
 * @param {nodeId, path} params 
 */
export function deleteLog(params) {
  return axios({
    url: '/system/log_del.json',
    method: 'post',
    data: params
  })
}