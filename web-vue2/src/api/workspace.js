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
 * 工作空间分组列表
 * @param {*}
 * } params
 */
export function getWorkSpaceGroupList(params) {
  return axios({
    url: "/system/workspace/list-group-all",
    method: "get",
    params: params,
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
 * 删除工作空间检查
 * @param {String} id
 * } params
 */
export function preDeleteWorkspace(id) {
  return axios({
    url: "/system/workspace/pre-check-delete",
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
/*
 * 工作空间环境变量全部列表
 * @param {*}
 * } params
 */
export function getWorkspaceEnvAll(data) {
  return axios({
    url: "/system/workspace_env/all",
    method: "post",
    data,
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

export function getTriggerUrlWorkspaceEnv(params) {
  return axios({
    url: "/system/workspace_env/trigger-url",
    method: "post",
    params: params,
  });
}

/**
 * 加载 菜单配置信息
 */
export function getMenusConfig(data) {
  return axios({
    url: "/system/workspace/get_menus_config",
    method: "post",
    data,
  });
}

/**
 * 保存菜单配置信息
 */
export function saveMenusConfig(data) {
  return axios({
    url: "/system/workspace/save_menus_config.json",
    method: "post",
    data: data,
  });
}
