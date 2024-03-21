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
import { loadRouterBase } from '@/api/config'
// 导入证书
export function certificateImportFile(formData) {
  return axios({
    url: '/certificate/import-file',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',

    data: formData
  })
}

/**
 * cert 列表
 */
export function certList(params) {
  return axios({
    url: '/certificate/list',
    method: 'post',
    data: params
  })
}

/**
 * cert 列表
 */
export function certListAll(params) {
  return axios({
    url: '/certificate/list-all',
    method: 'post',
    data: params
  })
}

/**
 * 删除 cert
 * @param {
 *
 * } params
 */
export function deleteCert(params) {
  return axios({
    url: '/certificate/del',
    method: 'get',
    params: params
  })
}

/**
 * 导出 cert
 * @param {
 *
 * } params
 */
export function downloadCert(params) {
  return loadRouterBase('/certificate/export', params)
}

// 修改证书
export function certificateEdit(params) {
  return axios({
    url: '/certificate/edit',
    method: 'post',
    data: params
  })
}

// 部署证书
export function certificateDeploy(params) {
  return axios({
    url: '/certificate/deploy',
    method: 'post',
    data: params
  })
}
