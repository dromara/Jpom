import axios from "./config";

// node 列表
export function getNodeList(params) {
  return axios({
    url: "/node/list_data.json",
    method: "post",
    params: params,
  });
}

// node 列表 all
export function getNodeListAll() {
  return axios({
    url: "/node/list_data_all.json",
    method: "get",
  });
}

// node 列表 根据 工作空间ID
export function getNodeListByWorkspace(params) {
  return axios({
    url: "/node/list_data_by_workspace_id.json",
    method: "get",
    params: params,
  });
}

// node group all
export function getNodeGroupAll() {
  return axios({
    url: "/node/list_group_all.json",
    method: "get",
  });
}

// 节点和版本信息
export function getNodeListWithVersion(params) {
  return axios({
    url: "/node/list_data_with_version",
    method: "get",
    params: params,
  });
}

// node 状态
export function getNodeStatus(nodeId) {
  return axios({
    url: "/node/node_status",
    method: "post",
    data: { nodeId },
  });
}

// 节点 + 项目列表
export function getNodeProjectList(params) {
  return axios({
    url: "/node/node_project_list",
    method: "post",
    params: params,
  });
}

// 节点 + 项目列表
export function getProjectList(params) {
  return axios({
    url: "/node/project_list",
    method: "post",
    params: params,
  });
}

// 节点 + 项目列表
export function getProjectListAll() {
  return axios({
    url: "/node/project_list_all",
    method: "get",
    params: {},
  });
}

// 同步节点项目
export function syncProject(nodeId) {
  return axios({
    url: "/node/sync_project",
    method: "get",
    params: { nodeId: nodeId },
  });
}

export function unLockWorkspace(params) {
  return axios({
    url: "/node/un_lock_workspace",
    method: "get",
    params: params,
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/node/sync-to-workspace",
    method: "get",
    params: params,
  });
}

// 删除节点项目缓存
export function delAllProjectCache() {
  return axios({
    url: "/node/clear_all_project",
    method: "get",
    params: {},
  });
}

/**
 * 编辑 node
 * @param {
 *  id: ID,
 *  name: 节点名称,
 *  group: 分组名称,
 *  sshId: SSH ID,
 *  protocol: 协议 HTTPS || HTTP,
 *  url: URL 地址,
 *  timeOut: 超时时间,
 *  cycle: 监控周期,
 *  openStatus: 状态,
 *  loginName: 用户名,
 *  loginPwd: 密码,
 *  type: 操作类型 add || update
 * } params
 */
export function editNode(params) {
  const data = {
    id: params.id,
    name: params.name,
    group: params.group,
    sshId: params.sshId,
    protocol: params.protocol,
    url: params.url,
    timeOut: params.timeOut,
    cycle: params.cycle,
    openStatus: params.openStatus,
    loginName: params.loginName,
    loginPwd: params.loginPwd,
    type: params.type,
    httpProxy: params.httpProxy,
    httpProxyType: params.httpProxyType,
  };
  return axios({
    url: "/node/save.json",
    method: "post",
    data,
  });
}

// 删除 node
export function deleteNode(id) {
  return axios({
    url: "/node/del.json",
    method: "post",
    data: { id },
  });
}

// 解绑 node
export function unbind(id) {
  return axios({
    url: "/node/unbind.json",
    method: "get",
    params: { id },
  });
}

// // 节点 top 命令
// export function getNodeTop(nodeId) {
//   return axios({
//     url: "/node/getTop",
//     method: "post",
//     data: { nodeId },
//     headers: {
//       loading: "no",
//     },
//   });
// }

// 获取进程列表
export function getProcessList(data) {
  return axios({
    url: "/node/processList",
    method: "post",
    data: data,
    headers: {
      loading: "no",
    },
  });
}

/**
 * 杀掉进程
 * @param {nodeId, pid} params
 */
export function killPid(params) {
  return axios({
    url: "/node/kill.json",
    method: "post",
    data: params,
  });
}

/**
 * 节点监控图表数据
 * @param {
 *  nodeId: 节点 ID,
 *  time: 时间段，格式：yyyy-MM-dd HH:mm:ss ~ yyyy-MM-dd HH:mm:ss
 * } params
 */
export function nodeMonitorData(params) {
  return axios({
    url: "/node/node_monitor_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 上传升级文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  nodeId: 节点 ID
 * } formData
 */
export function uploadAgentFile(formData) {
  return axios({
    url: "/node/upload_agent",
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
 *  检查远程最新
 * @returns json
 */
export function checkVersion() {
  return axios({
    url: "/node/check_version.json",
    method: "get",
    data: {},
  });
}

/**
 * 快速安装
 * @returns1
 */
export function fastInstall() {
  return axios({
    url: "/node/fast_install.json",
    method: "get",
    data: {},
  });
}

/**
 * 拉取结果
 * @param {JSON} params
 * @returns
 */
export function pullFastInstallResult(params) {
  return axios({
    url: "/node/pull_fast_install_result.json",
    method: "get",
    params: params,
    headers: {
      loading: "no",
    },
  });
}

/**
 * 安装确认
 * @param {Json} params
 * @returns
 */
export function confirmFastInstall(params) {
  return axios({
    url: "/node/confirm_fast_install.json",
    method: "get",
    params: params,
  });
}

/**
 * 下载远程文件
 * @returns json
 */
export function downloadRemote() {
  return axios({
    url: "/node/download_remote.json",
    method: "get",
    // 0 表示无超时时间
    timeout: 0,
    data: {},
  });
}
