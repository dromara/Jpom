import axios from "@/api/config";

// 机器 列表
export function machineListData(params) {
  return axios({
    url: "/system/assets/machine/list-data",
    method: "post",
    params: params,
  });
}

export function machineListGroup(params) {
  return axios({
    url: "/system/assets/machine/list-group",
    method: "get",
    params: params,
  });
}

// 编辑机器
export function machineEdit(params) {
  return axios({
    url: "/system/assets/machine/edit",
    method: "post",
    params: params,
  });
}

// 删除机器
export function machineDelete(params) {
  return axios({
    url: "/system/assets/machine/delete",
    method: "post",
    params: params,
  });
}

// 分配机器
export function machineDistribute(params) {
  return axios({
    url: "/system/assets/machine/distribute",
    method: "post",
    params: params,
  });
}

export const statusMap = {
  0: "无法连接",
  1: "正常",
  2: "授权信息错误",
  3: "状态码错误",
};

// 查看机器关联节点
export function machineListNode(params) {
  return axios({
    url: "/system/assets/machine/list-node",
    method: "get",
    params: params,
  });
}
