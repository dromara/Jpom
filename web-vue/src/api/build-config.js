import axios from "./config";

/**
 * 获取基础运行镜像列表
 * @returns {*}
 */
export function getRuns() {
  return axios({
    url: "/build-config/runs",
    method: "get",
  });
}

export function updateRuns(params) {
  return axios({
    url: "/build-config/runs",
    method: "post",
    data: params
  });
}

export function deleteRuns(name) {
  return axios({
    url: "/build-config/runs/" + name,
    method: "delete",
  });
}
