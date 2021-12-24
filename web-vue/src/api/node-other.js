import axios from "./config";

/**
 * tomcat 列表
 * @param {String} nodeId 节点 ID
 */
export function getTomcatList(nodeId) {
  return axios({
    url: "/node/tomcat/list",
    method: "post",
    data: { nodeId },
  });
}

/**
 * Tomcat 编辑
 * @param {nodeId, id, name, path, port, appBase} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.path jdk 路径
 * params.port 端口
 * params.appBase appBase 路径
 */
export function editTomcat(params) {
  return axios({
    url: "/node/tomcat/save",
    method: "post",
    data: params,
  });
}

/**
 * 删除 Tomcat
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function deleteTomcat(params) {
  return axios({
    url: "/node/tomcat/delete",
    method: "post",
    data: params,
  });
}

/**
 * 上传 Tomcat WAR 项目文件
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 *  id: Tomcat ID
 * } formData
 */
export function uploadTomcatWarFile(formData) {
  return axios({
    url: "/node/tomcat/uploadWar",
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
 * 查询项目列表
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function getTomcatProjectList(params) {
  return axios({
    url: "/node/tomcat/getTomcatProject",
    method: "post",
    data: params,
  });
}

/**
 * 查询 tomcat 状态
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function getTomcatStatus(params) {
  return axios({
    url: "/node/tomcat/getTomcatStatus",
    method: "post",
    data: params,
  });
}

/**
 * tomcat 日志列表
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function getTomcatLogList(params) {
  return axios({
    url: "/node/tomcat/getLogList",
    method: "post",
    data: params,
  });
}

/**
 * 删除 Tomcat 日志
 * @param {nodeId, path, filename, id} params
 * params.nodeId 节点 ID
 * params.path tomcat 日志目录
 * params.filename 日志名称
 * params.id 编辑修改时判断 ID
 */
export function deleteTomcatFile(params) {
  return axios({
    url: "/node/tomcat/deleteFile",
    method: "post",
    data: params,
  });
}

/**
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path, filename, id} params
 * params.nodeId 节点 ID
 * params.path tomcat 日志目录
 * params.filename 日志名称
 * params.id 编辑修改时判断 ID
 */
export function downloadTomcatFile(params) {
  return axios({
    url: "/node/tomcat/download",
    method: "get",
    responseType: "blob",
    params,
  });
}

/**
 * 启动 Tomcat
 * @param {
 *  nodeId: 节点 ID
 *  id: Tomcat ID
 * } params
 */
export function startTomcat(params) {
  return axios({
    url: "/node/tomcat/start",
    method: "post",
    data: params,
  });
}

/**
 * 停止 Tomcat
 * @param {
 *  nodeId: 节点 ID
 *  id: Tomcat ID
 * } params
 */
export function stopTomcat(params) {
  return axios({
    url: "/node/tomcat/stop",
    method: "post",
    data: params,
  });
}

/**
 * 重启 Tomcat
 * @param {
 *  nodeId: 节点 ID
 *  id: Tomcat ID
 * } params
 */
export function restartTomcat(params) {
  return axios({
    url: "/node/tomcat/restart",
    method: "post",
    data: params,
  });
}

/**
 * Tomcat 项目命令操作
 * @param {
 *  nodeId: 节点 ID
 *  id: Tomcat ID
 *  path: 项目目录
 *  op: 操作符
 * } params
 */
export function doTomcatProjectCommand(params) {
  return axios({
    url: "/node/tomcat/tomcatProjectManage",
    method: "post",
    data: params,
  });
}

/**
 * Tomcat 项目文件列表
 * @param {
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  path: Tomcat 项目目录
 *  except: dir 固定值
 * } params
 */
export function getTomcatFileList(params) {
  return axios({
    url: "/node/tomcat/getFileList",
    method: "post",
    data: params,
  });
}

/**
 * 上传 Tomcat 项目文件
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 *  id: 项目 ID
 *  path: 目录地址
 * } formData
 */
export function uploadTomcatProjectFile(formData) {
  return axios({
    url: "/node/tomcat/upload",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

/************************** */

/**
 * script 列表
 * @param {String} nodeId 节点 ID
 */
export function getScriptList(params) {
  return axios({
    url: "/node/script/list",
    method: "post",
    data: params,
  });
}

/**
 * script 服务端中的所有列表
 */
export function getScriptListAll(params) {
  return axios({
    url: "/node/script/list_all",
    method: "post",
    data: params,
  });
}

// 脚本模版日志列表
export function getScriptLogList(params) {
  return axios({
    url: "/node/script_log/list",
    method: "post",
    data: params,
  });
}

// 删除执行记录
export function scriptDel(params) {
  return axios({
    url: "/node/script_log/del",
    method: "post",
    data: params,
  });
}

//执行记录 详情
export function scriptLog(params) {
  return axios({
    url: "/node/script_log/log",
    method: "post",
    data: params,
    headers: {
      tip: "no",
    },
  });
}

/**
 * Script 编辑
 * @param {nodeId, id, name, path, port, appBase} params
 * params.type: add 表示添加
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 * params.name 名称
 * params.context 内容
 */
export function editScript(params) {
  return axios({
    url: "/node/script/save.json",
    method: "post",
    data: params,
  });
}

export function itemScript(params) {
  return axios({
    url: "/node/script/item.json",
    method: "get",
    params: params,
  });
}

export function syncScript(params) {
  return axios({
    url: "/node/script/sync",
    method: "get",
    params: params,
  });
}

/**
 * 删除 Script
 * @param {nodeId, id} params
 * params.nodeId 节点 ID
 * params.id 编辑修改时判断 ID
 */
export function deleteScript(params) {
  return axios({
    url: "/node/script/del.json",
    method: "post",
    data: params,
  });
}

// 删除节点脚本模版缓存
export function delAllCache() {
  return axios({
    url: "/node/script/clear_all",
    method: "get",
    params: {},
  });
}

/**
 * 上传 Script 文件
 * @param {
 *  file: 文件 multipart/form-data
 *  nodeId: 节点 ID
 * } formData
 */
export function uploadScriptFile(formData) {
  return axios({
    url: "/node/script/upload",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}
