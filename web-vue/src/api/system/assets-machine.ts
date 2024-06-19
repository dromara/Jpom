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
import axios from '@/api/config'

// 机器 列表
export function machineListData(params) {
  return axios({
    url: '/system/assets/machine/list-data',
    method: 'post',
    data: params
  })
}

export function machineListGroup(params) {
  return axios({
    url: '/system/assets/machine/list-group',
    method: 'get',
    params: params
  })
}

// 编辑机器
export function machineEdit(params) {
  return axios({
    url: '/system/assets/machine/edit',
    method: 'post',
    data: params
  })
}

// 删除机器
export function machineDelete(params) {
  return axios({
    url: '/system/assets/machine/delete',
    method: 'post',
    data: params
  })
}

// 分配机器
export function machineDistribute(params) {
  return axios({
    url: '/system/assets/machine/distribute',
    method: 'post',
    data: params
  })
}

export const statusMap = {
  0: t('i18n_757a730c9e'),
  1: t('i18n_fd6e80f1e0'),
  2: t('i18n_c18455fbe3'),
  3: t('i18n_c5bbaed670'),
  4: t('i18n_a14da34559')
}

// 查看机器关联节点
export function machineListNode(params) {
  return axios({
    url: '/system/assets/machine/list-node',
    method: 'get',
    params: params
  })
}

export function machineListTemplateNode(params) {
  return axios({
    url: '/system/assets/machine/list-template-node',
    method: 'get',
    params: params
  })
}

/**
 * 保存 授权配置
 */
export function saveWhitelist(data) {
  return axios({
    url: '/system/assets/machine/save-whitelist',
    method: 'post',
    data: data
  })
}

/**
 * 保存 节点系统配置
 */
export function saveNodeConfig(data) {
  return axios({
    url: '/system/assets/machine/save-node-config',
    method: 'post',
    data: data
  })
}

export function machineLonelyData(data) {
  return axios({
    url: '/system/assets/machine/lonely-data',
    method: 'get',
    params: data
  })
}

export function machineCorrectLonelyData(data) {
  return axios({
    url: '/system/assets/machine/correct-lonely-data',
    method: 'post',
    data: data
  })
}

export function machineMonitorConfig(data) {
  return axios({
    url: '/system/assets/machine/monitor-config',
    method: 'get',
    params: data
  })
}

export function machineSearch(data) {
  return axios({
    url: '/system/assets/machine/search',
    method: 'get',
    params: data
  })
}
