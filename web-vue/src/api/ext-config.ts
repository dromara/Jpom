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
 * 获取基础运行镜像列表
 * @returns {*}
 */
export function listExtConf() {
  return axios({
    url: '/system/ext-conf/list',
    method: 'get'
  })
}

export function getItem(params) {
  return axios({
    url: '/system/ext-conf/get-item',
    method: 'get',
    params: params
  })
}

export function getDefaultItem(params) {
  return axios({
    url: '/system/ext-conf/get-default-item',
    method: 'get',
    params: params
  })
}

export function saveItem(params) {
  return axios({
    url: '/system/ext-conf/save-item',
    method: 'post',
    data: params
  })
}

export function addItem(params) {
  return axios({
    url: '/system/ext-conf/add-item',
    method: 'get',
    params: params
  })
}
