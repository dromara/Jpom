import { t } from '@/i18n'
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
  0: t('api.user.user-login-log.a1d02c3e'),
  1: t('api.user.user-login-log.e5849544'),
  2: t('api.user.user-login-log.2d6b6d8f'),
  3: t('api.user.user-login-log.d08efdf4'),
  4: t('api.user.user-login-log.227abc8e'),
  5: t('api.user.user-login-log.521c4ec2'),
  6: 'oauth2'
}
