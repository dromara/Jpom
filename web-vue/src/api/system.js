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
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path} params 
 */
export function downloadFile(params) {
  return axios({
    url: '/system/log_download',
    method: 'get',
    responseType: 'blob',
    params
  })
}

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