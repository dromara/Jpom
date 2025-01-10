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

// 监控列表
export function getMonitorList(params) {
  return axios({
    url: '/monitor/getMonitorList',
    method: 'post',
    data: params
  })
}

/**
 * 编辑监控
 * @param {
 *  id: 监控 ID
 *  name: 监控名称
 *  status: 状态
 *  autoRestart: 是否自动重启
 *  cycle: 监控周期
 *  projects: 监控项目
 *  notifyUser: 报警联系人
 * } params
 */
export function editMonitor(params) {
  return axios({
    url: '/monitor/updateMonitor',
    method: 'post',
    data: params
  })
}

/**
 * 修改监控状态
 * @param {
 *  id: 监控 ID
 *  status: 状态 true | false
 *  type: 状态类型 status | restart
 * } params
 */
export function changeMonitorStatus(params) {
  return axios({
    url: '/monitor/changeStatus',
    method: 'post',
    data: params
  })
}

/**
 * 删除监控
 * @param {*} id
 */
export function deleteMonitor(id) {
  return axios({
    url: '/monitor/deleteMonitor',
    method: 'post',
    data: { id }
  })
}

/**
 * 监控日志
 * @param {
 *  page: 页码
 *  limit: 每页条数
 *  nodeId: 节点 ID
 *  notifyStatus: 通知状态
 * } params
 */
export function getMonitorLogList(params) {
  return axios({
    url: '/monitor/list_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 操作监控日志列表
 */
export function getMonitorOperateLogList() {
  return axios({
    url: '/monitor_user_opt/list_data',
    method: 'post'
  })
}

/**
 * 操作类型列表
 * @returns
 */
export function getMonitorOperateTypeList() {
  return axios({
    url: '/monitor_user_opt/type_data',
    method: 'post'
  })
}

/**
 * 编辑操作监控
 * @param {
 *  id: ID
 *  name: 名称
 *  status: 状态 => on 表示开启
 *  notifyUser: 通知用户 json 字符串
 *  monitorUser： 监控用户 json 字符串
 *  monitorOpt: 监控操作 json 字符串
 * } params
 * @returns
 */
export function editMonitorOperate(params) {
  return axios({
    url: '/monitor_user_opt/update',
    method: 'post',
    data: params
  })
}

/**
 * 删除操作监控
 * @param {*} id
 * @returns
 */
export function deleteMonitorOperate(id) {
  return axios({
    url: '/monitor_user_opt/delete',
    method: 'post',
    data: { id }
  })
}

export const notifyStyle = {
  0: t('i18n_4a0e9142e7'),
  1: t('i18n_3bc5e602b2'),
  2: t('i18n_ff17b9f9cd'),
  3: 'webhook'
}
