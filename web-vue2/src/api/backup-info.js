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
import { loadRouterBase } from "./config";

/**
 * 备份列表
 * @param {
 *  name: 备份名称
 *  backupType: 备份类型{0: 全量, 1: 部分}
 * } params
 */
export function getBackupList(params) {
  return axios({
    url: "/system/backup/list",
    method: "post",
    data: params,
  });
}

/**
 * 获取数据库表名列表
 */
export function getTableNameList() {
  return axios({
    url: "/system/backup/table-name-list",
    method: "post",
  });
}

/**
 * 创建备份信息
 * @param tableNameList 需要备份的表名称列表，没有默认表示全量备份
 */
export function createBackup(tableNameList) {
  const data = {
    tableNameList,
  };
  return axios({
    url: "/system/backup/create",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data,
  });
}

/**
 * 删除备份信息
 * @param {*} id
 */
export function deleteBackup(id) {
  return axios({
    url: "/system/backup/delete",
    method: "post",
    data: { id },
  });
}

/**
 * 还原备份信息
 * @param {*} id
 * @returns
 */
export function restoreBackup(id) {
  return axios({
    url: "/system/backup/restore",
    method: "post",
    timeout: 0,
    data: { id },
  });
}

/**
 * 下载备份文件
 * @param {*} id
 * @returns
 */
export function downloadBackupFile(id) {
  return loadRouterBase("/system/backup/download", {
    id: id,
  });
}

/**
 * 上传 SQL 备份文件
 * @param {
 *  file: 文件 multipart/form-data
 *  bakcupType: 0 全量备份 1 部分备份
 * } formData
 */
export function uploadBackupFile(formData) {
  return axios({
    url: "/system/backup/upload",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

export const backupTypeArray = [
  { key: 0, value: "全量备份", disabled: false },
  { key: 1, value: "部分备份", disabled: false },
  { key: 2, value: "导入备份", disabled: true },
  { key: 3, value: "自动备份", disabled: true },
];

export const arrayToMap = (arra) => {
  let obj = {};
  arra.forEach((value) => {
    obj[value.key] = value.value;
  });
  return obj;
};

export const backupTypeMap = arrayToMap(backupTypeArray);

export const backupStatusMap = {
  0: "处理中",
  1: "处理成功",
  2: "处理失败",
};
