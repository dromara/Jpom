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

// 权限组列表
export function getList(params) {
  return axios({
    url: '/user-permission-group/get-list',
    method: 'post',
    data: params
  })
}

// 编辑
export function editPermissionGroup(params) {
  return axios({
    url: '/user-permission-group/edit',
    method: 'post',
    data: params
  })
}

// 所有列表
export function getUserPermissionListAll() {
  return axios({
    url: '/user-permission-group/get-list-all',
    method: 'get'
  })
}

// 删除
export function deletePermissionGroup(id) {
  return axios({
    url: '/user-permission-group/delete',
    method: 'get',
    params: { id }
  })
}
