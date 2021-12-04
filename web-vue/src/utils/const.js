// 常量池
export const USER_NAME_KEY = "Jpom-UserName";

export const TOKEN_KEY = "Jpom-Token";

export const LONG_TERM_TOKEN = "Jpom-Long-Term-Token";

export const USER_INFO_KEY = "Jpom-User";

export const MENU_KEY = "Jpom-Menus";

export const TOKEN_HEADER_KEY = "Authorization";

export const ACTIVE_TAB_KEY = "Jpom-ActiveTab";

export const TAB_LIST_KEY = "Jpom-TabList";

export const ACTIVE_MENU_KEY = "Jpom-ActiveMenu";

export const GUIDE_FLAG_KEY = "Jpom-GuideFlag";

export const GUIDE_HOME_USED_KEY = "Jpom-Home-Guide-Used";

export const GUIDE_NODE_USED_KEY = "Jpom-Node-Guide-Used";

export const NO_NOTIFY_KEY = "tip";

export const NO_LOADING_KEY = "loading";

/**
 * 分页默认条数
 */
export const PAGE_DEFAULT_LIMIT = 10;
/**
 * 分页选择条
 */
export const PAGE_DEFAULT_SIZW_OPTIONS = ["10", "20", "50", "100"];
/**
 * 展示总条数计算方法
 * @param {Number} total 总记录数
 * @param {JSON} listQuery  通用分页参数
 * @returns String
 */
export function PAGE_DEFAULT_SHOW_TOTAL(total, listQuery) {
  if (total <= listQuery.limit) {
    return "";
  }
  return `总计 ${total} 条`;
}
