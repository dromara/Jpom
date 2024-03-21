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
 * white list data
 * @param {nodeId} nodeId
 */
export function getWhiteList(data) {
  return axios({
    url: '/node/system/white-list',
    method: 'post',
    data: data
  })
}

/**
 * edit white list data
 * @param {
 *  nodeId: 节点 ID,
 *  project: 项目目录,


 * } params
 */
export function editWhiteList(params) {
  return axios({
    url: '/node/system/whitelistDirectory_submit',
    method: 'post',
    data: params
  })
}
