import axios from './config';

/**
 * 仓库列表
 * @param {
 *  name: 仓库名称
 * } params
 */
export function getRepositoryList(params) {
  return axios({
    url: '/build/repository/list',
    method: 'post',
    data: params
  })
}
