/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * SSH 项目 api
 *
 * 接口约定（全部 POST，统一返回 { code: 200, data: ... }）：
 * - /node/ssh-project/list           分页列表
 * - /node/ssh-project/item           单条详情
 * - /node/ssh-project/save           保存（新增/编辑）
 * - /node/ssh-project/delete         删除
 * - /node/ssh-project/template-list  模板列表
 * - /node/ssh-project/template-render 按模板参数渲染 DSL 内容
 */
import { IResponse } from '@/interface/request'
import { NO_NOTIFY_KEY } from '@/utils/const'
import axios from './config'

/**
 * SSH 项目
 */
export interface SshProjectModel {
  id: string
  name: string
  group: string
  workspaceId: string
  /**
   * 资产机器 ssh id
   */
  machineSshId: string
  /**
   * 工作空间 ssh id
   */
  sshId: string
  /**
   * 项目路径
   */
  path: string
  /**
   * DSL（YAML）内容
   */
  dslContent: string
  templateKey: string
  logCharset: string
  autoStart: boolean
  token: string
  sortValue: number
  /**
   * 最近一次状态：running | stopped
   */
  lastStatus: string
  lastPid: number
  lastPort: string
  lastStatusMsg: string
  lastRefreshTime: number
  modifyTime: number
  modifyUser: string
}

/**
 * 列表查询条件
 *
 * 分页沿用项目内通用约定（page/limit）；同时兼容 current/pageSize 写法。
 * 名称/分组按 Jpom 通用过滤协议转换为 %field%（后端 listPage 会解析为 like 查询）。
 */
export interface SshProjectListQuery {
  page?: number
  limit?: number
  current?: number
  pageSize?: number
  name?: string
  group?: string
}

/**
 * 分页返回结构
 */
export interface SshProjectPageResult {
  result: SshProjectModel[]
  total: number
  page: number
  pageSize: number
  totalPage?: number
}

/**
 * 模板字段
 */
export interface SshProjectTemplateField {
  key: string
  label: string
  /**
   * text | number | select | boolean
   */
  type: string
  defaultValue?: string
  required?: boolean
  placeholder?: string
  /**
   * select 类型的候选项（后端契约暂未声明，存在时按选项渲染）
   */
  options?: Array<string | { label: string; value: string }>
}

/**
 * 模板日志文件
 */
export interface SshProjectTemplateLogFile {
  name: string
  path: string
}

/**
 * 模板
 */
export interface SshProjectTemplate {
  key: string
  name: string
  description?: string
  order?: number
  fields: SshProjectTemplateField[]
  logFiles?: SshProjectTemplateLogFile[]
}

/**
 * 保存参数
 */
export interface SshProjectSaveParams {
  id?: string
  name: string
  group?: string
  machineSshId: string
  sshId: string
  path: string
  dslContent: string
  templateKey: string
  logCharset?: string
  autoStart?: boolean
  token?: string
}

/**
 * 模板渲染结果
 */
export interface SshProjectRenderResult {
  dslContent: string
}

/**
 * 可用操作
 */
export type SshProjectOperateOpt = 'start' | 'stop' | 'restart' | 'reload'

/**
 * 操作执行结果
 */
export interface SshProjectOperateResult {
  success: boolean
  exitCode: number
  message: string
  output: string
  cost: number
  /**
   * 执行后的状态：running | stopped
   */
  status: string
  pid: number
  port: string
}

/**
 * 单个项目的实时状态
 */
export interface SshProjectStatusItem {
  status: string
  pid: number
  port: string
  message: string
}

/**
 * 实时状态集合：key 为项目 id
 */
export interface SshProjectStatusMap {
  [projectId: string]: SshProjectStatusItem
}

/**
 * 可查看的日志文件（控制台日志 + dsl config.log）
 */
export interface SshProjectLogItem {
  name: string
  label: string
  path?: string
}

/**
 * 日志备份文件
 */
export interface SshProjectLogBackup {
  name: string
}

/**
 * 统一分页参数（page/limit 为项目通用约定，current/pageSize 为契约别名）
 */
export function toSshProjectPageQuery(params: SshProjectListQuery = {}): Record<string, string | number> {
  const data: Record<string, string | number> = {
    page: params.page || params.current || 1,
    limit: params.limit || params.pageSize || 10
  }
  if (params.name) {
    data['%name%'] = params.name
  }
  if (params.group) {
    data['%group%'] = params.group
  }
  return data
}

/**
 * SSH 项目列表
 */
export function getSshProjectList(params: SshProjectListQuery = {}): Promise<IResponse<SshProjectPageResult>> {
  return axios({
    url: '/node/ssh-project/list',
    method: 'post',
    data: toSshProjectPageQuery(params)
  })
}

/**
 * 获取单个 SSH 项目
 */
export function getSshProjectItem(id: string): Promise<IResponse<SshProjectModel>> {
  return axios({
    url: '/node/ssh-project/item',
    method: 'post',
    data: { id }
  })
}

/**
 * 保存 SSH 项目（新增/编辑）
 */
export function saveSshProject(params: SshProjectSaveParams): Promise<IResponse<string>> {
  return axios({
    url: '/node/ssh-project/save',
    method: 'post',
    data: params
  })
}

/**
 * 删除 SSH 项目
 */
export function deleteSshProject(id: string): Promise<IResponse<boolean>> {
  return axios({
    url: '/node/ssh-project/delete',
    method: 'post',
    data: { id }
  })
}

/**
 * 启动/停止/重启/重载
 * @param params id 项目 id；opt 操作
 */
export function operateSshProject(params: {
  id: string
  opt: SshProjectOperateOpt
}): Promise<IResponse<SshProjectOperateResult>> {
  return axios({
    url: '/node/ssh-project/operate',
    method: 'post',
    data: params
  })
}

/**
 * 实时状态，单条（id）或批量（ids）
 * @param params id 或 ids（数组，内部转为 JSON 字符串）
 */
export function getSshProjectStatus(params: {
  id?: string
  ids?: string[] | string
}): Promise<IResponse<SshProjectStatusMap>> {
  const data: { id?: string; ids?: string } = {}
  if (params.id) {
    data.id = params.id
  }
  if (params.ids) {
    data.ids = typeof params.ids === 'string' ? params.ids : JSON.stringify(params.ids)
  }
  return axios({
    url: '/node/ssh-project/status',
    method: 'post',
    data,
    // 批量状态需要逐个机器执行 status 脚本，耗时随条数增长，放宽超时避免误判失败
    timeout: 120000
  })
}

/**
 * 可查看的日志文件列表
 */
export function getSshProjectLogList(id: string): Promise<IResponse<SshProjectLogItem[]>> {
  return axios({
    url: '/node/ssh-project/log-list',
    method: 'post',
    data: { id }
  })
}

/**
 * 日志备份列表（控制台日志的备份文件）
 */
export function getSshProjectLogBackupList(id: string): Promise<IResponse<SshProjectLogBackup[]>> {
  return axios({
    url: '/node/ssh-project/log/list-backup',
    method: 'post',
    data: { id }
  })
}

/**
 * 备份当前控制台日志
 * @returns 备份文件名
 */
export function backupSshProjectLog(id: string): Promise<IResponse<string>> {
  return axios({
    url: '/node/ssh-project/log/backup',
    method: 'post',
    data: { id }
  })
}

/**
 * 清空当前控制台日志
 */
export function resetSshProjectLog(id: string): Promise<IResponse<string>> {
  return axios({
    url: '/node/ssh-project/log/reset',
    method: 'post',
    data: { id }
  })
}

/**
 * 下载日志（name 为 console 时下载当前日志，否则下载对应备份文件）
 * <p>
 * 走 blob 下载：复用 request 封装的 Authorization 头与工作空间头，
 * 失败时后端返回 JSON，由调用方判断 blob 类型后给出提示。
 */
export function downloadSshProjectLog(params: { id: string; name: string }): Promise<IResponse<Blob>> {
  return axios({
    url: '/node/ssh-project/log/download',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 0,
    headers: {
      [NO_NOTIFY_KEY]: 'no'
    }
  })
}

/**
 * 模板列表
 */
export function getSshProjectTemplateList(): Promise<IResponse<SshProjectTemplate[]>> {
  return axios({
    url: '/node/ssh-project/template-list',
    method: 'post'
  })
}

/**
 * 按模板渲染 DSL 内容
 * @param params templateKey 模板标记；fieldsJson 模板参数 JSON 字符串，形如 {"javaBin":"java"}
 */
export function renderSshProjectTemplate(params: {
  templateKey: string
  fieldsJson: string
}): Promise<IResponse<SshProjectRenderResult>> {
  return axios({
    url: '/node/ssh-project/template-render',
    method: 'post',
    data: params
  })
}
