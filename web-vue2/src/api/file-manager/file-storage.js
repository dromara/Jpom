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

// 文件列表
export function fileStorageList(params) {
  return axios({
    url: "/file-storage/list",
    method: "post",
    data: params,
  });
}

export function uploadFile(formData) {
  return axios({
    url: "/file-storage/upload-sharding",
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

export function uploadFileMerge(params) {
  return axios({
    url: "/file-storage/upload-sharding-merge",
    method: "post",
    data: params,
    // 0 表示无超时时间
    timeout: 0,
  });
}

// 修改文件
export function fileEdit(params) {
  return axios({
    url: "/file-storage/edit",
    method: "post",
    data: params,
  });
}

// 下载远程文件
export function remoteDownload(params) {
  return axios({
    url: "/file-storage/remote-download",
    method: "post",
    data: params,
  });
}

// 判断文件是否存在
export function hasFile(params) {
  return axios({
    url: "/file-storage/has-file",
    method: "get",
    params: params,
  });
}

export function delFile(params) {
  return axios({
    url: "/file-storage/del",
    method: "get",
    params: params,
  });
}

// 下载 url
export function triggerUrl(params) {
  return axios({
    url: "/file-storage/trigger-url",
    method: "get",
    params: params,
  });
}

export const sourceMap = {
  0: "上传",
  1: "构建",
  2: "下载",
  3: "证书",
};

export const statusMap = {
  0: "下载中",
  1: "下载成功",
  2: "下载异常",
};
