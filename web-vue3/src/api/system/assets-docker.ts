import axios from "@/api/config";

/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerList(params) {
  return axios({
    url: "/system/assets/docker/list-data",
    method: "post",
    data: params,
  });
}

export function dockerImportTls(formData) {
  return axios({
    url: "/system/assets/docker/import-tls",
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
    url: "/system/assets/docker/edit",
    method: "post",
    data: data,
  });
}

/**
 * 自动探测 docker
 * @param {
 *
 * } params
 */
export function tryLocalDocker(params) {
  return axios({
    url: "/system/assets/docker/try-local-docker",
    method: "get",
    params,
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
    url: "/system/assets/docker/del",
    method: "get",
    params,
  });
}

export function initDockerSwarm(data) {
  return axios({
    url: "/system/assets/docker/init",
    method: "post",
    data: data,
  });
}

export function joinDockerSwarm(data) {
  return axios({
    url: "/system/assets/docker/join",
    method: "post",
    data: data,
  });
}

export function dockerSwarmListAll(params) {
  return axios({
    url: "/system/assets/docker/swarm/list-all",
    method: "get",
    params: params,
  });
}

/**
 * 强制退出集群
 * @param {
 *  id: docker ID
 * } params
 */
export function dcokerSwarmLeaveForce(params) {
  return axios({
    url: "/system/assets/docker/leave-force",
    method: "get",
    params,
  });
}

/**
 * 容器集群节点剔除
 * @param {JSON} params
 */
export function dockerSwarmNodeLeave(params) {
  return axios({
    url: "/system/assets/docker/leave-node",
    method: "get",
    params: params,
  });
}

export function machineDockerDistribute(params) {
  return axios({
    url: "/system/assets/docker/distribute",
    method: "post",
    params: params,
  });
}

export function dockerListWorkspace(params) {
  return axios({
    url: "/system/assets/docker/list-workspace-docker",
    method: "get",
    params: params,
  });
}
