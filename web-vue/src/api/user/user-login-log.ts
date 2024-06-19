///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///
import { t } from '@/i18n'
import axios from '../config'

export function userLoginLgin(params) {
  return axios({
    url: '/user/login-log/list-data',
    method: 'post',
    data: params
  })
}

export const operateCodeMap = {
  0: t('i18n_dd95bf2d45'),
  1: t('i18n_5a5368cf9b'),
  2: t('i18n_18d49918f5'),
  3: t('i18n_a093ae6a6e'),
  4: t('i18n_8b63640eee'),
  5: t('i18n_bb9a581f48'),
  6: 'oauth2'
}
