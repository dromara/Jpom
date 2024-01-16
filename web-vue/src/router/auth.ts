/**
 * 路由鉴权
 * 比如某些路由必须要登录
 */
import router from './index'

import { useAllMenuStore } from '@/stores/menu2'

import Qs from 'qs'
import { RouteLocationNormalized, NavigationGuardNext } from 'vue-router'

// 不需要鉴权的名单
const whiteList: string[] = ['/login', '/install', '/prohibit-access', '/404']
const noTabs: string[] = ['/full-terminal', '/ssh-tabs']

router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const useAppStore = appStore()
  useAppStore.pageLoading(true)
  if (to.matched.length === 0) {
    next('*')
    return
  }
  // 检测白名单
  if (whiteList.indexOf(to.path) !== -1) {
    next()
    return
  }
  // 判断 token 是否存在
  if (!userStore().getToken()) {
    if (from.path !== '/') {
      console.warn(`from: ${from.path} ==> to: ${to.path}`)
    }
    next({
      path: '/login',
      query: from.query,
      replace: true
    })
    return
  }
  // 如果存在 token (已经登录)
  // 刷新用户信息
  userStore().pageReloadRefreshUserInfo()

  // 没有 tabs 独立页面
  if (noTabs.indexOf(to.path) !== -1) {
    next()
    return
  }
  const lastMenuStore = useAllMenuStore()
  // if (to.meta?.mode === 'management') {
  //   // 刷新菜单
  //   menuStore2 = managementMenuStore
  // } else {
  //   // 刷新菜单
  //   menuStore2 = menuStore
  // }
  // console.log(lastMenuStore, to.meta?.mode)
  // console.log(to)

  const mode = to.meta?.mode || 'normal'

  lastMenuStore
    .loadSystemMenus(mode)
    .then(() => {
      // 存储 store
      lastMenuStore.addTab(mode, { key: to.name, path: to.path }).then((toMenu: any) => {
        toMenu?.path ? next(toMenu.path) : next()
      })
    })
    .catch(() => {
      next({
        path: '/',
        replace: true
      })
    })
})

router.afterEach((to: RouteLocationNormalized) => {
  const useAppStore = appStore()
  useAppStore.pageLoading(false)
  useAppStore.showInfo(to)
  const params = Qs.parse(location.search.substring(1))
  if (Object.keys(params).length) {
    //地址栏参数转 hash 参数
    const paramsStr = Qs.stringify(Object.assign({}, params, to.query))
    //console.error(`${location.origin}${location.pathname}#${to.path}?${paramsStr}`);
    location.href = `${location.origin}${location.pathname}#${to.path}?${paramsStr}`
  }
})

// https://www.jb51.net/article/242702.htm
// 监听到路由错误 刷新页面

/* 正则使用'\S'而不是'\d' 为了适配写魔法注释的朋友，写'\d'遇到魔法注释就匹配不成功了。
 * 使用reload方法而不是replace原因是replace还是去请求之前的js文件，会导致循环报错。
 * reload会刷新页面， 请求最新的index.html以及最新的js路径。
 * 直接修改location.href或使用location.assign或location.replace，和router.replace同理，
 * 在当前场景中请求的依然是原来的js文件，区别仅有浏览器的历史栈。因此必须采用reload.
 * reload()有个特点是当你在A页面试图进入B页面的时候报错，会在A页面刷新，因此在刷新后需要手动书写逻辑
 * 进入B页面，可以在router.onReady()方法里面书写
 * 为了避免在特殊情况下服务器丢失资源导致无限报错刷新，做了一步控制，仅尝试一次进入B页面，
 * 如果不成功就只刷新A页面，停留在当前的A页面。
 */
router.onError((error) => {
  const jsPattern: RegExp = /Loading chunk (\S)+ failed/g
  const cssPattern: RegExp = /Loading CSS chunk (\S)+ failed/g
  const isChunkLoadFailed = error.message.match(jsPattern || cssPattern)
  //const targetPath = router.history.pending.fullPath;
  if (isChunkLoadFailed) {
    //localStorage.setItem("targetPath", targetPath);
    window.location.reload()
  }
})

// router.onReady(() => {
//   const targetPath = localStorage.getItem("targetPath");
//   const tryReload = localStorage.getItem("tryReload");
//   if (targetPath) {
//     localStorage.removeItem("targetPath");
//     if (!tryReload) {
//       router.replace(targetPath);
//       localStorage.setItem("tryReload", true);
//     } else {
//       localStorage.removeItem("tryReload");
//     }
//   }
// });
