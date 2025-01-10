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
import { loadRouterBase } from './config'

/**
 * 备份列表
 * @param {
 *  name: 备份名称
 *  backupType: 备份类型{0: 全量, 1: 部分}
 * } params
 */
export function getBackupList(params) {
  return axios({
    url: '/system/backup/list',
    method: 'post',
    data: params
  })
}

/**
 * 获取数据库表名列表
 */
export function getTableNameList() {
  return axios({
    url: '/system/backup/table-name-list',
    method: 'post'
  })
}

/**
 * 创建备份信息
 * @param tableNameList 需要备份的表名称列表，没有默认表示api.backup-info.945bddc
 */
export function createBackup(tableNameList) {
  const data = {
    tableNameList
  }
  return axios({
    url: '/system/backup/create',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data
  })
}

/**
 * 删除备份信息
 * @param {*} id
 */
export function deleteBackup(id) {
  return axios({
    url: '/system/backup/delete',
    method: 'post',
    data: { id }
  })
}

/**
 * 还原备份信息
 * @param {*} id
 * @returns
 */
export function restoreBackup(id) {
  return axios({
    url: '/system/backup/restore',
    method: 'post',
    timeout: 0,
    data: { id }
  })
}

/**
 * 下载备份文件
 * @param {*} id
 * @returns
 */
export function downloadBackupFile(id) {
  return loadRouterBase('/system/backup/download', {
    id: id
  })
}

/**
 * 上传 SQL 备份文件
 * @param {
 *  file: 文件 multipart/form-data
 *  bakcupType: 0 全量备份 1 部分备份
 * } formData
 */
export function uploadBackupFile(formData) {
  return axios({
    url: '/system/backup/upload',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

export function getTriggerUrl(params) {
  return axios({
    url: '/system/backup/trigger-url',
    method: 'post',
    params: params
  })
}

export const backupTypeArray = [
  { key: 0, value: t('i18n_6d68bd5458'), disabled: false },
  { key: 1, value: t('i18n_67b667bf98'), disabled: false },
  { key: 2, value: t('i18n_90c0458a4c'), disabled: true },
  { key: 3, value: t('i18n_590e5b46a0'), disabled: true },
  { key: 4, value: t('i18n_4696724ed3'), disabled: true }
]

export const arrayToMap = (arra) => {
  const obj = {}
  arra.forEach((value) => {
    obj[value.key] = value.value
  })
  return obj
}

export const backupTypeMap = arrayToMap(backupTypeArray)

export const backupStatusMap = {
  0: t('i18n_5d459d550a'),
  1: t('i18n_3ba621d736'),
  2: t('i18n_1012e09849')
}
