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
    params: params
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
  const data = {
    id: params.id,
    name: params.name,
    group: params.group,
    protocol: params.protocol,
    url: params.url,
    timeOut: params.timeOut,
    cycle: params.cycle,
    openStatus: params.openStatus,
    loginName: params.loginName,
    loginPwd: params.loginPwd,
    type: params.type
  }
  return axios({
    url: '/node/save.json',
    method: 'post',
    data
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

// 节点 top 命令
export function getNodeTop(nodeId) {
  return axios({
    url: '/node/getTop',
    method: 'post',
    data: {nodeId}
  })
}

// 获取进程列表
export function getProcessList(nodeId) {
  return axios({
    url: '/node/processList',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 杀掉进程
 * @param {*} params = {nodeId, pid}
 */
export function killPid(params) {
  return axios({
    url: '/node/kill.json',
    method: 'post',
    data: params
  })
}