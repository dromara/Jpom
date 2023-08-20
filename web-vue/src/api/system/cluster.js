import axios from "@/api/config";

/*
 * 集群列表
 * @param {*}
 * } params
 */
export function getClusterList(params) {
  return axios({
    url: "/cluster/list",
    method: "post",
    data: params,
  });
}

/*
 * 删除集群
 * @param {String} id
 * } params
 */
export function deleteCluster(id) {
  return axios({
    url: "/cluster/delete",
    method: "get",
    params: { id: id },
  });
}

/*
 * 删除所有可用分组
 * @param {} *
 * } params
 */
export function listLinkGroups(params) {
  return axios({
    url: "/cluster/list-link-groups",
    method: "get",
    params: params,
  });
}

export function editCluster(params) {
  return axios({
    url: "/cluster/edit",
    method: "post",
    data: params,
  });
}
