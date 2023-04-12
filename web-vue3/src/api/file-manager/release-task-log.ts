import axios from '@/api/config'

// 任务列表
export function fileReleaseTaskLog(params: any) {
  return axios({
    url: '/file-storage/release-task/list',
    method: 'post',
    data: params
  })
}

// 添加发布任务
export function addReleaseTask(params: any) {
  return axios({
    url: '/file-storage/release-task/add-task',
    method: 'post',
    data: params
  })
}

// 重新发布任务
export function reReleaseTask(params: any) {
  return axios({
    url: '/file-storage/release-task/re-task',
    method: 'post',
    data: params
  })
}

// 取消任务
export function cancelReleaseTask(params: any) {
  return axios({
    url: '/file-storage/release-task/cancel-task',
    method: 'get',
    params: params
  })
}

// 删除任务
export function deleteReleaseTask(params: any) {
  return axios({
    url: '/file-storage/release-task/delete',
    method: 'get',
    params: params
  })
}

// 任务详情
export function taskDetails(params: any) {
  return axios({
    url: '/file-storage/release-task/details',
    method: 'get',
    params: params
  })
}

export function taskLogInfoList(params: any) {
  return axios({
    url: '/file-storage/release-task/log-list',
    method: 'get',
    params: params,
    headers: {
      loading: 'no'
    }
  })
}

export const statusMap = {
  0: '等待开始',
  1: '进行中',
  2: '任务结束',
  3: '发布失败',
  4: '取消任务'
}

export const taskTypeMap = {
  0: 'SSH',
  1: '节点'
}
