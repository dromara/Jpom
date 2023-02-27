import axios from "./config";
import { loadRouterBase } from "./config";

/**
 * 上传文件到 SSH 节点
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  name: 当前目录,
 *  path: 父级目录
 * } formData
 */
export function uploadFile(baseUrl, formData) {
  return axios({
    url: baseUrl + "upload",
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
export function getRootFileList(baseUrl, id) {
  return axios({
    url: baseUrl + "root_file_data.json",
    method: "post",
    data: { id },
  });
}

/**
 * 文件列表
 * @param {id, path, children} params
 */
export function getFileList(baseUrl, params) {
  return axios({
    url: baseUrl + "list_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 下载文件
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {id, path, name} params
 */
export function downloadFile(baseUrl, params) {
  return loadRouterBase(baseUrl + "download.html", params);
}

/**
 * 删除文件
 * @param {id, path, name} params x
 */
export function deleteFile(baseUrl, params) {
  return axios({
    url: baseUrl + "delete.json",
    method: "post",
    data: params,
  });
}

/**
 * 读取文件
 * @param {id, path, name} params x
 */
export function readFile(baseUrl, params) {
  return axios({
    url: baseUrl + "read_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 保存文件
 * @param {id, path, name,content} params x
 */
export function updateFileData(baseUrl, params) {
  return axios({
    url: baseUrl + "update_file_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 新增目录  或文件
 * @param params
 * @returns {id, path, name,unFolder} params x
 */
export function newFileFolder(baseUrl, params) {
  return axios({
    url: baseUrl + "new_file_folder.json",
    method: "post",
    data: params,
  });
}

/**
 * 修改目录或文件名称
 * @param params
 * @returns {id, levelName, filename,newname} params x
 */
export function renameFileFolder(baseUrl, params) {
  return axios({
    url: baseUrl + "rename.json",
    method: "post",
    data: params,
  });
}
