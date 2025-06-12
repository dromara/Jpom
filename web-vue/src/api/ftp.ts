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

// ftp 列表
export function getFtpList(params) {
  return axios({
    url: '/node/ftp/list_data.json',
    method: 'post',
    data: params
  })
}

// ftp group all
export function getFtpGroupAll() {
  return axios({
    url: '/node/ftp/list-group-all',
    method: 'get'
  })
}

// 根据 nodeId 查询列表
export function getFtpListAll() {
  return axios({
    url: '/node/ftp/list_data_all.json',
    method: 'get'
  })
}

/**
 * 编辑 FTP
 * @param {*} params
 * params.type = {'add': 表示新增, 'edit': 表示修改}
 */
export function editFtp(params) {
  return axios({
    url: '/node/ftp/save.json',
    method: 'post',

    params
  })
}

// 删除 FTP
export function deleteFtp(id) {
  return axios({
    url: '/node/ftp/del.json',
    method: 'post',
    data: {id}
  })
}

// 删除 FTP
export function deleteForeFtp(id) {
  return axios({
    url: '/node/ftp/del-fore',
    method: 'post',
    data: {id}
  })
}

export function syncToWorkspace(params) {
  return axios({
    url: '/node/ftp/sync-to-workspace',
    method: 'get',
    params: params
  })
}
