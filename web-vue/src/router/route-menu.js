/**
 * 路由菜单
 * id 是后台菜单指定的 id
 */
const routeMenuList = [
  {
    path: '/node/list',
    name: 'node-list',
    component: () => import('../pages/node/list')
  },
  {
    path: '/node/ssh',
    name: 'node-ssh',
    component: () => import('../pages/node/list')
  }
]

export default routeMenuList
