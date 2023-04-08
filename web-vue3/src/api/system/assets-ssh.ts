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

import axios from "@/api/config";

// ssh 列表
export function machineSshListData(params) {
  return axios({
    url: "/system/assets/ssh/list-data",
    method: "post",
    params: params,
  });
}

export function machineSshListGroup(params) {
  return axios({
    url: "/system/assets/ssh/list-group",
    method: "get",
    params: params,
  });
}

// 编辑ssh
export function machineSshEdit(params) {
  return axios({
    url: "/system/assets/ssh/edit",
    method: "post",
    params: params,
  });
}

// 检查 agent
export function machineSshCheckAgent(params) {
  return axios({
    url: "/system/assets/ssh/check-agent",
    method: "get",
    params: params,
    timeout: 0,
    headers: {
      loading: "no",
    },
  });
}

// 删除 ssh
export function machineSshDelete(params) {
  return axios({
    url: "/system/assets/ssh/delete",
    method: "post",
    params: params,
  });
}

// 分配 ssh
export function machineSshDistribute(params) {
  return axios({
    url: "/system/assets/ssh/distribute",
    method: "post",
    params: params,
  });
}

// ssh 操作日志列表
export function getMachineSshOperationLogList(params) {
  return axios({
    url: "/system/assets/ssh/log-list-data",
    method: "post",
    data: params,
  });
}

// ssh 关联工作空间的数据
export function machineListGroupWorkspaceSsh(params) {
  return axios({
    url: "/system/assets/ssh/list-workspace-ssh",
    method: "get",
    params: params,
  });
}

export function machineSshSaveWorkspaceConfig(params) {
  return axios({
    url: "/system/assets/ssh/save-workspace-config",
    method: "post",
    params: params,
  });
}

/**
 * restHideField by id
 * @param {String} id
 * @returns
 */
export function restHideField(id) {
  return axios({
    url: "/system/assets/ssh/rest-hide-field",
    method: "post",
    data: { id },
  });
}
