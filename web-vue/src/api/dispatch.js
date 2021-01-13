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