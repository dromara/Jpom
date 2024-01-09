/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import axios from "./config";
import { loadRouterBase } from "./config";

/**
 * 日志列表
 * @param {nodeId} params
 */
export function getLogList(params) {
  return axios({
    url: "/system/log_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 下载日志
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path} params
 */
export function downloadFile(params) {
  return loadRouterBase("/system/log_download", params);
}

/**
 * 删除日志
 * @param {nodeId, path} params
 */
export function deleteLog(params) {
  return axios({
    url: "/system/log_del.json",
    method: "post",
    data: params,
  });
}

/**
 * server 缓存数据
 */
export function getServerCache() {
  return axios({
    url: "/system/server-cache",
    method: "post",
  });
}

/**
 * 节点缓存数据
 * @param {String} nodeId
 */
export function getNodeCache(data) {
  return axios({
    url: "/system/node_cache.json",
    method: "post",
    data,
  });
}

/**
 * 清空缓存
 * @param {
 *  type: 类型
 *  nodeId: 节点 ID
 * } params
 */
export function clearCache(params) {
  return axios({
    url: "/system/clearCache.json",
    method: "post",
    data: params,
  });
}

/**
 * 清理错误工作空间的数据
 *
 */
export function clearErrorWorkspace(params) {
  return axios({
    url: "/system/clear-error-workspace",
    method: "get",

    headers: {},
    params,
  });
}

/**
 * 加载配置数据
 * @param {String} nodeId 节点 ID，若为空表示加载 Server 端配置
 */
export function getConfigData(data) {
  return axios({
    url: "/system/config-data",
    method: "post",
    data: data,
  });
}

/**
 * 加载ip配置数据
 */
export function getIpConfigData() {
  return axios({
    url: "/system/ip-config-data",
    method: "post",
    data: {},
  });
}

/**
 * 编辑配置
 * @param {JSON} params {
 *  allowed: 允许访问，授权ip,
 *  prohibited: 禁止访问，禁止ip
 * }
 */
export function editIpConfig(params) {
  return axios({
    url: "/system/save_ip_config.json",
    method: "post",
    data: params,
  });
}

/**
 * 编辑配置
 * @param {
 *  nodeId: 节点 ID,
 *  content: 配置内容,
 *  restart: 是否重启
 * } params
 */
export function editConfig(params) {
  return axios({
    url: "/system/save_config.json",
    method: "post",
    data: params,
  });
}

/**
 * 加载邮件配置
 */
export function getMailConfigData() {
  return axios({
    url: "/system/mail-config-data",
    method: "post",
  });
}

export function oauthConfigOauth2(params) {
  return axios({
    url: "/system/oauth-config/oauth2",
    method: "get",
    params,
  });
}

export function oauthConfigOauth2Save(params) {
  return axios({
    url: "/system/oauth-config/oauth2-save",
    method: "post",
    data: params,
  });
}

/**
 * 编辑配置
 * @param {
 *  host: SMTP 服务器域名,
 *  port: SMTP 服务端口,
 *  user: 用户名,
 *  pass: 密码,
 *  from: 发送方，遵循RFC-822标准,
 *  sslEnable: 是否 SSL 安全连接,
 *  socketFactoryPort: SSL 加密端口
 * } params
 */
export function editMailConfig(params) {
  return axios({
    url: "/system/mailConfig_save.json",
    method: "post",
    data: params,
  });
}

/**
 * 系统程序信息
 * @param {String} nodeId 节点 ID
 */
export function systemInfo(data) {
  return axios({
    url: "/system/info",
    method: "post",
    headers: {
      tip: "no",
      loading: "no",
    },
    data,
  });
}

/**
 * 上传升级文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  nodeId: 节点 ID
 * } formData
 */
export function uploadUpgradeFile(formData) {
  return axios({
    url: "/system/upload-jar-sharding",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
      loading: "no",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

/**
 * 上传文件合并
 *@param {String} nodeId 节点 ID
 */
export function uploadUpgradeFileMerge(data) {
  return axios({
    url: "/system/upload-jar-sharding-merge",
    method: "post",
    headers: {},
    data: data,
    // 0 表示无超时时间
    timeout: 0,
  });
}

/**
 * 获取更新日志
 *@param {String} nodeId 节点 ID
 */
export function changelog(data) {
  return axios({
    url: "/system/change_log",
    method: "post",
    headers: {},
    data,
  });
}

export function changBetaRelease(params) {
  return axios({
    url: "/system/change-beta-release",
    method: "get",
    headers: {},
    params,
  });
}

/**
 * 检查新版本
 *@param {String} nodeId 节点 ID
 */
export function checkVersion(data) {
  return axios({
    url: "/system/check_version.json",
    method: "post",
    headers: {},
    data,
  });
}

/**
 * 远程升级
 *@param {String} nodeId 节点 ID
 */
export function remoteUpgrade(params) {
  return axios({
    url: "/system/remote_upgrade.json",
    method: "get",
    timeout: 0,
    headers: {},
    params,
  });
}

/**
 * 加载 代理配置
 */
export function getProxyConfig() {
  return axios({
    url: "/system/get_proxy_config",
    method: "get",
    params: {},
  });
}

/**
 * 保存代理配置
 */
export function saveProxyConfig(data) {
  return axios({
    url: "/system/save_proxy_config",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data: data,
  });
}
