import axios from './config'

/**
 * 获取基础运行镜像列表
 * @returns {*}
 */
export function listExtConf() {
  return axios({
    url: '/system/ext-conf/list',
    method: 'get'
  })
}

export function getItem(params: any) {
  return axios({
    url: '/system/ext-conf/get-item',
    method: 'get',
    params: params
  })
}

export function getDefaultItem(params: any) {
  return axios({
    url: '/system/ext-conf/get-default-item',
    method: 'get',
    params: params
  })
}

export function saveItem(params: any) {
  return axios({
    url: '/system/ext-conf/save-item',
    method: 'post',
    data: params
  })
}

export function addItem(params: any) {
  return axios({
    url: '/system/ext-conf/add-item',
    method: 'get',
    params: params
  })
}
