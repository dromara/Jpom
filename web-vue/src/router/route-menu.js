/**
 * 路由菜单
 * key 对应后台接口返回的菜单中的 id
 * value 表示该路由的 path
 */
const routeMenuMap = {
  nodeList: "/node/list",

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
  permission_group: "/user/permission-group",
  user_log: "/operation/log",
  user_login_log: "/user/login-log",
  monitorConfigEmail: "/system/mail",
  cacheManage: "/system/cache",
  logManage: "/system/log",
  update: "/system/upgrade",
  sysConfig: "/system/config",
  systemExtConfig: "/system/ext-config",
  projectSearch: "/node/search",
  // 数据库备份
  backup: "/system/backup",
  workspace: "/system/workspace",
  globalEnv: "/system/global-env",
  machine_node_info: "/system/assets/machine-list",
  machine_ssh_info: "/system/assets/ssh-list",
  machine_docker_info: "/system/assets/docker-list",
  configWorkspaceEnv: "/script/env-list",
  cronTools: "/tools/cron",
  myWorkspaceList: "/my-workspace",
  fileStorage: "/file-manager/file-storage",
  fileReleaseTask: "/file-manager/release-task",
  certificate: "/certificate/list",
};

export default routeMenuMap;
