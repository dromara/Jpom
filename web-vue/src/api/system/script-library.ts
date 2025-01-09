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

export function getScriptLibraryList(params: any) {
  return axios({
    url: '/system/assets/script-library/list-data',
    method: 'post',
    data: params
  })
}

export function getScriptLibraryNoPermissionList(params: any) {
  return axios({
    url: '/system/assets/script-library/list-data-no-permission',
    method: 'post',
    data: params
  })
}

export function editScriptLibrary(params: any) {
  return axios({
    url: '/system/assets/script-library/edit',
    method: 'post',
    data: params
  })
}

export function delScriptLibrary(params: any) {
  return axios({
    url: '/system/assets/script-library/del',
    method: 'post',
    data: params
  })
}
