///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from 'axios'

const external = axios.create({
  timeout: 5 * 1000,
  headers: {}
})

// 响应拦截器
external.interceptors.response.use(
  async (response) => {
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

export function executionRequest(url: any, param: any) {
  return external({
    url: url,
    method: 'get',
    params: param
  })
}
