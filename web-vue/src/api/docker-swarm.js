import axios from "./config";

/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerSwarmList(params) {
  return axios({
    url: "/docker-swarm/list",
    method: "post",
    data: params,
  });
}

export function dockerSwarmListAll(params) {
  return axios({
    url: "/docker-swarm/list-all",
    method: "get",
    params: params,
  });
}

export function initDockerSwarm(data) {
  return axios({
    url: "/docker-swarm/init",
    method: "post",
    data: data,
  });
}

export function joinDockerSwarm(data) {
  return axios({
    url: "/docker-swarm/join",
    method: "post",
    data: data,
  });
}

export function editDockerSwarm(data) {
  return axios({
    url: "/docker-swarm/edit",
    method: "post",
    data: data,
  });
}
