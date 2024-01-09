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
import axios from "@/api/config";
import { loadRouterBase } from "@/api/config";

// ssh 列表
export function machineSshListData(params) {
  return axios({
    url: "/system/assets/ssh/list-data",
    method: "post",
    data: params,
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
    data: params,
  });
}

// 删除 ssh
export function machineSshDelete(params) {
  return axios({
    url: "/system/assets/ssh/delete",
    method: "post",
    data: params,
  });
}

// 分配 ssh
export function machineSshDistribute(params) {
  return axios({
    url: "/system/assets/ssh/distribute",
    method: "post",
    data: params,
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
    data: params,
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
/*
 * 下载导入模板
 *
 */
export function importTemplate(data) {
  return loadRouterBase("/system/assets/ssh/import-template", data);
}

/*
 * 导出数据
 *
 */
export function exportData(data) {
  return loadRouterBase("/system/assets/ssh/export-data", data);
}
// 导入数据
export function importData(formData) {
  return axios({
    url: "/system/assets/ssh/import-data",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

export const statusMap = {
  0: { desc: "无法连接", color: "red" },
  1: { desc: "正常", color: "green" },
  2: { desc: "禁用监控", color: "orange" },
};
