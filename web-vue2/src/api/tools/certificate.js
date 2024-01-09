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
// 导入证书
export function certificateImportFile(formData) {
  return axios({
    url: "/certificate/import-file",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",

    data: formData,
  });
}

/**
 * cert 列表
 */
export function certList(params) {
  return axios({
    url: "/certificate/list",
    method: "post",
    data: params,
  });
}

/**
 * cert 列表
 */
export function certListAll(params) {
  return axios({
    url: "/certificate/list-all",
    method: "post",
    data: params,
  });
}

/**
 * 删除 cert
 * @param {
 *
 * } params
 */
export function deleteCert(params) {
  return axios({
    url: "/certificate/del",
    method: "get",
    params: params,
  });
}

/**
 * 导出 cert
 * @param {
 *
 * } params
 */
export function downloadCert(params) {
  return loadRouterBase("/certificate/export", params);
}

// 修改证书
export function certificateEdit(params) {
  return axios({
    url: "/certificate/edit",
    method: "post",
    data: params,
  });
}

// 部署证书
export function certificateDeploy(params) {
  return axios({
    url: "/certificate/deploy",
    method: "post",
    data: params,
  });
}
