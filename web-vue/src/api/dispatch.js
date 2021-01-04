import axios from './config';




// 获取分发白名单数据
export function getDispatchWhiteList() {
  return axios({
    url: '/outgiving/white-list',
    method: 'post'
  })
}

/**
 * 编辑分发白名单
 * @param {*} params 
 */
export function editDispatchWhiteList(params) {
  return axios({
    url: '/outgiving/whitelistDirectory_submit',
    method: 'post',
    data: params
  })
}