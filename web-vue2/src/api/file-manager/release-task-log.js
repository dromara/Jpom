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

// 任务列表
export function fileReleaseTaskLog(params) {
  return axios({
    url: "/file-storage/release-task/list",
    method: "post",
    data: params,
  });
}

// 添加发布任务
export function addReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/add-task",
    method: "post",
    data: params,
  });
}

// 重新发布任务
export function reReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/re-task",
    method: "post",
    data: params,
  });
}

// 取消任务
export function cancelReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/cancel-task",
    method: "get",
    params: params,
  });
}

// 删除任务
export function deleteReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/delete",
    method: "get",
    params: params,
  });
}

// 任务详情
export function taskDetails(params) {
  return axios({
    url: "/file-storage/release-task/details",
    method: "get",
    params: params,
  });
}

export function taskLogInfoList(params) {
  return axios({
    url: "/file-storage/release-task/log-list",
    method: "get",
    params: params,
    headers: {
      loading: "no",
    },
  });
}

export const statusMap = {
  0: "等待开始",
  1: "进行中",
  2: "任务结束",
  3: "发布失败",
  4: "取消任务",
};

export const taskTypeMap = {
  0: "SSH",
  1: "节点",
};
