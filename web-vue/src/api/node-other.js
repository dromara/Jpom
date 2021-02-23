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