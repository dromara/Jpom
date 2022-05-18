import axios from "./config";
/**
 * 项目列表
 * @param {JSON} params {
 *  nodeId: 节点 ID,
 *  id: 项目ID
 * }
 */
export function listBackup(params) {
  return axios({
    url: "/node/manage/file/list-backup",
    method: "post",
    data: params,
  });
}

export function backupFileList(params) {
  return axios({
    url: "/node/manage/file/backup-item-files",
    method: "post",
    headers: {
      loading: "no",
    },
    data: params,
  });
}

/**
 * 下载项目文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 * } params
 */
export function backupDownloadProjectFile(params) {
  return axios({
    url: "/node/manage/file/backup-download",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

/**
 * 删除文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupDeleteProjectFile(params) {
  return axios({
    url: "/node/manage/file/backup-delete",
    method: "post",
    data: params,
  });
}

/**
 * 还原文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *
 * } params
 */
export function backupRecoverProjectFile(params) {
  return axios({
    url: "/node/manage/file/backup-recover",
    method: "post",
    data: params,
  });
}
