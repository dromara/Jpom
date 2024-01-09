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
 * 项目列表
 * @param {JSON} params {
 *  nodeId: 节点 ID,
 *  id: 项目ID
 * }
 */
export function listBackup(params) {
  return axios({
    url: "/node/manage/file/list-backup",
    method: "post",
    data: params,
  });
}

export function backupFileList(params) {
  return axios({
    url: "/node/manage/file/backup-item-files",
    method: "post",
    headers: {
      loading: "no",
    },
    data: params,
  });
}

/**
 * 下载项目文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 * } params
 */
export function backupDownloadProjectFile(params) {
  return loadRouterBase("/node/manage/file/backup-download", params);
}

/**
 * 删除文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupDeleteProjectFile(params) {
  return axios({
    url: "/node/manage/file/backup-delete",
    method: "post",
    data: params,
  });
}

/**
 * 还原文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupRecoverProjectFile(params) {
  return axios({
    url: "/node/manage/file/backup-recover",
    method: "post",
    data: params,
  });
}
