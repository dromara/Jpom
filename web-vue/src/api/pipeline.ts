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
 * 流水线分组（虚拟逻辑分组：返回当前工作空间已存在的分组名）
 * <p>
 * 与在线构建 /build/list_group_all 同一套语义，供编辑页定制下拉框作为候选项。
 * </p>
 */
export function getPipelineGroupAll() {
  return axios({
    url: '/pipeline/list_group_all',
    method: 'get'
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
 * 流水线触发器地址
 * <p>
 * 响应 data：{ triggerUrl, statusUrl, id, token }；triggerUrl 为相对地址，由前端拼绝对地址。
 * 传 rest='rest' 表示重置（服务端重新生成 token，旧地址立即失效）。
 * </p>
 */
export function getTriggerUrl(params) {
  return axios({
    url: '/pipeline/trigger-url',
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
 * 事件通知（Hooks）的事件码：与服务端固定 10 个码值一一对应，顺序即展示顺序
 * <p>
 * 服务端另有 POST /pipeline/hook-events 返回同样的 [{code, desc}]，这里用固定常量便于离线渲染，
 * 码值必须与后端完全一致（run_finished 总是最后触发）。
 * </p>
 */
export const pipelineHookEventCodes = [
  // 「全部事件」：命中当前与以后新增的任何事件（后端 PipelineEvent.ALL 同义），
  // 放在第一位，避免与具体事件逐条选择
  'all',
  'run_start',
  'run_success',
  'run_failed',
  'run_cancelled',
  'run_finished',
  'stage_start',
  'stage_success',
  'stage_failed',
  'stage_skipped',
  'stage_cancelled'
]

/**
 * 事件码 → i18n key（四个语言包必须都有对应文案）
 */
export function pipelineHookEventLabelKey(code) {
  return 'i18n_pipeline_hook_event_' + code
}

/**
 * 运行状态码 → 文案/颜色：与在线构建共用同一份状态字典（不另写一套中文）
 */
export const pipelineStatusMap = statusMap
export const pipelineStatusColor = statusColor
