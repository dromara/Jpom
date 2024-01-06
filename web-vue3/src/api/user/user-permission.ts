import axios from "../config";

// 权限组列表
export function getList(params) {
  return axios({
    url: "/user-permission-group/get-list",
    method: "post",
    data: params,
  });
}

// 编辑
export function editPermissionGroup(params) {
  return axios({
    url: "/user-permission-group/edit",
    method: "post",
    data: params,
  });
}

// 所有列表
export function getUserPermissionListAll() {
  return axios({
    url: "/user-permission-group/get-list-all",
    method: "get",
  });
}

// 删除
export function deletePermissionGroup(id) {
  return axios({
    url: "/user-permission-group/delete",
    method: "get",
    params: { id },
  });
}
