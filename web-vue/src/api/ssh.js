import axios from './config';

// ssh 列表
export function getSshList() {
  return axios({
    url: '/node/ssh/list_data.json',
    method: 'post'
  })
}

// 新增
export function addSsh(params) {
  return axios({
    url: '/node/ssh/save.json',
    method: 'post',
    data: params
  })
}
