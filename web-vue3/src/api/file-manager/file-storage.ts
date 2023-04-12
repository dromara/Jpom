import axios from '@/api/config'

// 文件列表
export function fileStorageList(params: any) {
  return axios({
    url: '/file-storage/list',
    method: 'post',
    data: params
  })
}

export function uploadFile(formData) {
  return axios({
    url: '/file-storage/upload-sharding',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8',
      loading: 'no'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

export function uploadFileMerge(params: any) {
  return axios({
    url: '/file-storage/upload-sharding-merge',
    method: 'post',
    data: params,
    // 0 表示无超时时间
    timeout: 0
  })
}

// 修改文件
export function fileEdit(params: any) {
  return axios({
    url: '/file-storage/edit',
    method: 'post',
    data: params
  })
}

// 下载远程文件
export function remoteDownload(params: any) {
  return axios({
    url: '/file-storage/remote-download',
    method: 'post',
    data: params
  })
}

// 判断文件是否存在
export function hasFile(params: any) {
  return axios({
    url: '/file-storage/has-file',
    method: 'get',
    params: params
  })
}

// 判断文件是否存在
export function delFile(params: any) {
  return axios({
    url: '/file-storage/del',
    method: 'get',
    params: params
  })
}

// 下载 url
export function triggerUrl(params: any) {
  return axios({
    url: '/file-storage/trigger-url',
    method: 'get',
    params: params
  })
}

export const sourceMap = {
  0: '上传',
  1: '构建',
  2: '下载',
  3: '证书'
}

export const statusMap = {
  0: '下载中',
  1: '下载成功',
  2: '下载异常'
}
