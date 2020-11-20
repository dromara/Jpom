/**
 * 节点管理 api
 */
import axios from './config';

// 项目列表
export function getProjectList(params) {
  return axios({
    url: '/node/manage/getProjectInfo',
    method: 'post',
    data: params
  })
}


// jdk 列表
export function getJdkList(params) {
  return axios({
    url: '/node/manage/jdk/list',
    method: 'post',
    data: params
  })
}