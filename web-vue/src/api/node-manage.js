import axios from './config';

// 节点管理 项目列表
export function getProjectList(params) {
  return axios({
    url: '/node/manage/getProjectInfo',
    method: 'post',
    data: params
  })
}