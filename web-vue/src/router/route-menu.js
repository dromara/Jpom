/**
 * 路由菜单
 * key 对应后台接口返回的菜单中的 id
 * value 表示该路由的 path
 */
const routeMenuMap = {
  nodeList: "/node/list",
  sshList: "/node/ssh",
  commandList: "/node/ssh/command",
  commandLogList: "/node/ssh/command-log",
  nodeUpdate: "/node/update",
  outgiving: "/dispatch/list",
  outgivingLog: "/dispatch/log",
  outgivingWhitelistDirectory: "/dispatch/white-list",
  monitor: "/monitor/list",
  monitorLog: "/monitor/log",
  userOptLog: "/monitor/operate-log",
  repository: "/repository/list",
  buildListOld: "/build/list",
  scriptAllList: "/node/script-all",
  /**
   * new build list page
   */
  buildList: "/build/list-info",
  buildHistory: "/build/history",
  user: "/user/list",
  roleList: "/role/list",
  user_log: "/operation/log",
  monitorConfigEmail: "/system/mail",
  cacheManage: "/system/cache",
  logManage: "/system/log",
  update: "/system/upgrade",
  sysConfig: "/system/config",
  projectSearch: "/node/search",
  // 数据库备份
  backup: "/system/backup",
  workspace: "/system/workspace",
};

export default routeMenuMap;
