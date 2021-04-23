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
// 节点和版本信息
export function getNodeListWithVersion(params) {
  return axios({
    url: '/node/list_data_with_version',
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

// 节点 + 项目列表
export function getNodeProjectList() {
  return axios({
    url: '/node/node_project_list',
    method: 'post',
    timeout: 0
  })
}

/**
 * 编辑 node
 * @param {
 *  id: ID,
 *  name: 节点名称,
 *  group: 分组名称,
 *  sshId: SSH ID,
 *  protocol: 协议 HTTPS || HTTP,
 *  url: URL 地址,
 *  timeOut: 超时时间,
 *  cycle: 监控周期,
 *  openStatus: 状态,
 *  loginName: 用户名,
 *  loginPwd: 密码,
 *  type: 操作类型 add || update
 * } params 
 */
export function editNode(params) {
  const data = {
    id: params.id,
    name: params.name,
    group: params.group,
    sshId: params.sshId,
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
    data: {nodeId},
    headers: {
      loading: 'no'
    }
  })
}

// 获取进程列表
export function getProcessList(nodeId) {
  return axios({
    url: '/node/processList',
    method: 'post',
    data: {nodeId},
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 杀掉进程
 * @param {nodeId, pid} params
 */
export function killPid(params) {
  return axios({
    url: '/node/kill.json',
    method: 'post',
    data: params
  })
}

/**
 * 节点监控图表数据
 * @param {
 *  nodeId: 节点 ID,
 *  time: 时间段，格式：yyyy-MM-dd HH:mm:ss ~ yyyy-MM-dd HH:mm:ss
 * } params 
 */
export function nodeMonitorData(params) {
  return axios({
    url: '/node/nodeMonitor_data.json',
    method: 'post',
    data: params
  })
}



/**
 * 上传升级文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  nodeId: 节点 ID
 * } formData
 */
export function uploadAgentFile(formData) {
  return axios({
    url: '/node/upload_agent',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}