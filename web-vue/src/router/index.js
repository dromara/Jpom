import Vue from 'vue';
import Router from 'vue-router';

Vue.use(Router)

const children = [
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../pages/dashboard')
  },
  {
    path: '/node/list',
    name: 'node-list',
    component: () => import('../pages/node/list')
  },
  {
    path: '/node/ssh',
    name: 'node-ssh',
    component: () => import('../pages/node/ssh')
  },
  {
    path: '/monitor/list',
    name: 'monitor-list',
    component: () => import('../pages/monitor/list')
  },
  {
    path: '/monitor/log',
    name: 'monitor-log',
    component: () => import('../pages/monitor/log')
  },
  {
    path: '/build/list',
    name: 'build-list',
    component: () => import('../pages/build/list')
  },
  {
    path: '/build/history',
    name: 'build-history',
    component: () => import('../pages/build/history')
  },
  {
    path: '/user/list',
    name: 'user-list',
    component: () => import('../pages/user')
  },
  {
    path: '/role/list',
    name: 'role-list',
    component: () => import('../pages/role')
  },
  {
    path: '/operation/log',
    name: 'operation-log',
    component: () => import('../pages/operation-log')
  },
  {
    path: '/system/mail',
    name: 'system-mail',
    component: () => import('../pages/system/mail')
  },
  {
    path: '/system/cache',
    name: 'system-cache',
    component: () => import('../pages/system/cache')
  },
  {
    path: '/system/log',
    name: 'system-log',
    component: () => import('../pages/system/log')
  },
  {
    path: '/system/upgrade',
    name: 'system-upgrade',
    component: () => import('../pages/system/upgrade')
  },
  {
    path: '/system/config',
    name: 'system-config',
    component: () => import('../pages/system/config')
  }
]

const router = new Router({
  mode: 'hash',
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/login')
    },
    {
      path: '/',
      name: 'home',
      component: () => import('../pages/layout'),
      redirect: '/node/list',
      children: children
    },
    {
      path: '/install',
      name: 'install',
      component: () => import('../pages/install')
    },
    {
      path: '*',
      name: '404',
      component: () => import('../pages/404')
    }
  ]
})

export default router
