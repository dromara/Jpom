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

// 分发列表
export function getDishPatchList(data, loading) {
  return axios({
    url: "/outgiving/dispatch-list",
    method: "post",
    data: data,
    headers: {
      loading: loading === false ? "no" : "",
    },
  });
}

// 分发列表
export function getDishPatchListAll() {
  return axios({
    url: "/outgiving/dispatch-list-all",
    method: "get",
  });
}

// 分发节点项目状态
export function getDispatchProject(id, loading) {
  return axios({
    url: "/outgiving/getItemData.json",
    method: "post",
    data: { id },
    timeout: 0,
    headers: {
      loading: loading === false ? "no" : "",
    },
  });
}

// reqId
export function getReqId() {
  return axios({
    url: "/outgiving/get-reqId",
    method: "get",
  });
}

/**
 * 编辑分发
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  node_xxx: xxx 表示节点 ID
 *  afterOpt: 发布后操作
 * } params
 */
export function editDispatch(params) {
  return axios({
    url: "/outgiving/save",
    method: "post",
    data: params,
  });
}

/**
 * 编辑分发项目
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  afterOpt: 发布后操作
 *  runMode: 运行方式
 *  mainClass: 启动类
 *  javaExtDirsCp: 目录地址
 *  whitelistDirectory: 白名单地址
 *  lib: lib
 *  add_xxx: xxx 表示添加的节点信息
 *  xxx_token: xxx 节点的 webhook 地址
 *  xxx_jvm: jvm 参数
 *  xxx_args: args 参数
 *  xxx_javaCopyIds: xxx 节点副本 ID （如果有副本，还需要加上 xxx_jvm_${副本ID} | xxx_args_${副本ID} 参数）
 * } params
 */
export function editDispatchProject(params) {
  return axios({
    url: "/outgiving/save_project",
    method: "post",
    data: params,
  });
}

/**
 * 上传分发文件
 * @param {
 *  id: 分发 ID
 *  afterOpt: 发布后操作
 *  clearOld: 是否清除
 *  file: 文件 multipart/form-data
 * } formData
 */
export function uploadDispatchFile(formData) {
  return axios({
    url: "/outgiving/upload-sharding",
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

export function uploadDispatchFileMerge(params) {
  return axios({
    url: "/outgiving/upload-sharding-merge",
    method: "post",
    data: params,
    // 0 表示无超时时间
    timeout: 0,
  });
}

/**
 * 远程下载文件分发
 * @param {
 *  id: 节点 ID
 *  clearOld:  是否清空
 *  afterOpt: 分发后的操作
 *  url: 远程URL
 *   unzip：是否为压缩包
 * } params
 */
export function remoteDownload(params) {
  return axios({
    url: "/outgiving/remote_download",
    method: "post",
    data: params,
  });
}

/**
 * 释放分发
 * @param {*} id 分发 ID
 */
export function releaseDelDisPatch(id) {
  return axios({
    url: "/outgiving/release_del.json",
    method: "post",
    data: { id },
  });
}

/**
 * 删除分发
 * @param {*} id 分发 ID
 */
export function delDisPatchProject(data) {
  return axios({
    url: "/outgiving/delete_project",
    method: "post",
    data: data,
  });
}

/**
 * 解绑分发
 * @param {*} id 分发 ID
 */
export function unbindOutgiving(id) {
  return axios({
    url: "/outgiving/unbind.json",
    method: "get",
    params: { id },
  });
}

/**
 * 分发日志
 * @param {
 *  nodeId: 节点 ID
 *  outGivingId: 分发 ID
 *  status: 分发状态
 *  time: 时间区间 2021-01-04 00:00:00 ~ 2021-01-12 00:00:00
 * } params
 */
export function getDishPatchLogList(params) {
  return axios({
    url: "/outgiving/log_list_data.json",
    method: "post",
    data: params,
  });
}

// 获取分发白名单数据
export function getDispatchWhiteList() {
  return axios({
    url: "/outgiving/white-list",
    method: "post",
  });
}

/**
 * 编辑分发白名单
 * @param {*} params
 */
export function editDispatchWhiteList(params) {
  return axios({
    url: "/outgiving/whitelistDirectory_submit",
    method: "post",
    data: params,
  });
}

/**
 * 取消分发
 * @param {*} id 分发 ID
 */
export function cancelOutgiving(data) {
  return axios({
    url: "/outgiving/cancel",
    method: "post",
    data,
  });
}

export const afterOptList = [
  { title: "不做任何操作", value: 0 },
  { title: "并发执行", value: 1 },
  { title: "完整顺序执行(有执行失败将结束本次)", value: 2 },
  { title: "顺序执行(有执行失败将继续)", value: 3 },
];

export const afterOptListSimple = [
  { title: "不做任何操作", value: 0 },
  { title: "执行重启", value: 1 },
];

export const dispatchStatusMap = {
  0: "未分发",
  1: "分发中",
  2: "分发成功",
  3: "分发失败",
  4: "系统取消分发",
  5: "准备分发",
  6: "手动取消分发",
};

export const statusMap = {
  0: "未分发",
  1: "分发中",
  2: "分发结束",
  3: "取消分发",
  4: "分发失败",
};
