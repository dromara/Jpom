import axios from './config';

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
 * @param tableNameList 需要备份的表名称列表，没有默认表示全量备份
 */
export function createBackup(tableNameList) {
  const data = {
    tableNameList,
  }
  return axios({
    url: '/system/backup/create',
    method: 'post',
    headers: {
      'Content-Type': 'application/json',
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
    data: {id}
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
    data: {id}
  })
}

export const backupTypeMap = {
  0: '全量备份',
  1: '部分备份'
}
