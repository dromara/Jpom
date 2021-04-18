import axios from './config';

/**
 * 日志列表
 * @param {nodeId} params 
 */
export function getLogList(params) {
  return axios({
    url: '/system/log_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 下载日志
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path} params 
 */
export function downloadFile(params) {
  return axios({
    url: '/system/log_download',
    method: 'get',
    responseType: 'blob',
    params
  })
}

/**
 * 删除日志
 * @param {nodeId, path} params 
 */
export function deleteLog(params) {
  return axios({
    url: '/system/log_del.json',
    method: 'post',
    data: params
  })
}

/**
 * server 缓存数据
 */
export function getServerCache() {
  return axios({
    url: '/system/server-cache',
    method: 'post'
  })
}

/**
 * 节点缓存数据
 * @param {String} nodeId 
 */
export function getNodeCache(nodeId) {
  return axios({
    url: '/system/node_cache.json',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 清空缓存
 * @param {
 *  type: 类型
 *  nodeId: 节点 ID
 * } params 
 */
export function clearCache(params) {
  return axios({
    url: '/system/clearCache.json',
    method: 'post',
    data: params
  })
}

/**
 * 加载配置数据
 * @param {String} nodeId 节点 ID，若为空表示加载 Server 端配置 
 */
export function getConfigData(nodeId) {
  return axios({
    url: '/system/config-data',
    method: 'post',
    data: {nodeId}
  })
}

/**
 * 加载ip配置数据
 */
export function getIpConfigData() {
  return axios({
    url: '/system/ip-config-data',
    method: 'post',
    data: {}
  })
}

/**
 * 编辑配置
 * @param {JSON} params {
 *  allowed: 允许访问，白名单ip,
 *  prohibited: 禁止访问，黑名单ip
 * }
 */
export function editIpConfig(params) {
  return axios({
    url: '/system/save_ip_config.json',
    method: 'post',
    data: params
  })
}


/**
 * 编辑配置
 * @param {
 *  nodeId: 节点 ID,
 *  content: 配置内容,
 *  restart: 是否重启
 * } params 
 */
export function editConfig(params) {
  return axios({
    url: '/system/save_config.json',
    method: 'post',
    data: params
  })
}

/**
 * 加载邮件配置
 */
export function getMailConfigData() {
  return axios({
    url: '/system/mail-config-data',
    method: 'post'
  })
}

/**
 * 编辑配置
 * @param {
 *  host: SMTP 服务器域名,
 *  port: SMTP 服务端口,
 *  user: 用户名,
 *  pass: 密码,
 *  from: 发送方，遵循RFC-822标准,
 *  sslEnable: 是否 SSL 安全连接,
 *  socketFactoryPort: SSL 加密端口
 * } params 
 */
export function editMailConfig(params) {
  return axios({
    url: '/system/mailConfig_save.json',
    method: 'post',
    data: params
  })
}

/**
 * 系统程序信息
 * @param {String} nodeId 节点 ID
 */
export function systemInfo(nodeId) {
  return axios({
    url: '/system/info',
    method: 'post',
    headers: {
      tip: 'no',
      loading: 'no'
    },
    data: {nodeId}
  })
}

/**
 * 上传升级文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  nodeId: 节点 ID
 * } formData 
 */
export function uploadUpgradeFile(formData) {
  return axios({
    url: '/system/uploadJar.json',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0, 
    data: formData
  })
}