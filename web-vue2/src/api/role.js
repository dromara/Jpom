/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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