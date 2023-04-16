import { createRouter, createWebHashHistory } from 'vue-router'

const children = [
  {
    path: '/my-workspace',
    name: 'my-workspace',
    component: () => import('../pages/layout/my-workspace.vue')
  },
  {
    path: '/node/list',
    name: 'node-list',
    component: () => import('../pages/node/list.vue')
  },
  {
    path: '/docker/list',
    name: 'docker-list',
    component: () => import('../pages/docker/list.vue')
  },
  {
    path: '/docker/swarm',
    name: 'docker-swarm',
    component: () => import('../pages/docker/swarm/list.vue')
  },

  {
    path: '/node/search',
    name: 'node-search',
    component: () => import('../pages/node/search.vue')
  },
  {
    path: '/node/script-all',
    name: 'node-script-list-all',
    component: () => import('../pages/node/script-list.vue')
  },
  {
    path: '/script/script-list',
    name: 'script-list-all',
    component: () => import('../pages/script/script-list.vue')
  },
  {
    path: '/script/script-log',
    name: 'script-log',
    component: () => import('../pages/script/script-log.vue')
  },

  {
    path: '/ssh',
    name: 'node-ssh',
    component: () => import('../pages/ssh/ssh.vue')
  },
  {
    path: '/ssh/command',
    name: 'node-command',
    component: () => import('../pages/ssh/command.vue')
  },
  {
    path: '/ssh/command-log',
    name: 'node-command-log',
    component: () => import('../pages/ssh/command-log.vue')
  },
  {
    path: '/dispatch/list',
    name: 'dispatch-list',
    component: () => import('../pages/dispatch/list.vue')
  },
  {
    path: '/dispatch/log',
    name: 'dispatch-log',
    component: () => import('../pages/dispatch/log.vue')
  },
  {
    path: '/dispatch/log-read',
    name: 'dispatch-log-read',
    component: () => import('../pages/dispatch/logRead.vue')
  },

  {
    path: '/monitor/list',
    name: 'monitor-list',
    component: () => import('../pages/monitor/list.vue')
  },
  {
    path: '/monitor/log',
    name: 'monitor-log',
    component: () => import('../pages/monitor/log.vue')
  },
  {
    path: '/monitor/operate-log',
    name: 'monitor-operate-log',
    component: () => import('../pages/monitor/operate-log.vue')
  },
  {
    path: '/repository/list',
    name: 'repository-list',
    component: () => import('../pages/repository/list.vue')
  },
  {
    path: '/build/list-info',
    name: 'build-list-info',
    component: () => import('../pages/build/list-info.vue')
  },
  {
    path: '/build/history',
    name: 'build-history',
    component: () => import('../pages/build/history.vue')
  },
  {
    path: '/dispatch/white-list',
    name: 'dispatch-white-list',
    component: () => import('../pages/dispatch/white-list.vue')
  },
  {
    path: '/script/env-list',
    name: 'script-env-list',
    component: () => import('../pages/script/env.vue')
  },
  {
    path: '/tools/cron',
    name: 'cron-tools',
    component: () => import('../pages/tools/cron.vue')
  },
  {
    path: '/file-manager/file-storage',
    name: 'file-storage',
    component: () => import('../pages/file-manager/fileStorage/list.vue')
  },
  {
    path: '/file-manager/release-task',
    name: 'file-storage-release-task',
    component: () => import('../pages/file-manager/release-task/list.vue')
  },
  {
    path: '/certificate/list',
    name: '/certificate-list',
    component: () => import('../pages/certificate/list.vue')
  }
]

const management = [
  {
    path: '/system/assets/machine-list',
    name: 'system-machine-list',
    component: () => import('../pages/system/assets/machine/machine-list.vue')
  },
  {
    path: '/system/assets/ssh-list',
    name: 'system-machine-ssh-list',
    component: () => import('../pages/system/assets/ssh/ssh-list.vue')
  },
  {
    path: '/system/assets/docker-list',
    name: 'system-machine-docker-list',
    component: () => import('../pages/system/assets/docker/list.vue')
  },
  {
    path: '/user/permission-group',
    name: 'permission-group',
    component: () => import('../pages/user/permission-group.vue')
  },
  {
    path: '/user/list',
    name: 'user-list',
    component: () => import('../pages/user/index.vue')
  },
  {
    path: '/operation/log',
    name: 'operation-log',
    component: () => import('../pages/user/operation-log.vue')
  },
  {
    path: '/user/login-log',
    name: 'user-login-log',
    component: () => import('../pages/user/user-login-log.vue')
  },
  // 工作空间
  {
    path: '/system/workspace',
    name: 'system-workspace',
    component: () => import('../pages/system/workspace.vue')
  },
  {
    path: '/system/global-env',
    name: 'global-env',
    component: () => import('../pages/system/global-env.vue')
  },
  {
    path: '/system/mail',
    name: 'system-mail',
    component: () => import('../pages/system/mail.vue')
  },
  {
    path: '/system/oauth-config',
    name: 'oauth-config',
    component: () => import('../pages/system/oauth-config.vue')
  },
  {
    path: '/system/cache',
    name: 'system-cache',
    component: () => import('../pages/system/cache.vue')
  },
  {
    path: '/system/log',
    name: 'system-log',
    component: () => import('../pages/system/log.vue')
  },
  {
    path: '/system/upgrade',
    name: 'system-upgrade',
    component: () => import('../pages/system/upgrade.vue')
  },
  {
    path: '/system/config',
    name: 'system-config',
    component: () => import('../pages/system/config.vue')
  },
  {
    path: '/system/ext-config',
    name: 'ext-config',
    component: () => import('../pages/system/ext-config.vue')
  },
  // 数据库备份
  {
    path: '/system/backup',
    name: 'system-backup',
    component: () => import('../pages/system/backup.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    // {
    //   path: "/test",
    //   name: "test",
    //   component: () => import("../pages/test"),
    // },
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/login/index.vue')
    },
    // 用于过渡页面（避免跳转到管理页面重复请求接口，oauth2）
    {
      path: '/',
      name: 'home',
      component: () => import('../pages/layout/loading.vue')
    },
    {
      path: '/management',
      name: 'management',
      component: () => import('../pages/layout/index.vue'),
      redirect: '/node/list',
      children: children.map((item: any) => {
        const props = item.props || {}
        props.routerUrl = item.path
        item.props = props
        return item
      })
    },
    {
      path: '/install',
      name: 'install',
      component: () => import('../pages/login/install.vue')
    },
    {
      path: '/full-terminal',
      name: 'full-terminal',
      component: () => import('../pages/ssh/full-terminal.vue')
    },
    {
      path: '/*',
      name: '404',
      component: () => import('../pages/404/index.vue')
    },
    {
      path: '/system/ipAccess',
      name: 'ipAccess',
      component: () => import('../pages/system/ipAccess.vue')
    },
    {
      path: '/system/management',
      name: 'sys-management',
      component: () => import('../pages/layout/management.vue'),
      redirect: '/system/workspace',
      children: management.map((item: any) => {
        const props = item.props || {}
        props.routerUrl = item.path
        props.mode = 'management'
        item.props = props
        //
        const meta = item.meta || {}
        meta.mode = props.mode
        item.meta = meta
        return item
      })
    }
  ]
})

export default router
