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
