import axios from './config';

// login
export function login(params) {
  return axios({
    url: '/userLogin',
    method: 'post',
    data: params
  })
}

// 修改密码
export function updatePwd(params) {
  return axios({
    url: '/user/updatePwd',
    method: 'post',
    data: params
  })
}

// 用户列表
export function getUserList() {
  return axios({
    url: '/user/getUserList',
    method: 'post'
  })
}

// 添加用户
export function addUser(params) {
  return axios({
    url: '/user/addUser',
    method: 'post',
    data: params
  })
}

// 修改用户
export function updateUser(params) {
  return axios({
    url: '/user/updateUser',
    method: 'post',
    data: params
  })
}

// 删除用户
export function deleteUser(id) {
  return axios({
    url: '/user/deleteUser',
    method: 'post',
    data: {id}
  })
}