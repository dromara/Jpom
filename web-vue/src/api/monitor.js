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