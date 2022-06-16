import Vue from "vue";
import Router from "vue-router";

// NavigationDuplicated: Avoided redundant navigation to current location: "xxxx".
const originalPush = Router.prototype.push;

Router.prototype.push = function push(location) {
  return originalPush.call(this, location).catch((err) => err);
};

Vue.use(Router);

const children = [
  {
    path: "/dashboard",
    name: "dashboard",
    component: () => import("../pages/dashboard"),
  },
  {
    path: "/node/list",
    name: "node-list",
    component: () => import("../pages/node/list"),
  },
  {
    path: "/docker/list",
    name: "docker-list",
    component: () => import("../pages/docker/list"),
  },
  {
    path: "/docker/swarm",
    name: "docker-swarm",
    component: () => import("../pages/docker/swarm/list"),
  },

  {
    path: "/node/stat",
    name: "node-stat",
    component: () => import("../pages/node/stat"),
  },
  {
    path: "/node/search",
    name: "node-search",
    component: () => import("../pages/node/search"),
  },
  {
    path: "/node/script-all",
    name: "node-script-list-all",
    component: () => import("../pages/node/script-list"),
  },
  {
    path: "/script/script-list",
    name: "script-list-all",
    component: () => import("../pages/script/script-list"),
  },
  {
    path: "/script/script-log",
    name: "script-log",
    component: () => import("../pages/script/script-log"),
  },

  {
    path: "/ssh",
    name: "node-ssh",
    component: () => import("../pages/ssh/ssh"),
  },
  {
    path: "/ssh/command",
    name: "node-command",
    component: () => import("../pages/ssh/command"),
  },
  {
    path: "/ssh/command-log",
    name: "node-command-log",
    component: () => import("../pages/ssh/command-log"),
  },
  {
    path: "/dispatch/list",
    name: "dispatch-list",
    component: () => import("../pages/dispatch/list"),
  },
  {
    path: "/dispatch/log",
    name: "dispatch-log",
    component: () => import("../pages/dispatch/log"),
  },
  {
    path: "/dispatch/log-read",
    name: "dispatch-log-read",
    component: () => import("../pages/dispatch/logRead"),
  },
  {
    path: "/dispatch/white-list",
    name: "dispatch-white-list",
    component: () => import("../pages/dispatch/white-list"),
  },
  {
    path: "/monitor/list",
    name: "monitor-list",
    component: () => import("../pages/monitor/list"),
  },
  {
    path: "/monitor/log",
    name: "monitor-log",
    component: () => import("../pages/monitor/log"),
  },
  {
    path: "/monitor/operate-log",
    name: "monitor-operate-log",
    component: () => import("../pages/monitor/operate-log"),
  },
  {
    path: "/repository/list",
    name: "repository-list",
    component: () => import("../pages/repository/list"),
  },
  {
    path: "/build/list-info",
    name: "build-list-info",
    component: () => import("../pages/build/list-info"),
  },
  {
    path: "/build/history",
    name: "build-history",
    component: () => import("../pages/build/history"),
  },
  {
    path: "/user/list",
    name: "user-list",
    component: () => import("../pages/user"),
  },
  // {
  //   path: "/role/list",
  //   name: "role-list",
  //   component: () => import("../pages/role"),
  // },
  {
    path: "/operation/log",
    name: "operation-log",
    component: () => import("../pages/user/operation-log"),
  },
  {
    path: "/system/mail",
    name: "system-mail",
    component: () => import("../pages/system/mail"),
  },
  {
    path: "/system/cache",
    name: "system-cache",
    component: () => import("../pages/system/cache"),
  },
  {
    path: "/system/log",
    name: "system-log",
    component: () => import("../pages/system/log"),
  },
  {
    path: "/system/upgrade",
    name: "system-upgrade",
    component: () => import("../pages/system/upgrade"),
  },
  {
    path: "/system/config",
    name: "system-config",
    component: () => import("../pages/system/config"),
  },
  // 数据库备份
  {
    path: "/system/backup",
    name: "system-backup",
    component: () => import("../pages/system/backup"),
  },
  // 工作空间
  {
    path: "/system/workspace",
    name: "system-workspace",
    component: () => import("../pages/system/workspace"),
  },
];

const router = new Router({
  mode: "hash",
  routes: [
    {
      path: "/test",
      name: "test",
      component: () => import("../pages/test"),
    },
    {
      path: "/login",
      name: "login",
      component: () => import("../pages/login"),
    },
    {
      path: "/",
      name: "home",
      component: () => import("../pages/layout"),
      redirect: "/node/list",
      children: children,
    },
    {
      path: "/install",
      name: "install",
      component: () => import("../pages/login/install"),
    },
    {
      path: "/full-terminal",
      name: "full-terminal",
      component: () => import("../pages/ssh/full-terminal"),
    },
    {
      path: "*",
      name: "404",
      component: () => import("../pages/404"),
    },
    {
      path: "/system/ipAccess",
      name: "ipAccess",
      component: () => import("../pages/system/ipAccess"),
    },
  ],
});

export default router;
