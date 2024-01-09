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

// 机器 列表
export function machineListData(params) {
  return axios({
    url: "/system/assets/machine/list-data",
    method: "post",
    data: params,
  });
}

export function machineListGroup(params) {
  return axios({
    url: "/system/assets/machine/list-group",
    method: "get",
    params: params,
  });
}

// 编辑机器
export function machineEdit(params) {
  return axios({
    url: "/system/assets/machine/edit",
    method: "post",
    data: params,
  });
}

// 删除机器
export function machineDelete(params) {
  return axios({
    url: "/system/assets/machine/delete",
    method: "post",
    data: params,
  });
}

// 分配机器
export function machineDistribute(params) {
  return axios({
    url: "/system/assets/machine/distribute",
    method: "post",
    data: params,
  });
}

export const statusMap = {
  0: "无法连接",
  1: "正常",
  2: "授权信息错误",
  3: "状态码错误",
};

// 查看机器关联节点
export function machineListNode(params) {
  return axios({
    url: "/system/assets/machine/list-node",
    method: "get",
    params: params,
  });
}

export function machineListTemplateNode(params) {
  return axios({
    url: "/system/assets/machine/list-template-node",
    method: "get",
    params: params,
  });
}

/**
 * 保存 授权配置
 */
export function saveWhitelist(data) {
  return axios({
    url: "/system/assets/machine/save-whitelist",
    method: "post",
    data: data,
  });
}

/**
 * 保存 节点系统配置
 */
export function saveNodeConfig(data) {
  return axios({
    url: "/system/assets/machine/save-node-config",
    method: "post",
    data: data,
  });
}
