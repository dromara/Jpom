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
import { loadRouterBase } from '@/api/config'

// ssh 列表
export function machineSshListData(params) {
  return axios({
    url: '/system/assets/ssh/list-data',
    method: 'post',
    data: params
  })
}

// 搜索 ssh 列表
export function machineSshSearchData(params) {
  return axios({
    url: '/system/assets/ssh/search',
    method: 'post',
    data: params
  })
}

export function machineSshListGroup(params) {
  return axios({
    url: '/system/assets/ssh/list-group',
    method: 'get',
    params: params
  })
}

// 编辑ssh
export function machineSshEdit(params) {
  return axios({
    url: '/system/assets/ssh/edit',
    method: 'post',
    data: params
  })
}

// 删除 ssh
export function machineSshDelete(params) {
  return axios({
    url: '/system/assets/ssh/delete',
    method: 'post',
    data: params
  })
}

// 分配 ssh
export function machineSshDistribute(params) {
  return axios({
    url: '/system/assets/ssh/distribute',
    method: 'post',
    data: params
  })
}

// ssh 操作日志列表
export function getMachineSshOperationLogList(params) {
  return axios({
    url: '/system/assets/ssh/log-list-data',
    method: 'post',
    data: params
  })
}

// ssh 关联工作空间的数据
export function machineListGroupWorkspaceSsh(params) {
  return axios({
    url: '/system/assets/ssh/list-workspace-ssh',
    method: 'get',
    params: params
  })
}

export function machineSshSaveWorkspaceConfig(params) {
  return axios({
    url: '/system/assets/ssh/save-workspace-config',
    method: 'post',
    data: params
  })
}

/**
 * restHideField by id
 * @param {String} id
 * @returns
 */
export function restHideField(id) {
  return axios({
    url: '/system/assets/ssh/rest-hide-field',
    method: 'post',
    data: { id }
  })
}
/*
 * 下载导入模板
 *
 */
export function importTemplate(data) {
  return loadRouterBase('/system/assets/ssh/import-template', data)
}

/*
 * 导出数据
 *
 */
export function exportData(data) {
  return loadRouterBase('/system/assets/ssh/export-data', data)
}
// 导入数据
export function importData(formData) {
  return axios({
    url: '/system/assets/ssh/import-data',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

export const statusMap = {
  0: { desc: t('i18n_757a730c9e'), color: 'red' },
  1: { desc: t('i18n_fd6e80f1e0'), color: 'green' },
  2: { desc: t('i18n_46158d0d6e'), color: 'orange' }
}
