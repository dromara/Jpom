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
    path: '/user/list',
    name: 'user-list',
    component: () => import('../pages/user')
  },
  {
    path: '/role/list',
    name: 'role-list',
    component: () => import('../pages/role')
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
      redirect: '/dashboard',
      children: children
    },
    {
      path: '*',
      name: '404',
      component: () => import('../pages/404')
    }
  ]
})

export default router
