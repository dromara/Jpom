import axios, {loadRouterBase} from "./config";

// 命令列表
export function getCommandList(params) {
  return axios({
    url: "/node/ssh_command/list",
    method: "post",
    data: params,
  });
}

// 编辑命令
export function editCommand(params) {
  return axios({
    url: "/node/ssh_command/edit",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data: params,
  });
}

// 删除命令
export function deleteCommand(id) {
  return axios({
    url: "/node/ssh_command/del",
    method: "post",
    data: { id },
  });
}

// 删除命令
export function executeBatch(param) {
  return axios({
    url: "/node/ssh_command/batch",
    method: "post",
    data: param,
  });
}

// 命令日志列表
export function getCommandLogList(params) {
  return axios({
    url: "/node/ssh_command_log/list",
    method: "post",
    data: params,
  });
}

// 命令日志批次列表
export function getCommandLogBarchList(params) {
  return axios({
    url: "/node/ssh_command_log/batch_list",
    method: "get",
    params: params,
  });
}

// 删除命令执行记录
export function deleteCommandLog(id) {
  return axios({
    url: "/node/ssh_command_log/del",
    method: "post",
    data: { id },
  });
}

// 命令日志信息
export function getCommandLogInfo(params) {
  return axios({
    url: "/node/ssh_command_log/log",
    method: "post",
    data: params,
    headers: {
      loading: "no",
    },
  });
}

/**
 * 下载日志
 * @param {*} logId
 */
export function downloadLog(logId) {
  return loadRouterBase("/node/ssh_command_log/download_log", {
    logId: logId,
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/node/ssh_command/sync-to-workspace",
    method: "get",
    params: params,
  });
}

export const statusMap = {
  0: "执行中",
  1: "执行结束",
  2: "执行错误",
  3: "会话异常",
};

export const triggerExecTypeMap = {
  0: "手动",
  1: "自动",
};
