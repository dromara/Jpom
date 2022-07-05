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
 * 强制退出集群
 * @param {
 *  id: docker ID
 * } params
 */
export function dcokerSwarmLeaveForce(params) {
  return axios({
    url: "/docker/swarm-leave-force",
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
    headers: {
      loading: "no",
    },
  });
}

/**
 * 查看 docker info
 * @param {JSON} params
 */
export function dockerInfo(params) {
  return axios({
    url: "/docker/info",
    method: "get",
    params: params,
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

/**
 * 获取容器统计信息
 * @param {JSON} params
 */
export function dockerContainerStats(params) {
  return axios({
    url: "/docker/container/stats",
    method: "get",
    params: params,
    headers: {
      // tip: "no",
      loading: "no",
    },
  });
}

/**
 * 获取容器信息
 * @param {JSON} params
 */
export function dockerInspectContainer(params) {
  return axios({
    url: "/docker/container/inspect-container",
    method: "get",
    params: params,
  });
}

/**
 * 更新容器
 * @param {JSON} params
 * @returns
 */
export function dockerUpdateContainer(params) {
  return axios({
    url: "/docker/container/update-container",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data: params,
  });
}

/**
 * 容器中的镜像列表
 * @param {JSON} params
 */
export function dockerImagesList(params) {
  return axios({
    url: "/docker/images/list",
    method: "post",
    data: params,
  });
}

/**
 * 删除镜像
 * @param {JSON} params
 */
export function dockerImageRemove(params) {
  return axios({
    url: "/docker/images/remove",
    method: "get",
    params: params,
  });
}

/**
 * inspect 镜像
 * @param {JSON} params
 */
export function dockerImageInspect(params) {
  return axios({
    url: "/docker/images/inspect",
    method: "get",
    params: params,
  });
}

/**
 * 镜像 创建容器
 * @param {JSON} params
 */
export function dockerImageCreateContainer(params) {
  return axios({
    url: "/docker/images/create-container",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data: params,
  });
}

/**
 * 拉取镜像
 * @param {JSON} params
 */
export function dockerImagePullImage(params) {
  return axios({
    url: "/docker/images/pull-image",
    method: "get",
    params: params,
  });
}

/**
 * 拉取镜像日志
 * @param {JSON} params
 */
export function dockerImagePullImageLog(params) {
  return axios({
    url: "/docker/images/pull-image-log",
    method: "get",
    params: params,
    headers: {
      // tip: "no",
      loading: "no",
    },
  });
}

/**
 * 卷
 * @param {JSON} params
 */
export function dockerVolumesList(params) {
  return axios({
    url: "/docker/volumes/list",
    method: "post",
    data: params,
  });
}

/**
 * 删除卷
 * @param {JSON} params
 */
export function dockerVolumesRemove(params) {
  return axios({
    url: "/docker/volumes/remove",
    method: "get",
    params: params,
  });
}

/**
 * 网络
 * @param {JSON} params
 */
export function dockerNetworksList(params) {
  return axios({
    url: "/docker/networks/list",
    method: "post",
    data: params,
  });
}
