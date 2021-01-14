import axios from './config';

// 分发列表
export function getDishPatchList() {
  return axios({
    url: '/outgiving/dispatch-list',
    method: 'post'
  })
}

// reqId
export function getReqId() {
  return axios({
    url: '/outgiving/get-reqId',
    method: 'get'
  })
}

/**
 * 编辑分发
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  node_xxx: xxx 表示节点 ID
 *  afterOpt: 发布后操作
 * } params 
 */
export function editDispatch(params) {
  return axios({
    url: '/outgiving/save',
    method: 'post',
    data: params
  })
}

/**
 * 编辑分发项目
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  afterOpt: 发布后操作
 *  runMode: 运行方式
 *  mainClass: 启动类
 *  javaExtDirsCp: 目录地址
 *  whitelistDirectory: 白名单地址
 *  lib: lib
 *  add_xxx: xxx 表示添加的节点信息
 *  xxx_token: xxx 节点的 webhook 地址
 *  xxx_jvm: jvm 参数
 *  xxx_args: args 参数
 *  xxx_javaCopyIds: xxx 节点副本 ID （如果有副本，还需要加上 xxx_jvm_${副本ID} | xxx_args_${副本ID} 参数）
 * } params 
 */
export function editDispatchProject(params) {
  return axios({
    url: '/outgiving/save_project',
    method: 'post',
    data: params
  })
}

/**
 * 删除分发
 * @param {*} id 分发 ID
 */
export function deleteDisPatch(id) {
  return axios({
    url: '/outgiving/del.json',
    method: 'post',
    data: {id}
  })
}

// 创建分发项目

// 获取分发白名单数据
export function getDispatchWhiteList() {
  return axios({
    url: '/outgiving/white-list',
    method: 'post'
  })
}

/**
 * 编辑分发白名单
 * @param {*} params 
 */
export function editDispatchWhiteList(params) {
  return axios({
    url: '/outgiving/whitelistDirectory_submit',
    method: 'post',
    data: params
  })
}