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

export function staticFileStorageList(params) {
  return axios({
    url: "/file-storage/static/list",
    method: "post",
    data: params,
  });
}

export function delFile(params) {
  return axios({
    url: "/file-storage/static/del",
    method: "get",
    params: params,
  });
}

// 下载 url
export function triggerUrl(params) {
  return axios({
    url: "/file-storage/static/trigger-url",
    method: "get",
    params: params,
  });
}

// 修改文件
export function fileEdit(params) {
  return axios({
    url: "/file-storage/static/edit",
    method: "post",
    data: params,
  });
}

export function hasStaticFile(params) {
  return axios({
    url: "/file-storage/static/has-file",
    method: "get",
    params: params,
  });
}

export function staticScanner(params) {
  return axios({
    url: "/file-storage/static/scanner",
    method: "get",
    params: params,
  });
}
