import axios from './config';

/**
 * tomcat 列表
 * @param {String} nodeId 节点 ID
 */
export function getTomcatList(nodeId) {
  return axios({
    url: '/node/tomcat/list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * Tomcat 编辑
 * @param {nodeId, id, name, path, port, appBase} params
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.path jdk 路径
 * params.port 端口
 * params.appBase appBase 路径
 */
export function editTomcat(params) {
  return axios({
    url: '/node/tomcat/save',
    method: 'post',
    data: params
  })
}

/**
 * 删除 Tomcat
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function deleteTomcat(params) {
  return axios({
    url: '/node/tomcat/delete',
    method: 'post',
    data: params
  })
}

/**
 * 查询项目列表
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function getTomcatProjectList(params) {
  return axios({
    url: '/node/tomcat/getTomcatProject',
    method: 'post',
    data: params
  })
}

/**
 * 查询 tomcat 状态
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function getTomcatStatus(params) {
  return axios({
    url: '/node/tomcat/getTomcatStatus',
    method: 'post',
    data: params
  })
}

/**
 * tomcat 日志列表
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function getTomcatLogList(params) {
  return axios({
    url: '/node/tomcat/getLogList',
    method: 'post',
    data: params
  })
}

/**
 * 删除 Tomcat 日志
 * @param {nodeId, path, filename, id} params 
 * params.nodeId 节点 ID 
 * params.path tomcat 日志目录
 * params.filename 日志名称
 * params.id 编辑修改时判断 ID
 */
export function deleteTomcatLog(params) {
  return axios({
    url: '/node/tomcat/deleteFile',
    method: 'post',
    data: params
  })
}

/**
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path, filename, id} params 
 * params.nodeId 节点 ID 
 * params.path tomcat 日志目录
 * params.filename 日志名称
 * params.id 编辑修改时判断 ID
 */
export function downloadTomcatFile(params) {
  return axios({
    url: '/node/tomcat/download',
    method: 'get',
    responseType: 'blob',
    params
  })
}