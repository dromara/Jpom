/**
 * 节点管理 api
 */
import axios from "./config";
import { loadRouterBase } from "./config";

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
export function getRuningProjectInfo(params, noTip) {
  return axios({
    url: "/node/manage/getProjectPort",
    method: "post",
    data: params,
    timeout: 0,
    headers: {
      loading: "no",
      tip: noTip ? "no" : "",
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
 *  ...
 * }

 */
export function editProject(params) {
  const data = {
    nodeId: params.nodeId,
    id: params.id,
    name: params.name,
    group: params.group,
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

 * } params
 */
export function deleteProject(params) {
  return axios({
    url: "/node/manage/deleteProject",
    method: "post",
    data: params,
  });
}

export function migrateWorkspace(params) {
  return axios({
    url: "/node/manage/migrate-workspace",
    method: "post",
    data: params,
  });
}

export function releaseOutgiving(params) {
  return axios({
    url: "/node/manage/release-outgiving",
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
  return loadRouterBase("/node/manage/file/download", params);
  // return axios({
  //   url: "/node/manage/file/download",
  //   method: "get",
  //   responseType: "blob",
  //   timeout: 0,
  //   params,
  // });
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
    timeout: 0,
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
    url: "/node/manage/file/upload-sharding",
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

/**
 * 合并分片项目文件
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  levelName: 目录地址
 *  type: unzip 表示压缩文件 *上传压缩文件时需要
 *  clearType: {clear: 清空文件夹, noClear: 不清空} *上传压缩文件时需要
 * } formData
 */
export function shardingMerge(formData) {
  return axios({
    url: "/node/manage/file/sharding-merge",
    headers: {},
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
 * 获取项目日志文件大小
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID

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

 * } params
 */
export function downloadProjectLogFile(params) {
  return loadRouterBase("/node/manage/log/export.html", params);
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

 *  key: 文件名
 * } params
 */
export function downloadProjectLogBackFile(params) {
  return loadRouterBase("/node/manage/log/logBack_download", params);
}

/**
 * 项目日志备份文件删除
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID

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

// /**
//  * 查看线程
//  * @param {
//  *  nodeId: 节点 ID
//  * } params
//  */
// export function getThreadInfo(params) {
//   return axios({
//     url: "/node/manage/threadInfos",
//     method: "post",
//     timeout: 0,
//     data: params,
//   });
// }

// /**
//  * 导出堆栈信息
//  * @param {
//  *  nodeId: 节点 ID
//  *  tag: 项目 ID
//  * } params
//  */
// export function exportStack(params) {
//   return axios({
//     url: "/node/manage/stack",
//     method: "get",
//     responseType: "blob",
//     timeout: 0,
//     params,
//   });
// }

// /**
//  * 导出内存信息
//  * @param {
//  *  nodeId: 节点 ID
//  *  tag: 项目 ID
//  * } params
//  */
// export function exportRam(params) {
//   return axios({
//     url: "/node/manage/ram",
//     method: "get",
//     responseType: "blob",
//     timeout: 0,
//     params,
//   });
// }

// /**
//  * 查询节点目录是否存在
//  * @param {
//  *  nodeId: 节点 ID,
//  *  newLib: 新目录地址
//  * } params
//  */
// export function nodeJudgeLibExist(params) {
//   return axios({
//     url: "/node/manage/judge_lib.json",
//     method: "post",
//     data: params,
//     headers: {
//       tip: "no",
//     },
//   });
// }

/**
 * 重启项目
 * @param {
 *  nodeId: 节点 ID,
 *  id: 项目id
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
 * 获取触发器地址
 * @param {*} id
 */
export function getProjectTriggerUrl(data) {
  return axios({
    url: "/node/project-trigger-url",
    method: "post",
    data: data,
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
 * 构建分组
 */
export function getProjectGroupAll() {
  return axios({
    url: "/node/list-project-group-all",
    method: "get",
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

/*
 * 下载导入模板
 *
 */
export function importTemplate(data) {
  return loadRouterBase("/node/manage/import-template", data);
}

/*
 * 导出数据
 *
 */
export function exportData(data) {
  return loadRouterBase("/node/manage/export-data", data);
}
// 导入数据
export function importData(formData) {
  return axios({
    url: "/node/manage/import-data",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}
