// import axios from './config';

// // 角色列表
// export function getRoleList() {
//   return axios({
//     url: '/user/role/list_data.json',
//     method: 'post'
//   })
// }

// // 角色权限
// export function getRoleFeature(id) {
//   return axios({
//     url: '/user/role/getFeature.json',
//     method: 'post',
//     data: {id}
//   })
// }

// // 编辑角色
// export function editRole(params) {
//   return axios({
//     url: '/user/role/save.json',
//     method: 'post',
//     data: params
//   })
// }

// // 删除角色
// export function deleteRole(id) {
//   return axios({
//     url: '/user/role/del.json',
//     method: 'post',
//     data: {id}
//   })
// }

// // 角色动态
// export function getDynamicList() {
//   return axios({
//     url: '/user/role/dynamic-list',
//     method: 'get'
//   })
// }

// /**
//  * getDynamic
//  * @param {
//  *  id: 角色 ID
//  *  dynamic: dynamic
//  * } params 
//  */
// export function getRoleDynamicList(params) {
//   return axios({
//     url: '/user/role/getDynamic.json',
//     method: 'post',
//     timeout: 0,
//     data: params
//   })
// }

// /**
//  * editRoleDynamic
//  * @param {
//  *  id: 角色 ID
//  *  dynamic: dynamic
//  * } params 
//  */
// export function editRoleDynamic(params) {
//   return axios({
//     url: '/user/role/saveDynamic.json',
//     method: 'post',
//     timeout: 0,
//     data: params
//   })
// }