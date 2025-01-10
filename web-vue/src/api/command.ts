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
import axios, { loadRouterBase } from './config'

// 命令列表
export function getCommandList(params) {
  return axios({
    url: '/node/ssh_command/list',
    method: 'post',
    data: params
  })
}

// 编辑命令
export function editCommand(params) {
  return axios({
    url: '/node/ssh_command/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: params
  })
}

// 删除命令
export function deleteCommand(id) {
  return axios({
    url: '/node/ssh_command/del',
    method: 'post',
    data: { id }
  })
}

// 删除命令
export function executeBatch(param) {
  return axios({
    url: '/node/ssh_command/batch',
    method: 'post',
    data: param
  })
}

// 命令日志列表
export function getCommandLogList(params) {
  return axios({
    url: '/node/ssh_command_log/list',
    method: 'post',
    data: params
  })
}

// 命令日志批次列表
export function getCommandLogBarchList(params) {
  return axios({
    url: '/node/ssh_command_log/batch_list',
    method: 'get',
    params: params
  })
}

// 删除命令执行记录
export function deleteCommandLog(id) {
  return axios({
    url: '/node/ssh_command_log/del',
    method: 'post',
    data: { id }
  })
}

// 命令日志信息
export function getCommandLogInfo(params) {
  return axios({
    url: '/node/ssh_command_log/log',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 下载日志
 * @param {*} logId
 */
export function downloadLog(logId) {
  return loadRouterBase('/node/ssh_command_log/download_log', {
    logId: logId
  })
}

export function syncToWorkspace(params) {
  return axios({
    url: '/node/ssh_command/sync-to-workspace',
    method: 'get',
    params: params
  })
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data) {
  return axios({
    url: '/node/ssh_command/trigger-url',
    method: 'post',
    data: data
  })
}

export const statusMap = {
  0: t('i18n_46e3867956'),
  1: t('i18n_ec219f99ee'),
  2: t('i18n_05f6e923af'),
  3: t('i18n_e2f942759e')
}

export const triggerExecTypeMap = {
  0: t('i18n_2a3e7f5c38'),
  1: t('i18n_3aed2c11e9'),
  2: t('i18n_4696724ed3')
}
