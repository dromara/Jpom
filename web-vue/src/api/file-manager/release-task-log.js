import axios from "@/api/config";

// 任务列表
export function fileReleaseTaskLog(params) {
  return axios({
    url: "/file-storage/release-task/list",
    method: "post",
    data: params,
  });
}

// 添加发布任务
export function addReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/add-task",
    method: "post",
    data: params,
  });
}

// 取消任务
export function cancelReleaseTask(params) {
  return axios({
    url: "/file-storage/release-task/cancel-task",
    method: "get",
    params: params,
  });
}

// 任务详情
export function taskDetails(params) {
  return axios({
    url: "/file-storage/release-task/details",
    method: "get",
    params: params,
  });
}

export function taskLogInfoList(params) {
  return axios({
    url: "/file-storage/release-task/log-list",
    method: "get",
    params: params,
  });
}

export const statusMap = {
  0: "等待开始",
  1: "进行中",
  2: "任务结束",
  3: "发布失败",
  4: "取消任务",
};

export const taskTypeMap = {
  0: "SSH",
  1: "节点",
};
