import axios from './config';

// 角色列表
export function getRoleList() {
  return axios({
    url: '/user/role/list_data.json',
    method: 'post'
  })
}
