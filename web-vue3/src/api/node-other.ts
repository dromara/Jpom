import axios from './config'

/************************** */

/**
 * script 列表
 * @param {String} nodeId 节点 ID
 */
export function getScriptList(params: any) {
  return axios({
    url: '/node/script/list',
    method: 'post',
    data: params
  })
}

/**
 * script 服务端中的所有列表
 */
export function getScriptListAll(params: any) {
  return axios({
    url: '/node/script/list_all',
    method: 'post',
    data: params
  })
}

// 脚本模版日志列表
export function getScriptLogList(params: any) {
  return axios({
    url: '/node/script_log/list',
    method: 'post',
    data: params
  })
}

// 删除执行记录
export function scriptDel(params: any) {
  return axios({
    url: '/node/script_log/del',
    method: 'post',
    data: params
  })
}

//执行记录 详情
export function scriptLog(params: any) {
  return axios({
    url: '/node/script_log/log',
    method: 'post',
    data: params,
    headers: {
      tip: 'no'
    }
  })
}

/**
 * Script 编辑
 * @param {nodeId, id, name, path, port, appBase} params
 * params.type: add 表示添加
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.context 内容
 */
export function editScript(params: any) {
  return axios({
    url: '/node/script/save.json',
    method: 'post',
    data: params
  })
}

export function itemScript(params: any) {
  return axios({
    url: '/node/script/item.json',
    method: 'get',
    params: params
  })
}

export function syncScript(params: any) {
  return axios({
    url: '/node/script/sync',
    method: 'get',
    params: params
  })
}
export const triggerExecTypeMap = {
  0: '手动',
  1: '自动',
  2: '触发器'
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data: any) {
  return axios({
    url: '/node/script/trigger-url',
    method: 'post',
    data: data
  })
}

/**
 * 删除 Script
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function deleteScript(params: any) {
  return axios({
    url: '/node/script/del.json',
    method: 'post',
    data: params
  })
}

// 删除节点脚本模版缓存
export function delAllCache() {
  return axios({
    url: '/node/script/clear_all',
    method: 'get',
    params: {}
  })
}
