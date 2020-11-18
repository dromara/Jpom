import axios from './config';

// 获取系统菜单列表
export function getSystemMenu() {
  return axios({
    url: 'menus_data.json',
    method: 'post'
  })
}

// 节点菜单
export function getNodeMenu(nodeId) {
  return axios({
    url: 'menus_data.json',
    method: 'post',
    data: {nodeId}
  })
}
