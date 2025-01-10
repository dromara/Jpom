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
import { loadRouterBase } from './config'

/**
 * 构建列表
 * @param {
 *  group: 分组名称
 * } params
 */
export function getBuildList(params, loading) {
  return axios({
    url: '/build/list',
    method: 'post',
    data: params,
    headers: {
      loading: loading === false ? 'no' : ''
    }
  })
}

/**
 * 构建详情
 * @param {
 *
 * } params
 */
export function getBuildGet(params) {
  return axios({
    url: '/build/get',
    method: 'get',
    params
  })
}

/**
 * 构建环境变量
 * @param {
 *
 * } params
 */
export function getBuildEnvironment(params) {
  return axios({
    url: '/build/manage/environment',
    method: 'post',
    data: params
  })
}

/**
 * 构建分组
 */
export function getBuildGroupAll() {
  return axios({
    url: '/build/list_group_all',
    method: 'get'
  })
}

/**
 * 获取仓库分支信息
 * @param {
 *  repositoryId: 仓库id
 * } params
 */
export function getBranchList(params) {
  return axios({
    url: '/build/branch-list',
    method: 'post',
    timeout: 0,
    data: params,
    headers: {}
  })
}

/**
 * 编辑构建信息
 * @param {
 *  id: 构建 ID
 *  name: 构建名称
 *  group: 分组名称
 *  branchName: 分支名称
 *  branchTagName: 标签
 *  script: 构建命令
 *  resultDirFile: 构建产物目录
 *  releaseMethod: 发布方法
 *  extraData: 额外信息 JSON 字符串
 *  repostitoryId: 仓库信息
 * } params
 */
export function editBuild(params) {
  const data = {
    id: params.id,
    name: params.name,
    repositoryId: params.repositoryId,
    resultDirFile: params.resultDirFile,
    script: params.script,
    releaseMethod: params.releaseMethod,
    branchName: params.branchName,
    branchTagName: params.branchTagName,
    group: params.group,
    repoType: params.repoType,
    // 其他参数
    extraData: params.extraData,
    webhook: params.webhook,
    autoBuildCron: params.autoBuildCron,
    buildMode: params.buildMode,
    aliasCode: params.aliasCode,
    resultKeepDay: params.resultKeepDay,
    buildEnvParameter: params.buildEnvParameter
  }
  return axios({
    url: '/build/edit',
    method: 'post',
    data
  })
}

/**
 * 删除构建信息
 * @param {*} id
 */
export function deleteBuild(id) {
  return axios({
    url: '/build/delete',
    method: 'post',
    data: { id }
  })
}

export function deleteatchBuild(data) {
  return axios({
    url: '/build/batch-delete',
    method: 'post',
    data: data
  })
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data) {
  return axios({
    url: '/build/trigger/url',
    method: 'post',
    data: data
  })
}

// /**
//  * 重置触发器
//  * @param {*} id
//  */
// export function resetTrigger(id) {
//   return axios({
//     url: "/build/trigger/rest",
//     method: "post",
//     data: { id },
//   });
// }

/**
 * 清理构建
 * @param {*} id
 */
export function clearBuid(id) {
  return axios({
    url: '/build/clean-source',
    method: 'post',
    data: { id }
  })
}

/**
 * 查看构建日志
 * @param {JSON} params {
 *  id: 构建 ID
 *  buildId: 构建任务 ID
 *  line: 需要获取的行号 1 开始
 * }
 */
export function loadBuildLog(params) {
  return axios({
    url: '/build/manage/get-now-log',
    method: 'post',
    data: params,
    headers: {
      tip: 'no',
      loading: 'no'
    }
  })
}

/**
 * 开始构建
 * @param {*} id
 */
export function startBuild(data) {
  return axios({
    url: '/build/manage/start',
    method: 'post',
    data: data
  })
}

/**
 * 停止构建
 * @param {*} id
 */
export function stopBuild(id) {
  return axios({
    url: '/build/manage/cancel',
    method: 'post',
    data: { id }
  })
}

/**
 * 构建历史
 * @param {
 *  buildDataId: 构建任务 ID
 *  status: 状态
 * } params
 */
export function geteBuildHistory(params) {
  return axios({
    url: '/build/history/history_list.json',
    method: 'post',
    data: params
  })
}

/**
 * 下载构建日志
 * @param {*} logId
 */
export function downloadBuildLog(logId) {
  return loadRouterBase('/build/history/download_log', {
    logId: logId
  })
}

/**
 * 下载构建产物
 * @param {*} logId
 */
export function downloadBuildFile(logId) {
  return loadRouterBase('/build/history/download_file', {
    logId: logId
  })
}

/**
 * 下载构建产物
 * @param {*} logId
 */
export function downloadBuildFileByBuild(id, numberId) {
  return loadRouterBase('/build/history/download_file_by_build', {
    buildId: id,
    buildNumberId: numberId
  })
}

/**
 * 回滚（重新发布）
 * @param {*} logId
 * @returns
 */
export function rollback(logId) {
  return axios({
    url: '/build/manage/reRelease',
    method: 'post',
    data: { logId }
  })
}

/**
 * 删除构建历史记录
 * @param {*} logId
 */
export function deleteBuildHistory(logId) {
  return axios({
    url: '/build/history/delete_log.json',
    method: 'post',
    data: { logId }
  })
}

export function sortItem(params) {
  return axios({
    url: '/build/sort-item',
    method: 'get',
    params: params
  })
}

export const statusMap = {
  1: t('i18n_32493aeef9'),
  2: t('i18n_641796b655'),
  3: t('i18n_41298f56a3'),
  4: t('i18n_0baa0e3fc4'),
  5: t('i18n_2fff079bc7'),
  6: t('i18n_250688d7c9'),
  7: t('i18n_b4fc1ac02c'),
  8: t('i18n_979b7d10b0'),
  9: t('i18n_81afd9e713'),
  10: t('i18n_8160b4be4e')
}
export const statusColor = {
  1: 'orange',
  2: 'green',
  3: 'red',
  4: 'orange',
  5: 'green',
  6: 'red',
  7: '',
  8: 'blue',
  9: 'orange',
  10: 'red'
}

export const releaseMethodMap = {
  0: t('i18n_a189314b9e'),
  1: t('i18n_ae6838c0e6'),
  2: t('i18n_31ecc0e65b'),
  3: 'SSH',
  4: t('i18n_b71a7e6aab'),
  5: t('i18n_9136e1859a')
}

export const triggerBuildTypeMap = {
  0: t('i18n_2a3e7f5c38'),
  1: t('i18n_4696724ed3'),
  2: t('i18n_72ebfe28b0'),
  3: t('i18n_31070fd376')
}

export const buildModeMap = {
  0: t('i18n_69c3b873c1'),
  1: t('i18n_685e5de706')
}
