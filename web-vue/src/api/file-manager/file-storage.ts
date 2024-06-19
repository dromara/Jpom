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
import axios from '@/api/config'

// 文件列表
export function fileStorageList(params) {
  return axios({
    url: '/file-storage/list',
    method: 'post',
    data: params
  })
}

export function uploadFile(formData) {
  return axios({
    url: '/file-storage/upload-sharding',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8',
      loading: 'no'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

export function uploadFileMerge(params) {
  return axios({
    url: '/file-storage/upload-sharding-merge',
    method: 'post',
    data: params,
    // 0 表示无超时时间
    timeout: 0
  })
}

// 修改文件
export function fileEdit(params) {
  return axios({
    url: '/file-storage/edit',
    method: 'post',
    data: params
  })
}

// 下载远程文件
export function remoteDownload(params) {
  return axios({
    url: '/file-storage/remote-download',
    method: 'post',
    data: params
  })
}

// 判断文件是否存在
export function hasFile(params) {
  return axios({
    url: '/file-storage/has-file',
    method: 'get',
    params: params
  })
}

export function delFile(params) {
  return axios({
    url: '/file-storage/del',
    method: 'get',
    params: params
  })
}

// 下载 url
export function triggerUrl(params) {
  return axios({
    url: '/file-storage/trigger-url',
    method: 'get',
    params: params
  })
}

export const sourceMap = {
  0: t('i18n_d5a73b0c7f'),
  1: t('i18n_fcba60e773'),
  2: t('i18n_f26ef91424'),
  3: t('i18n_d40b511510')
}

export const statusMap = {
  0: t('i18n_2d455ce5cd'),
  1: t('i18n_50940ed76f'),
  2: t('i18n_af924a1a14')
}
