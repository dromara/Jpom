import axios from "./config";

/**
 * 分页获取仓库列表
 *
 * @param {Object} params               分页和查询参数
 * @param {Number} params.limit         每页显示条数，默认 10 条
 * @param {Number} params.page          获取第几页的数据，默认第 1 页
 * @param {Number} params.total         总条数，默认 0
 * @param {String} params.order         排序方式[ascend(升序，从小到大), descend(降序，从大到小)]
 * @param {String} params.order_field   需要排序的字段名称
 * @param {Number} params.repoType      查询的仓库类型[0(GIT), 1(SVN)]
 * @param {String} params.name          仓库名称
 * @param {String} params.gitUrl        仓库地址
 * @return {axios} 请求结果 axios 对象
 */
export function getRepositoryList(params) {
  return axios({
    url: "/build/repository/list",
    method: "post",
    data: params,
  });
}

/**
 * 不分页获取所有仓库列表
 *
 * @return {axios} 请求结果 axios 对象
 */
export function getRepositoryListAll() {
  return axios({
    url: "/build/repository/list_all",
    method: "get",
  });
}

/**
 * 编辑仓库信息，新增或者更新
 *
 * @param {Object} params               请求参数
 * @param {String} params.id            仓库id
 * @param {String} params.name          仓库名称
 * @param {String} params.gitUrl        仓库地址
 * @param {Number} params.repoType      仓库类型[0(GIT), 1(SVN)]
 * @param {Number} params.protocol      协议[0(HTTP(S)), 1(SSH)]
 * @param {String} params.userName      用户名
 * @param {String} params.password      密码
 * @param {String} params.rsaPub      公钥信息
 * @return {axios} 请求结果 axios 对象
 */
export function editRepository(params) {
  return axios({
    url: "/build/repository/edit",
    method: "post",
    data: params,
  });
}

/**
 * 根据 仓库id 删除仓库
 *
 * @param {Object}  params              请求参数
 * @param {String}  params.id           仓库id
 * @param {Boolean} params.isRealDel    是否真正删除
 * @return {axios} 请求结果 axios 对象
 */
export function deleteRepository(params) {
  return axios({
    url: "/build/repository/delete",
    method: "post",
    data: params,
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

export function authorizeRepos(param) {
  return axios({
    url: "/build/repository/authorize_repos",
    method: "get",
    params: param,
  });
}
