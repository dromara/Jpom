import axios from "@/api/config";

// ssh 列表
export function machineSshListData(params) {
  return axios({
    url: "/system/assets/ssh/list-data",
    method: "post",
    params: params,
  });
}

export function machineSshListGroup(params) {
  return axios({
    url: "/system/assets/ssh/list-group",
    method: "get",
    params: params,
  });
}

// 编辑ssh
export function machineSshEdit(params) {
  return axios({
    url: "/system/assets/ssh/edit",
    method: "post",
    params: params,
  });
}

// 检查 agent
export function machineSshCheckAgent(params) {
  return axios({
    url: "/system/assets/ssh/check-agent",
    method: "get",
    params: params,
    timeout: 0,
    headers: {
      loading: "no",
    },
  });
}

// 删除 ssh
export function machineSshDelete(params) {
  return axios({
    url: "/system/assets/ssh/delete",
    method: "post",
    params: params,
  });
}

// 分配 ssh
export function machineSshDistribute(params) {
  return axios({
    url: "/system/assets/ssh/distribute",
    method: "post",
    params: params,
  });
}

// ssh 操作日志列表
export function getMachineSshOperationLogList(params) {
  return axios({
    url: "/system/assets/ssh/log-list-data",
    method: "post",
    data: params,
  });
}

// ssh 关联工作空间的数据
export function machineListGroupWorkspaceSsh(params) {
  return axios({
    url: "/system/assets/ssh/list-workspace-ssh",
    method: "get",
    params: params,
  });
}

export function machineSshSaveWorkspaceConfig(params) {
  return axios({
    url: "/system/assets/ssh/save-workspace-config",
    method: "post",
    params: params,
  });
}
