import axios from "./config";

// 日志阅读列表
export function getLogReadList(params) {
  return axios({
    url: "/log-read/list",
    method: "post",
    data: params,
  });
}

/**
 * 编辑日志阅读
 * @param {
 *  id: 监控 ID
 *  name: 监控名称
 *  nodeProject: { nodeId:'',projectId:''}
 *
 * } params
 */
export function editLogRead(params) {
  return axios({
    url: "/log-read/save.json",
    method: "post",
    data: params,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

export function updateCache(params) {
    return axios({
      url: "/log-read/update-cache.json",
      method: "post",
      data: params,
      headers: {
        "Content-Type": "application/json",
      },
    });
  }

/**
 * 删除日志阅读
 * @param {*} id
 */
export function deleteLogRead(id) {
  return axios({
    url: "/log-read/del.json",
    method: "post",
    data: { id },
  });
}
