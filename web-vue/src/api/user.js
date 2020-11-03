import axios from './config';

// login
export function login(params) {
  return axios({
    url: '/userLogin',
    method: 'post',
    data: params
  })
}

// 修改昵称
export function updatePwd(params) {
  return axios({
    url: '/user/updatePwd',
    method: 'post',
    data: params
  })
}
