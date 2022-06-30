import axios from "./config";

// ssh 列表
export function getSshList(params) {
  return axios({
    url: "/node/ssh/list_data.json",
    method: "post",
    data: params,
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

// 检查 ssh 是否安装 插件端
export function getSshCheckAgent(params) {
  return axios({
    url: "/node/ssh/check_agent.json",
    method: "get",
    params: params,
    timeout: 0,
    headers: {
      loading: "no",
    },
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
  const data = {
    type: params.type,
    id: params.id,
    name: params.name,
    host: params.host,
    port: params.port,
    user: params.user,
    password: params.password,
    connectType: params.connectType,
    privateKey: params.privateKey,
    charset: params.charset,
    fileDirs: params.fileDirs,
    notAllowedCommand: params.notAllowedCommand,
    allowEditSuffix: params.allowEditSuffix,
    timeout: params.timeout,
  };
  return axios({
    url: "/node/ssh/save.json",
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data,
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
 * 上传安装文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  nodeData: 节点数据 json 字符串 `{"url":"121.42.160.109:2123","protocol":"http","id":"test","name":"tesst","path":"/seestech"}`,
 *  path: 文件保存的路径
 * } formData
 */
export function installAgentNode(formData) {
  return axios({
    url: "/node/ssh/installAgentSubmit.json",
    headers: {
      // "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}
/**
 *  获取插件端信息
 * @returns json
 */
export function getAgent() {
  return axios({
    url: "/node/ssh/get_agent.json",
    headers: {},
    method: "get",
  });
}

/**
 * 上传插件包
 * @param {form} formData
 * @returns
 */
export function uploadAgent(formData) {
  return axios({
    url: "/node/ssh/upload_agent.json",
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
  return axios({
    url: "/node/ssh/download.html",
    method: "get",
    responseType: "blob",
    timeout: 0,
    params,
  });
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
