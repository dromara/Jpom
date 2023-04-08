///
/// The MIT License (MIT)
///
/// Copyright (c) 2019 Code Technology Studio
///
/// Permission is hereby granted, free of charge, to any person obtaining a copy of
/// this software and associated documentation files (the "Software"), to deal in
/// the Software without restriction, including without limitation the rights to
/// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
/// the Software, and to permit persons to whom the Software is furnished to do so,
/// subject to the following conditions:
///
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
///
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
/// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
/// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
/// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
/// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
///

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
};

export default routeMenuMap;
