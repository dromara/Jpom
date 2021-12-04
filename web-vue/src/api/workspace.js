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
