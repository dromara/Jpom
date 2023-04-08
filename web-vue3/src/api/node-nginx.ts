///
/// The MIT License (MIT)
///
/// Copyright (c) 2019 Code Technology Studio
///
/// Permission is hereby granted, free of charge, to any person obtaining a copy of
/// this software and associated documentation files (the "Software"), to deal in
/// the Software without restriction, including without limitation the rights to
/// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
/// the Software, and to permit persons to whom the Software is furnished to do so,
/// subject to the following conditions:
///
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
///
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
/// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
/// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
/// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
/// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
///

import axios from "./config";
import { loadRouterBase } from "./config";
/**
 * Nginx 目录列表
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function getNginxDirectoryList(params) {
  return axios({
    url: "/node/system/nginx/tree.json",
    method: "post",
    data: params,
  });
}

/**
 * Nginx 配置文件数据
 * @param {
 *  nodeId: 节点 ID
 *  whitePath: nginx 白名单目录
 *  name: 子级目录
 * } params
 */
export function getNginxFileList(params) {
  return axios({
    url: "/node/system/nginx/list_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 编辑 Nginx 配置文件
 * @param {
 *  nodeId: 节点 ID
 *  genre: 操作类型
 *  whitePath: 白名单目录
 *  name: 文件名称
 *  context: 内容
 * } params
 */
export function editNginxConfig(params) {
  return axios({
    url: "/node/system/nginx/updateNgx",
    method: "post",
    data: params,
  });
}

/**
 * 删除
 * @param {
 *  nodeId: 节点 ID
 *  path: 路径
 *  name: 文件名称
 * } params
 */
export function deleteNginxConfig(params) {
  return axios({
    url: "/node/system/nginx/delete",
    method: "post",
    data: params,
  });
}

/**
 * 加载 Nginx 白名单列表
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function loadNginxWhiteList(params) {
  return axios({
    url: "/node/system/nginx/white-list",
    method: "post",
    data: params,
  });
}

/**
 * 加载 Nginx 配置内容
 * @param {
 *  nodeId: 节点 ID
 *  path: 白名单目录
 *  name: 文件名称
 * } params
 */
export function loadNginxConfig(params) {
  return axios({
    url: "/node/system/nginx/load-config",
    method: "post",
    data: params,
  });
}

/**
 * 加载 Nginx 状态
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function loadNginxData(params) {
  return axios({
    url: "/node/system/nginx/status",
    method: "post",
    data: params,
  });
}

/**
 * 管理 Nginx 服务
 * @param {
 *  nodeId: 节点 ID
 *  command: 命令 {open || reload || stop}
 * } params
 */
export function doNginxCommand(params) {
  return axios({
    url: `/node/system/nginx/${params.command}`,
    method: "post",
    data: params,
  });
}

/**
 * 编辑 Nginx 服务名称
 * @param {
 *  nodeId: 节点 ID
 *  name: Nginx 服务名称
 * } params
 */
export function editNginxServerName(params) {
  return axios({
    url: "/node/system/nginx/updateConf",
    method: "post",
    data: params,
  });
}

/***************************** */

/**
 * cert 白名单列表
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function getCertWhiteList(params) {
  return axios({
    url: "/node/system/certificate/white-list",
    method: "post",
    data: params,
  });
}

/**
 * cert 列表
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function getCertList(params) {
  return axios({
    url: "/node/system/certificate/getCertList",
    method: "post",
    data: params,
  });
}

/**
 * 编辑 cert
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 *  data: 证书相关数据
 * } formData
 */
export function editCert(formData) {
  return axios({
    url: "/node/system/certificate/saveCertificate",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    timeout: 0,
    data: formData,
  });
}

/**
 * 删除 cert
 * @param {
 *  nodeId: 节点 ID
 *  id: 证书 ID
 * } params
 */
export function deleteCert(params) {
  return axios({
    url: "/node/system/certificate/delete",
    method: "post",
    data: params,
  });
}

/**
 * 导出 cert
 * @param {
 *  nodeId: 节点 ID
 *  id: 证书 ID
 * } params
 */
export function downloadCert(params) {
  return loadRouterBase("/node/system/certificate/export", params);
}
