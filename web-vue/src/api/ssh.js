import axios from "./config";
import { loadRouterBase } from "./config";

// ssh 列表
export function getSshList(params) {
  return axios({
    url: "/node/ssh/list_data.json",
    method: "post",
    data: params,
  });
}

// ssh group all
export function getSshGroupAll() {
  return axios({
    url: "/node/ssh/list-group-all",
    method: "get",
  });
}

// 查询单个 ssh
export function getItem(params) {
  return axios({
    url: "/node/ssh/get-item.json",
    method: "get",
    params: params,
  });
}

// 根据 nodeId 查询列表
export function getSshListAll() {
  return axios({
    url: "/node/ssh/list_data_all.json",
    method: "get",
  });
}

// ssh 操作日志列表
export function getSshOperationLogList(params) {
  return axios({
    url: "/node/ssh/log_list_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 编辑 SSH
 * @param {*} params
 * params.type = {'add': 表示新增, 'edit': 表示修改}
 */
export function editSsh(params) {
  return axios({
    url: "/node/ssh/save.json",
    method: "post",

    params,
  });
}

// 删除 SSH
export function deleteSsh(id) {
  return axios({
    url: "/node/ssh/del.json",
    method: "post",
    data: { id },
  });
}

/**
 * 上传文件到 SSH 节点
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  name: 当前目录,
 *  path: 父级目录
 * } formData
 */
export function uploadFile(formData) {
  return axios({
    url: "/node/ssh/upload",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

/**
 * 授权目录列表
 * @param {String} id
 */
export function getRootFileList(id) {
  return axios({
    url: "/node/ssh/root_file_data.json",
    method: "post",
    data: { id },
  });
}

/**
 * 文件列表
 * @param {id, path, children} params
 */
export function getFileList(params) {
  return axios({
    url: "/node/ssh/list_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 下载文件
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {id, path, name} params
 */
export function downloadFile(params) {
  return loadRouterBase("/node/ssh/download.html", params);
}

/**
 * 删除文件
 * @param {id, path, name} params x
 */
export function deleteFile(params) {
  return axios({
    url: "/node/ssh/delete.json",
    method: "post",
    data: params,
  });
}

/**
 * 读取文件
 * @param {id, path, name} params x
 */
export function readFile(params) {
  return axios({
    url: "/node/ssh/read_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 保存文件
 * @param {id, path, name,content} params x
 */
export function updateFileData(params) {
  return axios({
    url: "/node/ssh/update_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 新增目录  或文件
 * @param params
 * @returns {id, path, name,unFolder} params x
 */
export function newFileFolder(params) {
  return axios({
    url: "/node/ssh/new_file_folder.json",
    method: "post",
    data: params,
  });
}

/**
 * 修改目录或文件名称
 * @param params
 * @returns {id, levelName, filename,newname} params x
 */
export function renameFileFolder(params) {
  return axios({
    url: "/node/ssh/rename.json",
    method: "post",
    data: params,
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/node/ssh/sync-to-workspace",
    method: "get",
    params: params,
  });
}
