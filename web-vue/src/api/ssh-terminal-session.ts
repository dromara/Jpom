/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

import { t } from '@/i18n'
import axios, { loadRouterBase } from './config'

/**
 * SSH 终端会话录制列表
 * 支持 '%sshName%'、'%machineSshName%' 模糊查询与 machineSshId、userId、sshId 精确过滤
 * @param params 分页与查询参数
 */
export function getSshTerminalSessionList(params) {
  return axios({
    url: '/node/ssh-terminal-session/list',
    method: 'post',
    data: params
  })
}

/**
 * 会话命令索引（用于回放跳转）
 * @param id 会话 id
 */
export function getSshTerminalSessionCommands(id) {
  return axios({
    url: '/node/ssh-terminal-session/commands',
    method: 'get',
    params: { id }
  })
}

/**
 * 删除会话录制记录（多个使用逗号分隔）
 * @param id 会话 id
 */
export function deleteSshTerminalSession(id) {
  return axios({
    url: '/node/ssh-terminal-session/del',
    method: 'post',
    data: { id }
  })
}

/**
 * 会话录制文件地址（.cast，同源 cookie 鉴权，可直接给播放器或下载使用）
 * @param id 会话 id
 */
export function getSshTerminalSessionCastUrl(id) {
  return loadRouterBase('/node/ssh-terminal-session/cast', { id })
}

/**
 * 会话状态
 */
export const sshTerminalSessionStatusMap = {
  0: t('i18n_031e3d9738'),
  1: t('i18n_3bfeb6bb1f'),
  2: t('i18n_cb55d2720c')
}

/**
 * 会话状态对应的标签颜色（使用 ant 预设色，避免写死色值）
 */
export const sshTerminalSessionStatusColorMap = {
  0: 'processing',
  1: 'success',
  2: 'error'
}
