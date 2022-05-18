/**
 * 路由菜单
 * key 对应后台接口返回的菜单中的 id
 * value 表示该路由的 path
 */
const routeMenuMap = {
  nodeList: "/node/list",
  nodeStat: "/node/stat",
  dockerList: "/docker/list",
  dockerSwarm: "/docker/swarm",
  sshList: "/ssh",
  commandList: "/ssh/command",
  commandLogList: "/ssh/command-log",
  outgivingList: "/dispatch/list",
  outgivingLog: "/dispatch/log",
  logRead: "/dispatch/log-read",
  outgivingWhitelistDirectory: "/dispatch/white-list",
  monitorList: "/monitor/list",
  monitorLog: "/monitor/log",
  userOptLog: "/monitor/operate-log",
  repository: "/repository/list",
  buildListOld: "/build/list",
  scriptAllList: "/node/script-all",
  serverScriptList: "/script/script-list",
  serverScriptLogList: "/script/script-log",
  /**
   * new build list page
   */
  buildList: "/build/list-info",
  buildHistory: "/build/history",
  userList: "/user/list",
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
