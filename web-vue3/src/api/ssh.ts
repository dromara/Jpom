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

// ssh 列表
export function getSshList(params) {
  return axios({
    url: "/node/ssh/list_data.json",
    method: "post",
    data: params,
  });
}

// ssh group all
export function getSshGroupAll() {
  return axios({
    url: "/node/ssh/list-group-all",
    method: "get",
  });
}

// 查询单个 ssh
export function getItem(params) {
  return axios({
    url: "/node/ssh/get-item.json",
    method: "get",
    params: params,
  });
}

// 根据 nodeId 查询列表
export function getSshListAll() {
  return axios({
    url: "/node/ssh/list_data_all.json",
    method: "get",
  });
}

// ssh 操作日志列表
export function getSshOperationLogList(params) {
  return axios({
    url: "/node/ssh/log_list_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 编辑 SSH
 * @param {*} params
 * params.type = {'add': 表示新增, 'edit': 表示修改}
 */
export function editSsh(params) {
  return axios({
    url: "/node/ssh/save.json",
    method: "post",

    params,
  });
}

// 删除 SSH
export function deleteSsh(id) {
  return axios({
    url: "/node/ssh/del.json",
    method: "post",
    data: { id },
  });
}

// 删除 SSH
export function deleteForeSsh(id) {
  return axios({
    url: "/node/ssh/del-fore",
    method: "post",
    data: { id },
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/node/ssh/sync-to-workspace",
    method: "get",
    params: params,
  });
}
