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

export function triggerTokenList(data) {
  return axios({
    url: '/system/trigger/list',
    method: 'post',
    data: data
  })
}

export function triggerTokenAllType(data) {
  return axios({
    url: '/system/trigger/all-type',
    method: 'get',
    params: data
  })
}

export function triggerTokenDelete(data) {
  return axios({
    url: '/system/trigger/delete',
    method: 'get',
    params: data
  })
}
