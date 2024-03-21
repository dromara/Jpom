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

/*
 * 集群列表
 * @param {*}
 * } params
 */
export function getClusterList(params) {
  return axios({
    url: '/cluster/list',
    method: 'post',
    data: params
  })
}

/*
 * 删除集群
 * @param {String} id
 * } params
 */
export function deleteCluster(id) {
  return axios({
    url: '/cluster/delete',
    method: 'get',
    params: { id: id }
  })
}

/*
 * 删除所有可用分组
 * @param {} *
 * } params
 */
export function listLinkGroups(params) {
  return axios({
    url: '/cluster/list-link-groups',
    method: 'get',
    params: params
  })
}

export function listClusterAll(params) {
  return axios({
    url: '/cluster/list-all',
    method: 'get',
    params: params
  })
}

export function editCluster(params) {
  return axios({
    url: '/cluster/edit',
    method: 'post',
    data: params
  })
}
