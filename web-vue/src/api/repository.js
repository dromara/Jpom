import axios from "./config";

/**
 * 仓库列表
 * @param {
 *  name: 仓库名称
 * } params
 */
export function getRepositoryList(params) {
  return axios({
    url: "/build/repository/list",
    method: "post",
    data: params,
  });
}

/**
 * 编辑仓库信息，新增或者删除
 * @param {
 *  id: id
 *  name: 仓库名称
 *  gitUrl: 仓库地址
 *  repoType: 仓库类型 {0: GIT, 1: SVN}
 *  protocol: 协议 {0: HTTP(S), 1: SSH}
 *  userName: 用户名
 *  password: 密码
 *  rsaPub: 公钥信息
 * } params
 * @returns
 */
export function editRepository(params) {
  return axios({
    url: "/build/repository/edit",
    method: "post",
    data: params,
  });
}

/**
 * delete by id
@param {
  *  id: id
  *  isRealDel: 是否真删
  * } params 
 * @returns 
 */
export function deleteRepository(params) {
  return axios({
    url: "/build/repository/delete",
    method: "post",
    data: params,
  });
}
/**
 * delete by id
 * @param {String} id
 * @returns
 */
export function recoveryRepository(id) {
  return axios({
    url: "/build/repository/recovery",
    method: "post",
    data: { id },
  });
}

/**
 * restHideField by id
 * @param {String} id
 * @returns
 */
export function restHideField(id) {
  return axios({
    url: "/build/repository/rest_hide_field",
    method: "post",
    data: { id },
  });
}
