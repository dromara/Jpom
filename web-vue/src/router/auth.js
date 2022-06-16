/**
 * 路由鉴权
 * 比如某些路由必须要登录
 */
import {notification} from "ant-design-vue";
import router from "./index";
import store from "../store/index";

// 不需要鉴权的名单
const whiteList = ["/login", "/install", "/system/ipAccess"];
const noTabs = ["/full-terminal"];

router.beforeEach((to, from, next) => {
  // 检测白名单
  if (whiteList.indexOf(to.path) !== -1) {
    next();
    return;
  }
  // 判断 token 是否存在
  if (!store.getters.getToken) {
    if (from.path !== "/") {
      notification.error({
        message: "未登录，无法访问！",
        description: `from: ${from.path} ==> to: ${to.path}`,
      });
    }
    next("/login");
    return;
  }

  if (noTabs.indexOf(to.path) !== -1) {
    next();
    return;
  }
  // 如果存在 token (已经登录)
  store.dispatch("loadSystemMenus").then(() => {
    // 存储 store
    store.dispatch("addTab", { key: to.name, path: to.path }).then((toMenu) => {
      toMenu ? next(toMenu.path) : next();
    });
  });
});

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
  const jsPattern = /Loading chunk (\S)+ failed/g;
  const cssPattern = /Loading CSS chunk (\S)+ failed/g;
  const isChunkLoadFailed = error.message.match(jsPattern || cssPattern);
  //const targetPath = router.history.pending.fullPath;
  if (isChunkLoadFailed) {
    //localStorage.setItem("targetPath", targetPath);
    window.location.reload();
  }
});

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
