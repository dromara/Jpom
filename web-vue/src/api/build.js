import axios from './config';

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
 *  repoType: 仓库类型 GIT | SVN
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