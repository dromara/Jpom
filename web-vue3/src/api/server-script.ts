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

/**
 * script 服务端中的列表
 */
export function getScriptList(params) {
  return axios({
    url: "/script/list",
    method: "post",
    data: params,
  });
}

/**
 * script 服务端中的所有列表
 */
export function getScriptListAll(params) {
  return axios({
    url: "/script/list-all",
    method: "get",
    params: params,
  });
}

/**
 *  保存脚本
 * @param {Json} params
 * @returns
 */
export function editScript(params) {
  return axios({
    url: "/script/save.json",
    method: "post",
    data: params,
  });
}

/**
 * 删除 Script
 * @param {id} params
 
 * params.id 编辑修改时判断 ID
 */
export function deleteScript(params) {
  return axios({
    url: "/script/del.json",
    method: "post",
    data: params,
  });
}

/**
 * 解绑 Script
 * @param {id} params
 
 * params.id 编辑修改时判断 ID
 */
export function unbindScript(params) {
  return axios({
    url: "/script/unbind.json",
    method: "get",
    params: params,
  });
}

// 脚本模版日志列表
export function getScriptLogList(params) {
  return axios({
    url: "/script_log/list",
    method: "post",
    data: params,
  });
}

// 删除执行记录
export function scriptDel(params) {
  return axios({
    url: "/script_log/del_log",
    method: "post",
    data: params,
  });
}

//执行记录 详情
export function scriptLog(params) {
  return axios({
    url: "/script_log/log",
    method: "post",
    data: params,
    headers: {
      tip: "no",
    },
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/script/sync-to-workspace",
    method: "get",
    params: params,
  });
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data) {
  return axios({
    url: "/script/trigger-url",
    method: "post",
    data: data,
  });
}

export const triggerExecTypeMap = {
  0: "手动",
  1: "自动",
  2: "触发器",
};
