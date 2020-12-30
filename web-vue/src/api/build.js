import axios from './config';

// 分组列表
export function getBuildGroupList() {
  return axios({
    url: '/build/group-list',
    method: 'get'
  })
}

/**
 * 构建列表
 * @param {
 *  group: 分组名称
 * } params 
 */
export function getBuildList(params) {
  return axios({
    url: '/build/list_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 获取仓库分支信息
 * @param {
 *  url: 仓库地址
 *  userName: 用户名
 *  userPwd: 密码
 * } params 
 */
export function getBranchList(params) {
  return axios({
    url: '/build/branchList.json',
    method: 'post',
    timeout: 0,
    data: params
  })
}

/**
 * 编辑构建信息
 * @param {
 *  id: 构建 ID
 *  name: 构建名称
 *  gitUrl: 仓库地址
 *  userName: 登录用户
 *  password: 登录密码
 *  resultDirFile: 构建产物目录
 *  script: 构建命令
 *  releaseMethod: 发布方法
 *  branchName: 分支名称
 *  group: 分组名称
 *  repoType: 仓库类型 0: GIT | 1: SVN
 * } params 
 */
export function editBuild(params) {
  return axios({
    url: '/build/updateBuild',
    method: 'post',
    data: params
  })
}

/**
 * 删除构建信息
 * @param {*} id 
 */
export function deleteBuild(id) {
  return axios({
    url: '/build/delete.json',
    method: 'post',
    data: {id}
  })
}

/**
 * 获取触发器地址
 * @param {*} id 
 */
export function getTriggerUrl(id) {
  return axios({
    url: '/build/trigger-url',
    method: 'post',
    data: {id}
  })
}

/**
 * 重置触发器
 * @param {*} id 
 */
export function resetTrigger(id) {
  return axios({
    url: '/build/trigger_rest.json',
    method: 'post',
    data: {id}
  })
}

/**
 * 清理构建
 * @param {*} id 
 */
export function clearBuid(id) {
  return axios({
    url: '/build/cleanSource.json',
    method: 'post',
    data: {id}
  })
}