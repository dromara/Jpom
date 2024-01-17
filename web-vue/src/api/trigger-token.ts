import axios from './config'

export function triggerTokenList(data) {
  return axios({
    url: '/system/trigger/list',
    method: 'post',
    data: data
  })
}

export function triggerTokenAllType(data) {
  return axios({
    url: '/system/trigger/all-type',
    method: 'get',
    params: data
  })
}

export function triggerTokenDelete(data) {
  return axios({
    url: '/system/trigger/delete',
    method: 'get',
    params: data
  })
}
