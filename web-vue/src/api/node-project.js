/**
 * 节点管理 api
 */
import axios from "./config";

/**
 * jdk 列表
 * @param {String} nodeId 节点 ID
 */
export function getJdkList(nodeId) {
  return axios({
    url: "/node/manage/jdk/list",
    method: "post",
    data: { nodeId },
  });
}

/**
 * jdk 编辑
 * @param {nodeId, id, name, path} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.path jdk 路径
 */
export function editJdk(params) {
  return axios({
    url: "/node/manage/jdk/update",
    method: "post",
    data: params,
  });
}

/**
 * 删除 JDK
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function deleteJdk(params) {
  return axios({
    url: "/node/manage/jdk/delete",
    method: "post",
    data: params,
  });
}

/**
 * 项目列表
 * @param {JSON} params {
 *  nodeId: 节点 ID
 *  group: 分组名称
 * }
 */
export function getProjectList(params) {
  return axios({
    url: "/node/manage/get_project_info",
    method: "post",
    data: params,
  });
}

/**
 * 项目运行信息，返回项目占用端口和 pid
 * @param {JSON} params {
 *  nodeId: 节点 ID
 *  ids: 项目 ID 数组字符串格式 ["id1", "id2"]
 * }
 */
export function getRuningProjectInfo(params) {
  return axios({
    url: "/node/manage/getProjectPort",
    method: "post",
    data: params,
    timeout: 0,
    headers: {
      loading: "no",
    },
  });
}

/**
 *  获取副本 端口信息
 * @param {*} params
 * @returns
 */
export function getRuningProjectCopyInfo(params) {
  return axios({
    url: "/node/manage/getProjectCopyPort",
    method: "post",
    data: params,
    timeout: 0,
    headers: {
      loading: "no",
    },
  });
}

/**
 * 获取单个项目信息
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 * } params
 */
export function getProjectData(params) {
  return axios({
    url: "/node/manage/getProjectData.json",
    method: "post",
    data: params,
  });
}

/**
 * 项目白名单列表
 * @param {String} nodeId 节点 ID
 */
export function getProjectAccessList(nodeId) {
  return axios({
    url: "/node/manage/project-access-list",
    method: "post",
    data: { nodeId },
  });
}

/**
 * 编辑项目
 * @param {JSON} params {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  name: 项目名称
 *  runMode: 运行方式
 *  whitelistDirectory: 项目白名单路径
 *  lib: 项目文件夹
 *  group: 分组名称
 *  jdkId: JDK
 *  ...
 * }
 * @param {JSON} replicaParams {
 *  javaCopyIds: 副本 xx1,xx2
 *  jvm_xxn: 副本 n JVM 参数
 *  args_xxn: 副本 n args 参数
 * }
 */
export function editProject(params, replicaParams) {
  const data = {
    nodeId: params.nodeId,
    id: params.id,
    name: params.name,
    group: params.group,
    jdkId: params.jdkId,
    runMode: params.runMode,
    whitelistDirectory: params.whitelistDirectory,
    lib: params.lib,
    mainClass: params.mainClass,
    javaExtDirsCp: params.javaExtDirsCp,
    jvm: params.jvm,
    args: params.args,
    javaCopyIds: params.javaCopyIds,
    token: params.token,
    logPath: params.logPath,
    autoStart: params.autoStart,
    dslContent: params.dslContent,
    ...replicaParams,
  };
  return axios({
    url: "/node/manage/saveProject",
    method: "post",
    data,
  });
}

/**
 * 删除项目
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  copyId: copyId
 * } params
 */
export function deleteProject(params) {
  return axios({
    url: "/node/manage/deleteProject",
    method: "post",
    data: params,
  });
}

/**
 * 项目文件列表
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 * } params
 */
export function getFileList(params) {
  return axios({
    url: "/node/manage/file/getFileList",
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
export function downloadProjectFile(params) {
  return axios({
    url: "/node/manage/file/download",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

export function readFile(formData) {
  return axios({
    url: "/node/manage/file/read_file",
    method: "get",
    params: formData,
  });
}

export function remoteDownload(formData) {
  return axios({
    url: "/node/manage/file/remote_download",
    method: "get",
    params: formData,
  });
}

export function updateFile(formData) {
  return axios({
    url: "/node/manage/file/update_config_file",
    method: "post",
    data: formData,
  });
}

/**
 * 上传项目文件
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 目录地址
 *  type: unzip 表示压缩文件 *上传压缩文件时需要
 *  clearType: {clear: 清空文件夹, noClear: 不清空} *上传压缩文件时需要
 * } formData
 */
export function uploadProjectFile(formData) {
  return axios({
    url: "/node/manage/file/upload",
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
 * 删除文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 文件 levelName
 *  filename: 文件名称
 *  type: 操作类型 {clear: 清空, noclear: 不清空} 填入此参数可以忽略 levelName 和 filename 参数
 * } params
 */
export function deleteProjectFile(params) {
  return axios({
    url: "/node/manage/file/deleteFile",
    method: "post",
    data: params,
  });
}

/**
 * 项目回收列表
 * @param {String} nodeId 节点 ID
 */
export function getRecoverList(nodeId) {
  return axios({
    url: "/node/manage/recover/recover-list",
    method: "post",
    data: { nodeId },
  });
}

/**
 * 获取回收项目信息
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 * } params
 */
export function getRecoverData(params) {
  return axios({
    url: "/node/manage/recover/data.json",
    method: "post",
    data: params,
  });
}

/**
 * 获取项目日志文件大小
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  copyId: copyId
 * } params
 */
export function getProjectLogSize(params) {
  return axios({
    url: "/node/manage/log/logSize",
    method: "post",
    data: params,
    headers: {
      loading: "no",
    },
  });
}

/**
 * 下载项目日志文件
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  copyId: copyId
 * } params
 */
export function downloadProjectLogFile(params) {
  return axios({
    url: "/node/manage/log/export.html",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

/**
 * 项目日志备份列表
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 * } params
 */
export function getLogBackList(params) {
  return axios({
    url: "/node/manage/log/log-back-list",
    method: "post",
    data: params,
  });
}

/**
 * 项目日志备份文件下载
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  copyId: copyId
 *  key: 文件名
 * } params
 */
export function downloadProjectLogBackFile(params) {
  return axios({
    url: "/node/manage/log/logBack_download",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

/**
 * 项目日志备份文件删除
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  copyId: copyId
 *  name: 文件名
 * } params
 */
export function deleteProjectLogBackFile(params) {
  return axios({
    url: "/node/manage/log/logBack_delete",
    method: "post",
    data: params,
  });
}

/**
 * 获取内存信息接口
 * @param {
 *  nodeId: 节点 ID
 *  tag: 项目 ID
 *  copyId: copyId
 * } params
 */
export function getInternalData(params) {
  return axios({
    url: "/node/manage/getInternalData",
    method: "post",
    timeout: 0,
    data: params,
  });
}

/**
 * 查看线程
 * @param {
 *  nodeId: 节点 ID
 * } params
 */
export function getThreadInfo(params) {
  return axios({
    url: "/node/manage/threadInfos",
    method: "post",
    timeout: 0,
    data: params,
  });
}

/**
 * 导出堆栈信息
 * @param {
 *  nodeId: 节点 ID
 *  tag: 项目 ID
 *  copyId: copyId
 * } params
 */
export function exportStack(params) {
  return axios({
    url: "/node/manage/stack",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

/**
 * 导出内存信息
 * @param {
 *  nodeId: 节点 ID
 *  tag: 项目 ID
 *  copyId: copyId
 * } params
 */
export function exportRam(params) {
  return axios({
    url: "/node/manage/ram",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
}

/**
 * 加载副本集
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 * } params
 */
export function getProjectReplicaList(params) {
  return axios({
    url: "/node/manage/project_copy_list",
    method: "post",
    data: params,
  });
}

/**
 * 查询节点目录是否存在
 * @param {
 *  nodeId: 节点 ID,
 *  newLib: 新目录地址
 * } params
 */
export function nodeJudgeLibExist(params) {
  return axios({
    url: "/node/manage/judge_lib.json",
    method: "post",
    data: params,
    headers: {
      tip: "no",
    },
  });
}

/**
 * 重启项目
 * @param {
 *  nodeId: 节点 ID,
 *  id: 项目id
 *  copyId: 副本id
 * } params
 */
export function restartProject(params) {
  return axios({
    url: "/node/manage/restart",
    method: "post",
    data: params,
    headers: {
      loading: "no",
      tip: "no",
    },
  });
}

/**
 * 启动项目
 * @param {
 *  nodeId: 节点 ID,
 *  id: 项目id
 *  copyId: 副本id
 * } params
 */
export function startProject(params) {
  return axios({
    url: "/node/manage/start",
    method: "post",
    data: params,
    headers: {
      loading: "no",
      tip: "no",
    },
  });
}

/**
 * 关闭项目
 * @param {
 *  nodeId: 节点 ID,
 *  id: 项目id
 *  copyId: 副本id
 * } params
 */
export function stopProject(params) {
  return axios({
    url: "/node/manage/stop",
    method: "post",
    data: params,
    headers: {
      loading: "no",
      tip: "no",
    },
  });
}

/**
 * 新增目录  或文件
 * @param params
 * @returns {id, path, name,unFolder} params x
 */
export function newFileFolder(params) {
  return axios({
    url: "/node/manage/file/new_file_folder",
    method: "get",
    params,
  });
}

/**
 * 修改目录或文件名称
 * @param params
 * @returns {id, levelName, filename,newname} params x
 */
export function renameFileFolder(params) {
  return axios({
    url: "/node/manage/file/rename_file_folder",
    method: "get",
    params,
  });
}

/**
 * 所有的运行模式
 */
export const runModeList = ["Dsl", "ClassPath", "Jar", "JarWar", "JavaExtDirsCp", "File"];

/**
 * java 项目的运行模式
 */
export const javaModes = ["ClassPath", "Jar", "JarWar", "JavaExtDirsCp"];

/**
 * 有状态管理的运行模式
 */
export const noFileModes = ["ClassPath", "Jar", "JarWar", "JavaExtDirsCp", "Dsl"];
