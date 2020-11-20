/**
 * 路由菜单
 * key 对应后台接口返回的菜单中的 id
 * value 表示该路由的 path
 */
const routeMenuMap = {
  'nodeList': '/node/list',
  'sshList': '/node/ssh',
  'user': '/user/list',
  'roleList': '/role/list',
  'user_log': '/operation/log',
  'logManage': '/system/log'
}

export default routeMenuMap
