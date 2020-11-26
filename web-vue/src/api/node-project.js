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

// jdk 列表
export function getJdkList(params) {
  return axios({
    url: '/node/manage/jdk/list',
    method: 'post',
    data: params
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