/**
 * 路由鉴权
 * 比如某些路由必须要登录
 */
import { notification } from "ant-design-vue";
import router from "./index";
import store from "../store/index";

// 不需要鉴权的名单
const whiteList = ["/login", "/install", "/system/ipAccess"];

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

  // 如果存在 token (已经登录)
  store.dispatch("loadSystemMenus").then(() => {
    // 存储 store
    store.dispatch("addTab", { key: to.name, path: to.path }).then((toMenu) => {
      toMenu ? next(toMenu.path) : next();
    });
  });
});
