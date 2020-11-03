import axios from './config';

// 获取系统菜单列表
export function getSystemMenu() {
  return axios({
    url: 'menus_data.json',
    method: 'post'
  })
}
