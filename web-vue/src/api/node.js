import axios from './config';

// 分组
export function getNodeGroupList() {
  return axios({
    url: '/node/list_group.json',
    method: 'post'
  })
}

// node 列表
export function getNodeList(params) {
  return axios({
    url: '/node/list_data.json',
    method: 'post',
    data: params
  })
}