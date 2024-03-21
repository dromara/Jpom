///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from './config'
import { loadRouterBase } from './config'

/**
 * 项目列表
 * @param {JSON} params {
 *  nodeId: 节点 ID,
 *  id: 项目ID
 * }
 */
export function listBackup(params) {
  return axios({
    url: '/node/manage/file/list-backup',
    method: 'post',
    data: params
  })
}

export function backupFileList(params) {
  return axios({
    url: '/node/manage/file/backup-item-files',
    method: 'post',
    headers: {
      loading: 'no'
    },
    data: params
  })
}

/**
 * 下载项目文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 * } params
 */
export function backupDownloadProjectFile(params) {
  return loadRouterBase('/node/manage/file/backup-download', params)
}

/**
 * 删除文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupDeleteProjectFile(params) {
  return axios({
    url: '/node/manage/file/backup-delete',
    method: 'post',
    data: params
  })
}

/**
 * 还原文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupRecoverProjectFile(params) {
  return axios({
    url: '/node/manage/file/backup-recover',
    method: 'post',
    data: params
  })
}
