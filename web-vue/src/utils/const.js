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
export const PAGE_DEFAULT_SIZW_OPTIONS = ["5", PAGE_DEFAULT_LIMIT + "", "20", "30", "40", "50"];
/**
 * 展示总条数计算方法
 * @param {Number} total 总记录数
 * @param {JSON} listQuery  通用分页参数
 * @returns String
 */
export function PAGE_DEFAULT_SHOW_TOTAL(total, listQuery) {
  if (total <= listQuery.limit) {
    return `总计 ${total} 条`;
  }
  return `总计 ${total} 条`;
}

export const PAGE_DEFAULT_LIST_QUERY = { page: 1, limit: PAGE_DEFAULT_LIMIT, total: 0 };

/**
 * 缓存当前的工作空间 ID
 */
export const CACHE_WORKSPACE_ID = "workspaceId";

/**
 * 升级 重启检查等待次数
 */
export const RESTART_UPGRADE_WAIT_TIME_COUNT = 80;
