import axios from './config';

// 监控列表
export function getMonitorList(params) {
  return axios({
    url: '/monitor/getMonitorList',
    method: 'post',
    data: params
  })
}

/**
 * 编辑监控
 * @param {
 *  id: 监控 ID
 *  name: 监控名称
 *  status: 状态
 *  autoRestart: 是否自动重启
 *  cycle: 监控周期
 *  projects: 监控项目
 *  notifyUser: 报警联系人
 * } params 
 */
export function editMonitor(params) {
  return axios({
    url: '/monitor/updateMonitor',
    method: 'post',
    data: params
  })
}

/**
 * 修改监控状态
 * @param {
 *  id: 监控 ID
 *  status: 状态 true | false
 *  type: 状态类型 status | restart
 * } params 
 */
export function changeMonitorStatus(params) {
  return axios({
    url: '/monitor/changeStatus',
    method: 'post',
    data: params
  })
}

/**
 * 删除监控
 * @param {*} id 
 */
export function deleteMonitor(id) {
  return axios({
    url: '/monitor/deleteMonitor',
    method: 'post',
    data: {id}
  })
}