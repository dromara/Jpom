///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from '@/api/config'

export function staticFileStorageList(params) {
  return axios({
    url: '/file-storage/static/list',
    method: 'post',
    data: params
  })
}

export function delFile(params) {
  return axios({
    url: '/file-storage/static/del',
    method: 'get',
    params: params
  })
}

// 下载 url
export function triggerUrl(params) {
  return axios({
    url: '/file-storage/static/trigger-url',
    method: 'get',
    params: params
  })
}

// 修改文件
export function fileEdit(params) {
  return axios({
    url: '/file-storage/static/edit',
    method: 'post',
    data: params
  })
}

export function hasStaticFile(params) {
  return axios({
    url: '/file-storage/static/has-file',
    method: 'get',
    params: params
  })
}

export function staticScanner(params) {
  return axios({
    url: '/file-storage/static/scanner',
    method: 'get',
    params: params
  })
}
