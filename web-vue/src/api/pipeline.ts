/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

import axios, { loadRouterBase } from './config'
import { statusMap, statusColor } from './build-info'

/**
 * 流水线列表（分页，与在线构建列表同一套参数：page/limit/%name%/status/order_field）
 */
export function getPipelineList(params) {
  return axios({
    url: '/pipeline/list',
    method: 'post',
    data: params
  })
}

/**
 * 流水线详情
 */
export function getPipelineGet(params) {
  return axios({
    url: '/pipeline/get',
    method: 'post',
    data: params
  })
}

/**
 * 保存流水线
 */
export function savePipeline(params) {
  return axios({
    url: '/pipeline/save',
    method: 'post',
    data: params
  })
}

/**
 * 删除流水线
 */
export function deletePipeline(params) {
  return axios({
    url: '/pipeline/delete',
    method: 'post',
    data: params
  })
}

/**
 * 运行流水线
 */
export function runPipeline(params) {
  return axios({
    url: '/pipeline/run',
    method: 'post',
    data: params
  })
}

/**
 * 取消运行
 */
export function cancelPipeline(params) {
  return axios({
    url: '/pipeline/cancel',
    method: 'post',
    data: params
  })
}

/**
 * 运行记录（分页）
 */
export function getPipelineRuns(params) {
  return axios({
    url: '/pipeline/run/list',
    method: 'post',
    data: params
  })
}

/**
 * 运行日志（增量：从 line 行开始取，与在线构建 get-now-log 同一套读取器）
 */
export function getPipelineRunLog(params) {
  return axios({
    url: '/pipeline/run/log',
    method: 'post',
    data: params
  })
}

/**
 * 下载运行日志
 */
export function downloadPipelineRunLog(runId) {
  return loadRouterBase('/pipeline/run/download-log', {
    runId: runId
  })
}

/**
 * 运行状态码 → 文案/颜色：与在线构建共用同一份状态字典（不另写一套中文）
 */
export const pipelineStatusMap = statusMap
export const pipelineStatusColor = statusColor
