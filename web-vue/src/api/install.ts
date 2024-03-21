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

// 检查是否需要初始化系统
export function checkSystem() {
  return axios({
    url: '/check-system',
    method: 'post',
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 初始化系统
 * @param {
 *  userName: 登录名
 *  userPwd: 初始密码
 * } params
 */
export function initInstall(params) {
  return axios({
    url: '/install_submit.json',
    method: 'post',
    data: params
  })
}

export function loadingLogo() {
  return axios({
    url: '/logo-image',
    method: 'get',
    headers: {
      loading: 'no'
    }
  })
}
