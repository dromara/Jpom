/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
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

/**
 * 项目文件版本快照列表
 * @param {JSON} params { nodeId, id }
 */
export function versionList(params) {
  return axios({
    url: '/node/manage/file/version-list',
    method: 'post',
    data: params
  })
}

/**
 * 切换到指定版本（快速回滚，不重新传输产物）
 * @param {JSON} params { nodeId, id, versionId, after, snapshotCurrent }
 */
export function versionSwitch(params) {
  return axios({
    url: '/node/manage/file/version-switch',
    method: 'post',
    data: params
  })
}

/**
 * 删除版本
 * @param {JSON} params { nodeId, id, versionId }
 */
export function versionDelete(params) {
  return axios({
    url: '/node/manage/file/version-delete',
    method: 'post',
    data: params
  })
}
