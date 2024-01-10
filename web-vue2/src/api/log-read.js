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

// 日志搜索列表
export function getLogReadList(params) {
  return axios({
    url: "/log-read/list",
    method: "post",
    data: params,
  });
}

/**
 * 编辑日志搜索
 * @param {
 *  id: 监控 ID
 *  name: 监控名称
 *  nodeProject: { nodeId:'',projectId:''}
 *
 * } params
 */
export function editLogRead(params) {
  return axios({
    url: "/log-read/save.json",
    method: "post",
    data: params,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

export function updateCache(params) {
  return axios({
    url: "/log-read/update-cache.json",
    method: "post",
    data: params,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

/**
 * 删除日志搜索
 * @param {*} id
 */
export function deleteLogRead(id) {
  return axios({
    url: "/log-read/del.json",
    method: "post",
    data: { id },
  });
}
