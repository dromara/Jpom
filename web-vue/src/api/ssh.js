import axios from "./config";

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

// 删除 SSH
export function deleteForeSsh(id) {
  return axios({
    url: "/node/ssh/del-fore",
    method: "post",
    data: { id },
  });
}

export function syncToWorkspace(params) {
  return axios({
    url: "/node/ssh/sync-to-workspace",
    method: "get",
    params: params,
  });
}
