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

// 任务列表
export function fileReleaseTaskLog(params) {
  return axios({
    url: '/file-storage/release-task/list',
    method: 'post',
    data: params
  })
}

// 新增发布任务
export function addReleaseTask(params) {
  return axios({
    url: '/file-storage/release-task/add-task',
    method: 'post',
    data: params
  })
}

// 重新发布任务
export function reReleaseTask(params) {
  return axios({
    url: '/file-storage/release-task/re-task',
    method: 'post',
    data: params
  })
}

// 取消任务
export function cancelReleaseTask(params) {
  return axios({
    url: '/file-storage/release-task/cancel-task',
    method: 'get',
    params: params
  })
}

// 删除任务
export function deleteReleaseTask(params) {
  return axios({
    url: '/file-storage/release-task/delete',
    method: 'get',
    params: params
  })
}

export function getTaskTemplate(params) {
  return axios({
    url: '/file-storage/release-task/get-template',
    method: 'get',
    params: params
  })
}

export function listTaskTemplate(params) {
  return axios({
    url: '/file-storage/release-task/list-template',
    method: 'post',
    data: params
  })
}

export function deleteTaskTemplate(params) {
  return axios({
    url: '/file-storage/release-task/delete-template',
    method: 'get',
    params: params
  })
}

// 任务详情
export function taskDetails(params) {
  return axios({
    url: '/file-storage/release-task/details',
    method: 'get',
    params: params
  })
}

export function taskLogInfoList(params) {
  return axios({
    url: '/file-storage/release-task/log-list',
    method: 'get',
    params: params,
    headers: {
      loading: 'no'
    }
  })
}

export const statusMap = {
  0: t('i18n_a87818b04f'),
  1: t('i18n_fb852fc6cc'),
  2: t('i18n_5ab90c17a3'),
  3: t('i18n_250688d7c9'),
  4: t('i18n_d926e2f58e')
}

export const taskTypeMap = {
  0: 'SSH',
  1: t('i18n_3bf3c0a8d6')
}
