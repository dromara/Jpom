import axios from './config';

// login
export function login(params) {
  return axios({
    url: '/userLogin',
    method: 'post',
    data: params
  })
}

// refresh token
export function refreshToken() {
  return axios({
    url: '/renewal',
    method: 'post'
  })
}

// 获取用户信息
export function getUserInfo() {
  return axios({
    url: '/user/user-basic-info',
    method: 'post'
  })
}

// 退出登录
export function loginOut(params) {
  return axios({
    url: '/logout2',
    method: 'get',
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

// 所有管理员列表
export function getAdminUserList() {
  return axios({
    url: '/user/get-admin-user-list',
    method: 'post'
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

/**
 * 编辑用户资料
 * @param {
 *  token: token,
 *  email: 邮箱地址,
 *  code: 邮箱验证码
 *  dingDing: 钉钉群通知地址,
 *  workWx: 企业微信群通知地址
 * } params 
 */
export function editUserInfo(params) {
  return axios({
    url: '/user/save_basicInfo.json',
    method: 'post',
    data: params
  })
}

/**
 * 发送邮箱验证码
 * @param {String} email 邮箱地址
 */
export function sendEmailCode(email) {
  return axios({
    url: '/user/sendCode.json',
    method: 'post',
    timeout: 0,
    data: {email}
  })
}

/**
 * 解锁管理员
 * @param {String} id 管理员 ID 
 * @returns 
 */
export function unlockUser(id) {
  return axios({
    url: '/user/unlock',
    method: 'post',
    data: {id}
  })
}