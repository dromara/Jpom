import axios from './config';

// 分组
export function getNodeGroupList() {
  return axios({
    url: '/node/list_group.json',
    method: 'get'
  })
}

// node 列表
export function getNodeList(params) {
  return axios({
    url: '/node/list_data.json',
    method: 'get',
    data: params
  })
}

// node 状态
export function getNodeStatus(nodeId) {
  return axios({
    url: '/node/node_status',
    method: 'post',
    data: {nodeId}
  })
}

// 编辑 node
export function editNode(params) {
  return axios({
    url: '/node/save.json',
    method: 'post',
    data: params
  })
}

// 删除 node
export function deleteNode(id) {
  return axios({
    url: '/node/del.json',
    method: 'post',
    data: {id}
  })
}