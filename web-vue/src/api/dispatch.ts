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
import axios from './config'

// 分发列表
export function getDishPatchList(data, loading) {
  return axios({
    url: '/outgiving/dispatch-list',
    method: 'post',
    data: data,
    headers: {
      loading: loading === false ? 'no' : ''
    }
  })
}

// 分发列表
export function getDishPatchListAll() {
  return axios({
    url: '/outgiving/dispatch-list-all',
    method: 'get'
  })
}

// 分发节点项目状态
export function getDispatchProject(id, loading) {
  return axios({
    url: '/outgiving/getItemData.json',
    method: 'post',
    data: { id },
    timeout: 0,
    headers: {
      loading: loading === false ? 'no' : ''
    }
  })
}

// reqId
export function getReqId() {
  return axios({
    url: '/outgiving/get-reqId',
    method: 'get'
  })
}

/**
 * 编辑分发
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  node_xxx: xxx 表示节点 ID
 *  afterOpt: 发布后操作
 * } params
 */
export function editDispatch(params) {
  return axios({
    url: '/outgiving/save',
    method: 'post',
    data: params
  })
}

/**
 * 编辑分发项目
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  afterOpt: 发布后操作
 *  runMode: 运行方式
 *  mainClass: 启动类
 *  javaExtDirsCp: 目录地址
 *  whitelistDirectory: 授权地址
 *  lib: lib
 *  add_xxx: xxx 表示新增的节点信息

 * } params
 */
export function editDispatchProject(params) {
  return axios({
    url: '/outgiving/save_project',
    method: 'post',
    data: params
  })
}

/**
 * 上传分发文件
 * @param {
 *  id: 分发 ID
 *  afterOpt: 发布后操作
 *  clearOld: 是否清除
 *  file: 文件 multipart/form-data
 * } formData
 */
export function uploadDispatchFile(formData) {
  return axios({
    url: '/outgiving/upload-sharding',
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

export function uploadDispatchFileMerge(params) {
  return axios({
    url: '/outgiving/upload-sharding-merge',
    method: 'post',
    data: params,
    // 0 表示无超时时间
    timeout: 0
  })
}

/**
 * 远程下载文件分发
 * @param {
 *  id: 节点 ID
 *  clearOld:  是否清空
 *  afterOpt: 分发后的操作
 *  url: 远程URL
 *   unzip：是否为压缩包
 * } params
 */
export function remoteDownload(params) {
  return axios({
    url: '/outgiving/remote_download',
    method: 'post',
    data: params
  })
}

export function useBuild(params) {
  return axios({
    url: '/outgiving/use-build',
    method: 'post',
    data: params
  })
}

export function useuseFileStorage(params) {
  return axios({
    url: '/outgiving/use-file-storage',
    method: 'post',
    data: params
  })
}

export function useuseStaticFileStorage(params) {
  return axios({
    url: '/outgiving/use-static-file-storage',
    method: 'post',
    data: params
  })
}

/**
 * 释放分发
 * @param {*} id 分发 ID
 */
export function releaseDelDisPatch(id) {
  return axios({
    url: '/outgiving/release_del.json',
    method: 'post',
    data: { id }
  })
}

/**
 * 删除分发
 * @param {*} id 分发 ID
 */
export function delDisPatchProject(data) {
  return axios({
    url: '/outgiving/delete_project',
    method: 'post',
    data: data
  })
}

/**
 * 解绑分发
 * @param {*} id 分发 ID
 */
export function unbindOutgiving(id) {
  return axios({
    url: '/outgiving/unbind.json',
    method: 'get',
    params: { id }
  })
}

/**
 * 分发日志
 * @param {
 *  nodeId: 节点 ID
 *  outGivingId: 分发 ID
 *  status: 分发状态
 *  time: 时间区间 2021-01-04 00:00:00 ~ 2021-01-12 00:00:00
 * } params
 */
export function getDishPatchLogList(params) {
  return axios({
    url: '/outgiving/log_list_data.json',
    method: 'post',
    data: params
  })
}

// 获取分发授权数据
export function getDispatchWhiteList(params) {
  return axios({
    url: '/outgiving/white-list',
    method: 'post',
    data: params
  })
}

/**
 * 编辑分发授权
 * @param {*} params
 */
export function editDispatchWhiteList(params) {
  return axios({
    url: '/outgiving/whitelistDirectory_submit',
    method: 'post',
    data: params
  })
}

/**
 * 取消分发
 * @param {*} id 分发 ID
 */
export function cancelOutgiving(data) {
  return axios({
    url: '/outgiving/cancel',
    method: 'post',
    data
  })
}

export function removeProject(params) {
  return axios({
    url: '/outgiving/remove-project',
    method: 'get',
    params: params
  })
}

export function saveDispatchProjectConfig(data) {
  return axios({
    url: '/outgiving/config-project',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

export const afterOptList = [
  { title: t('i18n_a2ebd000e4'), value: 0 },
  { title: t('i18n_82915930eb'), value: 1 },
  { title: t('i18n_0e1ecdae4a'), value: 2 },
  { title: t('i18n_8887e94cb7'), value: 3 }
]

export const afterOptListSimple = [
  { title: t('i18n_a2ebd000e4'), value: 0 },
  { title: t('i18n_913ef5d129'), value: 1 }
]

export const dispatchStatusMap = {
  0: t('i18n_29efa328e5'),
  1: t('i18n_5b3ffc2910'),
  2: t('i18n_7e300e89b1'),
  3: t('i18n_2a049f4f5b'),
  4: t('i18n_036c0dc2aa'),
  5: t('i18n_339097ba2e'),
  6: t('i18n_7bf62f7284')
}

export const statusMap = {
  0: t('i18n_29efa328e5'),
  1: t('i18n_5b3ffc2910'),
  2: t('i18n_3ea6c5e8ec'),
  3: t('i18n_30e855a053'),
  4: t('i18n_2a049f4f5b')
}

export const dispatchMode = {
  upload: t('i18n_bd7c8c96bc'),
  download: t('i18n_bd7043cae3'),
  'build-trigger': t('i18n_74d5f61b9f'),
  'use-build': t('i18n_c1af35d001'),
  'static-file-storage': t('i18n_28f6e7a67b'),
  'file-storage': t('i18n_26183c99bf')
}
