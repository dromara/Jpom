import axios from './config'
import { loadRouterBase } from './config'

/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerList(params) {
  return axios({
    url: '/docker/list',
    method: 'post',
    data: params
  })
}

/**
 *  获取支持的所有 api 版本
 * @returns json
 */
export function apiVersions() {
  return axios({
    url: '/docker/api-versions',
    method: 'get',
    data: {}
  })
}

export function editDocker(data) {
  return axios({
    url: '/docker/edit',
    method: 'post',
    data: data
  })
}

/**
 * 删除 docker
 * @param {
 *  id: docker ID
 * } params
 */
export function deleteDcoker(params) {
  return axios({
    url: '/docker/del',
    method: 'get',
    params
  })
}

/**
 * 容器中的列表
 * @param {JSON} params
 */
export function dockerContainerList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器中的列表
 * @param {JSON} params
 */
export function dockerContainerListCompose(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/list-compose',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 查看 docker info
 * @param {JSON} params
 */
export function dockerInfo(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/info',
    method: 'get',
    params: params
  })
}

/**
 * 修剪 docker
 * @param {JSON} params
 */
export function dockerPrune(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/prune',
    method: 'post',
    data: params,
    timeout: 0
  })
}

/**
 * 删除容器
 * @param {JSON} params
 */
export function dockerContainerRemove(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/remove',
    method: 'get',
    params: params
  })
}

/**
 * 重启容器
 * @param {JSON} params
 */
export function dockerContainerRestart(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/restart',
    method: 'get',
    params: params
  })
}

/**
 * 启动容器
 * @param {JSON} params
 */
export function dockerContainerStart(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/start',
    method: 'get',
    params: params
  })
}

/**
 * 停止容器
 * @param {JSON} params
 */
export function dockerContainerStop(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/stop',
    method: 'get',
    params: params
  })
}

/**
 * 获取容器统计信息
 * @param {JSON} params
 */
export function dockerContainerStats(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/stats',
    method: 'get',
    params: params,
    headers: {
      // tip: "no",
      loading: 'no'
    }
  })
}

/**
 * 获取容器信息
 * @param {JSON} params
 */
export function dockerInspectContainer(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/inspect-container',
    method: 'get',
    params: params
  })
}

/**
 * 更新容器
 * @param {JSON} params
 * @returns
 */
export function dockerUpdateContainer(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/update-container',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: params
  })
}

export function dockerContainerDownloaLog(urlPrefix, id) {
  return loadRouterBase(urlPrefix + '/container/download-log', {
    id: id
  })
}

/**
 * 容器中的镜像列表
 * @param {JSON} params
 */
export function dockerImagesList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/list',
    method: 'post',
    data: params
  })
}

/**
 * 删除镜像
 * @param {JSON} params
 */
export function dockerImageRemove(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/remove',
    method: 'get',
    params: params
  })
}

/**
 * 批量删除镜像
 * @param {JSON} params
 */
export function dockerImageBatchRemove(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/batchRemove',
    method: 'get',
    params: params
  })
}

/**
 * inspect 镜像
 * @param {JSON} params
 */
export function dockerImageInspect(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/inspect',
    method: 'get',
    params: params
  })
}

/**
 * 镜像 创建容器
 * @param {JSON} params
 */
export function dockerImageCreateContainer(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/create-container',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: params
  })
}

/**
 * 拉取镜像
 * @param {JSON} params
 */
export function dockerImagePullImage(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/pull-image',
    method: 'get',
    params: params
  })
}

/**
 * 导出镜像
 * @param {JSON} params
 */
export function dockerImageSaveImage(urlPrefix, params) {
  return loadRouterBase(urlPrefix + '/images/save-image', params)
}

/**
 * 导入镜像到容器 节点
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: 容器ID,
 *
 * } formData
 */
export function dockerImageLoadImage(baseUrl, formData) {
  return axios({
    url: baseUrl + '/images/load-image',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

/**
 * 拉取镜像日志
 * @param {JSON} params
 */
export function dockerImagePullImageLog(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/images/pull-image-log',
    method: 'get',
    params: params,
    headers: {
      // tip: "no",
      loading: 'no'
    }
  })
}

/**
 * 卷
 * @param {JSON} params
 */
export function dockerVolumesList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/volumes/list',
    method: 'post',
    data: params
  })
}

/**
 * 删除卷
 * @param {JSON} params
 */
export function dockerVolumesRemove(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/volumes/remove',
    method: 'get',
    params: params
  })
}

/**
 * 网络
 * @param {JSON} params
 */
export function dockerNetworksList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/networks/list',
    method: 'post',
    data: params
  })
}

export function syncToWorkspace(params) {
  return axios({
    url: '/docker/sync-to-workspace',
    method: 'get',
    params: params
  })
}

export function dockerAllTag(params) {
  return axios({
    url: '/docker/all-tag',
    method: 'get',
    params: params
  })
}

/**
 * 容器 重建容器
 * @param {JSON} params
 */
export function dockerContainerRebuildContainer(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/container/rebuild-container',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: params
  })
}
