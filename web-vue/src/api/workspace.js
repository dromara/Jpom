import axios from "./config";

/**
 *
 * @param data
 */
export function editWorkSpace(data) {
  return axios({
    url: "/system/workspace/edit",
    method: "post",
    data: data,
  });
}

/*
 * 工作空间列表
 * @param {*}
 * } params
 */
export function getWorkSpaceList(params) {
  return axios({
    url: "/system/workspace/list",
    method: "post",
    data: params,
  });
}

/*
 * 工作空间列表（查询所有)
 * @param {*}
 * } params
 */
export function getWorkSpaceListAll() {
  return axios({
    url: "/system/workspace/list_all",
    method: "get",
    data: {},
  });
}

/*
 * 删除工作空间
 * @param {String} id
 * } params
 */
export function deleteWorkspace(id) {
  return axios({
    url: "/system/workspace/delete",
    method: "get",
    params: { id: id },
  });
}

/*
 * 工作空间环境变量列表
 * @param {*}
 * } params
 */
export function getWorkspaceEnvList(params) {
  return axios({
    url: "/system/workspace_env/list",
    method: "post",
    data: params,
  });
}

/**
 *
 * @param data
 */
export function editWorkspaceEnv(data) {
  return axios({
    url: "/system/workspace_env/edit",
    method: "post",
    data: data,
  });
}

/*
 * 删除工作空间变量
 * @param {String} id
 * } params
 */
export function deleteWorkspaceEnv(params) {
  return axios({
    url: "/system/workspace_env/delete",
    method: "get",
    params: params,
  });
}
