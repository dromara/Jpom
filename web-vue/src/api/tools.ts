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
 *
 * @param data
 */
export function cronTools(data) {
  return axios({
    url: '/tools/cron',
    method: 'get',
    params: data
  })
}

export function ipList(data) {
  return axios({
    url: '/tools/ip-list',
    method: 'get',
    params: data
  })
}

export function netPing(data) {
  return axios({
    url: '/tools/net-ping',
    method: 'get',
    params: data
  })
}

export function netTelnet(data) {
  return axios({
    url: '/tools/net-telnet',
    method: 'get',
    params: data
  })
}
