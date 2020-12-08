import axios from './config';

/**
 * 日志列表
 * @param {nodeId} params 
 */
export function getLogList(params) {
  return axios({
    url: '/system/log_data.json',
    method: 'post',
    data: params
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

/**
 * server 缓存数据
 */
export function getServerCache() {
  return axios({
    url: '/system/server-cache',
    method: 'post'
  })
}

/**
 * 节点缓存数据
 * @param {String} nodeId 
 */
export function getNodeCache(nodeId) {
  return axios({
    url: '/system/node_cache.json',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 清空缓存
 * @param {
 *  type: 类型
 *  nodeId: 节点 ID
 * } params 
 */
export function clearCache(params) {
  return axios({
    url: '/system/clearCache.json',
    method: 'post',
    data: params
  })
}