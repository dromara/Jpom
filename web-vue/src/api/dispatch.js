import axios from "./config";

// 分发列表
export function getDishPatchList(data) {
  return axios({
    url: "/outgiving/dispatch-list",
    method: "post",
    data: data,
  });
}

// 分发列表
export function getDishPatchListAll() {
  return axios({
    url: "/outgiving/dispatch-list-all",
    method: "get",
    
  });
}

// 分发节点项目状态
export function getDispatchProject(id) {
  return axios({
    url: "/outgiving/getItemData.json",
    method: "post",
    data: { id },
  });
}

// reqId
export function getReqId() {
  return axios({
    url: "/outgiving/get-reqId",
    method: "get",
  });
}

/**
 * 编辑分发
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  node_xxx: xxx 表示节点 ID
 *  afterOpt: 发布后操作
 * } params
 */
export function editDispatch(params) {
  return axios({
    url: "/outgiving/save",
    method: "post",
    data: params,
  });
}

/**
 * 编辑分发项目
 * @param {
 *  id: 分发 ID
 *  name: 分发名称
 *  reqId: 请求 ID
 *  type: 类型 add | edit
 *  afterOpt: 发布后操作
 *  runMode: 运行方式
 *  mainClass: 启动类
 *  javaExtDirsCp: 目录地址
 *  whitelistDirectory: 白名单地址
 *  lib: lib
 *  add_xxx: xxx 表示添加的节点信息
 *  xxx_token: xxx 节点的 webhook 地址
 *  xxx_jvm: jvm 参数
 *  xxx_args: args 参数
 *  xxx_javaCopyIds: xxx 节点副本 ID （如果有副本，还需要加上 xxx_jvm_${副本ID} | xxx_args_${副本ID} 参数）
 * } params
 */
export function editDispatchProject(params) {
  return axios({
    url: "/outgiving/save_project",
    method: "post",
    data: params,
  });
}

/**
 * 上传分发文件
 * @param {
 *  id: 分发 ID
 *  afterOpt: 发布后操作
 *  clearOld: 是否清除
 *  file: 文件 multipart/form-data
 * } formData
 */
export function uploadDispatchFile(formData) {
  return axios({
    url: "/outgiving/upload",
    headers: {
      "Content-Type": "multipart/form-data;charset=UTF-8",
    },
    method: "post",
    // 0 表示无超时时间
    timeout: 0,
    data: formData,
  });
}

/**
 * 远程下载文件分发
 * @param {
 *  id: 节点 ID
 *  clearOld:  是否清空
 *  afterOpt: 分发后的操作
 *  url: 远程URL
 *   unzip：是否为压缩包
 * } params
 */
export function remoteDownload(params) {
  return axios({
    url: "/outgiving/remote_download",
    method: "post",
    data: params,
  });
}

/**
 * 删除分发
 * @param {*} id 分发 ID
 */
export function deleteDisPatch(id) {
  return axios({
    url: "/outgiving/del.json",
    method: "post",
    data: { id },
  });
}

/**
 * 分发日志
 * @param {
 *  nodeId: 节点 ID
 *  outGivingId: 分发 ID
 *  status: 分发状态
 *  time: 时间区间 2021-01-04 00:00:00 ~ 2021-01-12 00:00:00
 * } params
 */
export function getDishPatchLogList(params) {
  return axios({
    url: "/outgiving/log_list_data.json",
    method: "post",
    data: params,
  });
}

// 获取分发白名单数据
export function getDispatchWhiteList() {
  return axios({
    url: "/outgiving/white-list",
    method: "post",
  });
}

/**
 * 编辑分发白名单
 * @param {*} params
 */
export function editDispatchWhiteList(params) {
  return axios({
    url: "/outgiving/whitelistDirectory_submit",
    method: "post",
    data: params,
  });
}
