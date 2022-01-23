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

// export const GUIDE_FLAG_KEY = "Jpom-GuideFlag";

// export const GUIDE_HOME_USED_KEY = "Jpom-Home-Guide-Used";

// export const GUIDE_NODE_USED_KEY = "Jpom-Node-Guide-Used";

export const NO_NOTIFY_KEY = "tip";

export const NO_LOADING_KEY = "loading";

export const LOADING_TIP = "loadingTip";

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

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: "取消定时,不再定时执行",
    children: [
      {
        title: "",
        value: "",
      },
    ],
  },
  {
    title: "分钟级别",
    children: [
      {
        title: "1分钟",
        value: "0 0/1 * * * ?",
      },
      {
        title: "5分钟",
        value: "0 0/5 * * * ?",
      },
      {
        title: "10分钟",
        value: "0 0/10 * * * ?",
      },
      {
        title: "30分钟",
        value: "0 0/30 * * * ?",
      },
    ],
  },
  {
    title: "小时级别",
    children: [
      {
        title: "每小时",
        value: "0 0 0/1 * * ?",
      },
    ],
  },
  {
    title: "天级别",
    children: [
      {
        title: "凌晨0点和中午12点",
        value: "0 0 0,12 * * ?",
      },
      {
        title: "凌晨0点",
        value: "0 0 0 * * ?",
      },
    ],
  },
  {
    title: "秒级别（默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）",
    children: [
      {
        title: "5秒一次",
        value: "0/5 * * * * ?",
      },
      {
        title: "10秒一次",
        value: "0/10 * * * * ?",
      },
      {
        title: "30秒一次",
        value: "0/30 * * * * ?",
      },
    ],
  },
];

/**
 * 压缩文件格式
 */
export const ZIP_ACCEPT = ".tar,.bz2,.gz,.zip,.tar.bz2,.tar.gz";
