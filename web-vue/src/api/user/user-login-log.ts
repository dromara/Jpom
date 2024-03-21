///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from '../config'

export function userLoginLgin(params) {
  return axios({
    url: '/user/login-log/list-data',
    method: 'post',
    data: params
  })
}

export const operateCodeMap = {
  0: '正常登录',
  1: '密码错误',
  2: '账号被锁定',
  3: '自动续期',
  4: '账号被禁用',
  5: '登录成功,需要验证 MFA',
  6: 'oauth2'
}
