///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { t } from '@/i18n'
import axios from './config'

/************************** */

/**
 * script 服务端中的所有列表
 */
export function getScriptListAll(params) {
  return axios({
    url: '/node/script/list_all',
    method: 'post',
    data: params
  })
}

// 脚本模版日志列表
export function getScriptLogList(params) {
  return axios({
    url: '/node/script_log/list',
    method: 'post',
    data: params
  })
}

// 删除执行记录
export function scriptDel(params) {
  return axios({
    url: '/node/script_log/del',
    method: 'post',
    data: params
  })
}

//执行记录 详情
export function scriptLog(params) {
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
 * params.type: add 表示新增
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.context 内容
 */
export function editScript(params) {
  return axios({
    url: '/node/script/save.json',
    method: 'post',
    data: params
  })
}

export function itemScript(params) {
  return axios({
    url: '/node/script/item.json',
    method: 'get',
    params: params
  })
}

export function syncScript(params) {
  return axios({
    url: '/node/script/sync',
    method: 'get',
    params: params
  })
}
export const triggerExecTypeMap = {
  0: t('i18n_2a3e7f5c38'),
  1: t('i18n_3aed2c11e9'),
  2: t('i18n_4696724ed3')
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data) {
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
export function deleteScript(params) {
  return axios({
    url: '/node/script/del.json',
    method: 'post',
    data: params
  })
}

/**
 * 解绑 Script
 * @param {id} params

 * params.id 编辑修改时判断 ID
 */
export function unbindScript(params) {
  return axios({
    url: '/node/script/unbind.json',
    method: 'get',
    params: params
  })
}
