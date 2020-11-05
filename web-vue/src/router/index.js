import Vue from 'vue';
import Router from 'vue-router';

Vue.use(Router)

const children = [
  {
    path: '/node/list',
    name: 'node-list',
    component: () => import('../pages/node/list')
  },
  {
    path: '/node/ssh',
    name: 'node-ssh',
    component: () => import('../pages/node/ssh')
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
