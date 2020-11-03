import Vue from 'vue';
import Router from 'vue-router';
import routeMenuList from './route-menu';

Vue.use(Router)

export default new Router({
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
      children: routeMenuList
    },
    {
      path: '*',
      name: '404',
      component: () => import('../pages/404')
    }
  ]
})
