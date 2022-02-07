import axios from "./config";

/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerList(params) {
  return axios({
    url: "/docker/list",
    method: "post",
    data: params,
  });
}

/**
 *  获取支持的所有 api 版本
 * @returns json
 */
export function apiVersions() {
  return axios({
    url: "/docker/api-versions",
    method: "get",
    data: {},
  });
}

export function editDockerByFile(formData) {
  return axios({
    url: "/docker/edit",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    timeout: 0,
    data: formData,
  });
}

export function editDocker(data) {
  return axios({
    url: "/docker/edit",
    method: "post",
    data: data,
  });
}

/**
 * 删除 docker
 * @param {
 *  id: docker ID
 * } params
 */
export function deleteDcoker(params) {
  return axios({
    url: "/docker/del",
    method: "get",
    params,
  });
}

/**
 * 容器中的列表
 * @param {JSON} params
 */
export function dockerContainerList(params) {
  return axios({
    url: "/docker/container/list",
    method: "post",
    data: params,
  });
}

/**
 * 删除容器
 * @param {JSON} params
 */
export function dockerContainerRemove(params) {
  return axios({
    url: "/docker/container/remove",
    method: "get",
    params: params,
  });
}

/**
 * 重启容器
 * @param {JSON} params
 */
export function dockerContainerRestart(params) {
  return axios({
    url: "/docker/container/restart",
    method: "get",
    params: params,
  });
}

/**
 * 启动容器
 * @param {JSON} params
 */
export function dockerContainerStart(params) {
  return axios({
    url: "/docker/container/start",
    method: "get",
    params: params,
  });
}

/**
 * 停止容器
 * @param {JSON} params
 */
export function dockerContainerStop(params) {
  return axios({
    url: "/docker/container/stop",
    method: "get",
    params: params,
  });
}
