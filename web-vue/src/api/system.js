import axios from "./config";

/**
 * 日志列表
 * @param {nodeId} params
 */
export function getLogList(params) {
  return axios({
    url: "/system/log_data.json",
    method: "post",
    data: params,
  });
}

/**
 * 下载日志
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {nodeId, path} params
 */
export function downloadFile(params) {
  return axios({
    url: "/system/log_download",
    method: "get",
    responseType: "blob",
    params,
  });
}

/**
 * 删除日志
 * @param {nodeId, path} params
 */
export function deleteLog(params) {
  return axios({
    url: "/system/log_del.json",
    method: "post",
    data: params,
  });
}

/**
 * server 缓存数据
 */
export function getServerCache() {
  return axios({
    url: "/system/server-cache",
    method: "post",
  });
}

/**
 * 节点缓存数据
 * @param {String} nodeId
 */
export function getNodeCache(nodeId) {
  return axios({
    url: "/system/node_cache.json",
    method: "post",
    data: { nodeId },
  });
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
    url: "/system/clearCache.json",
    method: "post",
    data: params,
  });
}

/**
 * 加载配置数据
 * @param {String} nodeId 节点 ID，若为空表示加载 Server 端配置
 */
export function getConfigData(nodeId) {
  return axios({
    url: "/system/config-data",
    method: "post",
    data: { nodeId },
  });
}

/**
 * 加载ip配置数据
 */
export function getIpConfigData() {
  return axios({
    url: "/system/ip-config-data",
    method: "post",
    data: {},
  });
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
    url: "/system/save_ip_config.json",
    method: "post",
    data: params,
  });
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
    url: "/system/save_config.json",
    method: "post",
    data: params,
  });
}

/**
 * 加载邮件配置
 */
export function getMailConfigData() {
  return axios({
    url: "/system/mail-config-data",
    method: "post",
  });
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
    url: "/system/mailConfig_save.json",
    method: "post",
    data: params,
  });
}

/**
 * 系统程序信息
 * @param {String} nodeId 节点 ID
 */
export function systemInfo(nodeId) {
  return axios({
    url: "/system/info",
    method: "post",
    headers: {
      tip: "no",
      loading: "no",
    },
    data: { nodeId },
  });
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
    url: "/system/uploadJar.json",
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
 * 获取更新日志
 *@param {String} nodeId 节点 ID
 */
export function changelog(nodeId) {
  return axios({
    url: "/system/change_log",
    method: "post",
    headers: {},
    data: { nodeId },
  });
}

/**
 * 检查新版本
 *@param {String} nodeId 节点 ID
 */
export function checkVersion(nodeId) {
  return axios({
    url: "/system/check_version.json",
    method: "post",
    headers: {},
    data: { nodeId },
  });
}

/**
 * 远程升级
 *@param {String} nodeId 节点 ID
 */
export function remoteUpgrade(nodeId) {
  return axios({
    url: "/system/remote_upgrade.json",
    method: "get",
    timeout: 0,
    headers: {},
    data: { nodeId },
  });
}

/**
 * 加载 白名单配置
 */
export function getWhitelist() {
  return axios({
    url: "/system/get_whitelist",
    method: "post",
    data: {},
  });
}

/**
 * 保存 白名单配置
 */
export function saveWhitelist(data) {
  return axios({
    url: "/system/save_whitelist",
    method: "post",
    data: data,
  });
}

/**
 * 加载 节点系统配置缓存信息
 */
export function getNodeConfig() {
  return axios({
    url: "/system/get_node_config",
    method: "post",
    data: {},
  });
}

/**
 * 保存 节点系统配置
 */
export function saveNodeConfig(data) {
  return axios({
    url: "/system/save_node_config.json",
    method: "post",
    data: data,
  });
}

/**
 * 加载 菜单配置信息
 */
export function getMenusConfig() {
  return axios({
    url: "/system/get_menus_config",
    method: "post",
    data: {},
  });
}

/**
 * 保存菜单配置信息
 */
export function saveMenusConfig(data) {
  return axios({
    url: "/system/save_menus_config.json",
    method: "post",
    data: data,
  });
}

/**
 * 加载 代理配置
 */
export function getProxyConfig() {
  return axios({
    url: "/system/get_proxy_config",
    method: "get",
    params: {},
  });
}

/**
 * 保存代理配置
 */
export function saveProxyConfig(data) {
  return axios({
    url: "/system/save_proxy_config",
    method: "post",
    headers: {
      "Content-Type": "application/json",
    },
    data: data,
  });
}
