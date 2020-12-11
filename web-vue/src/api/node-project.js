/**
 * 节点管理 api
 */
import axios from './config';

// 项目列表
export function getProjectList(params) {
  return axios({
    url: '/node/manage/getProjectInfo',
    method: 'post',
    data: params
  })
}

/**
 * jdk 列表
 * @param {String} nodeId 节点 ID
 */
export function getJdkList(nodeId) {
  return axios({
    url: '/node/manage/jdk/list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * jdk 编辑
 * @param {nodeId, id, name, path} params
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.path jdk 路径
 */
export function editJdk(params) {
  return axios({
    url: '/node/manage/jdk/update',
    method: 'post',
    data: params
  })
}

/**
 * 删除 JDK
 * @param {nodeId, id} params 
 * params.nodeId 节点 ID 
 * params.id 编辑修改时判断 ID
 */
export function deleteJdk(params) {
  return axios({
    url: '/node/manage/jdk/delete',
    method: 'post',
    data: params
  })
}

/**
 * 加载项目分组列表
 * @param {String} nodeId 节点 ID
 */
export function getPorjectGroupList(nodeId) {
  return axios({
    url: '/node/manage/project-group-list',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 项目白名单列表
 * @param {String} nodeId 节点 ID
 */
export function getProjectAccessList(nodeId) {
  return axios({
    url: '/node/manage/project-access-list',
    method: 'post',
    data: {nodeId}
  })
}

// 编辑项目
export function editProject(params) {
  return axios({
    url: '/node/manage/saveProject',
    method: 'post',
    data: params
  })
}