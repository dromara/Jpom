import axios from './config';

/**
 * white list data
 * @param {nodeId} nodeId 
 */
export function getWhiteList(nodeId) {
  return axios({
    url: '/node/system/white-list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * edit white list data
 * @param {
 *  nodeId: 节点 ID,
 *  project: 项目目录,
 *  certificate: 证书目录,
 *  nginx: Nginx 目录
 * } params 
 */
export function editWhiteList(params) {
  return axios({
    url: '/node/system/whitelistDirectory_submit',
    method: 'post',
    data: params
  })
}