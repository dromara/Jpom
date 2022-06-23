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

const cachePageLimitKeyName = "page_limit";
const cachePageLimit = parseInt(localStorage.getItem(cachePageLimitKeyName) || 10);

/**
 * 分页选择条
 */
export const PAGE_DEFAULT_SIZW_OPTIONS = ["5", "10", "15", "20", "25", "30", "35", "40", "50"];

/**
 * 展示总条数计算方法
 * @param {Number} total 总记录数
 * @returns String
 */
export function PAGE_DEFAULT_SHOW_TOTAL(total) {
    return `总计 ${total} 条`;
}

export const PAGE_DEFAULT_LIST_QUERY = { page: 1, limit: isNaN(cachePageLimit) ? 10 : cachePageLimit, total: 0 };

/**
 * 计算分页数据
 * @param {JSON} queryParam 分页参数
 * @param {Array} pageSizeOptions 分页选择条选项
 * @returns
 */
export function COMPUTED_PAGINATION(queryParam, pageSizeOptions) {
  // console.log(queryParam);
  const limit = queryParam.limit || PAGE_DEFAULT_LIST_QUERY.limit;
  return {
    total: queryParam.total || 0,
    current: queryParam.page || 1,
    pageSize: limit,
    pageSizeOptions: pageSizeOptions || PAGE_DEFAULT_SIZW_OPTIONS,
    showSizeChanger: true,
    showQuickJumper: true,
    showLessItems: true,
    hideOnSinglePage: limit <= 20,
    showTotal: (total) => {
      return PAGE_DEFAULT_SHOW_TOTAL(total);
    },
  };
}

/**
 * 分页切换
 * @param {JSON} listQuery
 * @param {JSON} param1
 * @returns
 */
export function CHANGE_PAGE(listQuery, { pagination, sorter }) {
  if (pagination && Object.keys(pagination).length) {
    listQuery = { ...listQuery, page: pagination.current, limit: pagination.pageSize };
    //
    localStorage.setItem(cachePageLimitKeyName, pagination.pageSize);
    //
    PAGE_DEFAULT_LIST_QUERY.limit = pagination.pageSize;
  }
  if (sorter && Object.keys(sorter).length) {
    listQuery = { ...listQuery, order: sorter.order, order_field: sorter.field };
  }
  return listQuery;
}

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

/**
 * mfa app 应用举例
 */
export const MFA_APP_TIP_ARRAY = [
  '<strong>【推荐】腾讯身份验证码</strong> 简单好用 <a href="https://a.app.qq.com/o/simple.jsp?pkgname=com.tencent.authenticator">Android</a>',
  '<strong>Authy</strong> 功能丰富 专为两步验证码 <a href="https://authy.com/download/">iOS/Android/Windows/Mac/Linux</a> &nbsp; <a href="https://chrome.google.com/webstore/detail/authy/gaedmjdfmmahhbjefcbgaolhhanlaolb?hl=cn">Chrome 扩展</a>',
  '<strong>Google Authenticator</strong> 简单易用，但不支持密钥导出备份 <a href="https://apps.apple.com/us/app/google-authenticator/id388497605">iOS</a> <a href="https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&amp;hl=cn">Android</a>',
  '<strong>Microsoft Authenticator</strong> 使用微软全家桶的推荐 <a href="https://www.microsoft.com/zh-cn/account/authenticator">iOS/Android</a>',
  '<strong>1Password</strong> 强大安全的密码管理付费应用<a href="https://1password.com/zh-cn/downloads/">iOS/Android/Windows/Mac/Linux/ChromeOS</a>',
];

/**
 * 项目 DSL 示例
 */
export const PROJECT_DSL_DEFATUL =
  "# scriptId 可以是项目路径下脚本文件名或者系统中的脚本模版ID\r\n" +
  "description: 测试\r\n" +
  "run:\r\n" +
  "  start:\r\n" +
  "#    scriptId: project.sh\r\n" +
  "    scriptId: eb16f693147b43a1b06f9eb96aed1bc7\r\n" +
  "    scriptArgs: start\r\n" +
  "  status:\r\n" +
  "#    scriptId: project.sh\r\n" +
  "    scriptId: eb16f693147b43a1b06f9eb96aed1bc7\r\n" +
  "    scriptArgs: status\r\n" +
  "  stop:\r\n" +
  "#    scriptId: project.sh\r\n" +
  "    scriptId: eb16f693147b43a1b06f9eb96aed1bc7\r\n" +
  "    scriptArgs: stop\r\n" +
  "file:\r\n" +
  "# 备份文件保留个数\r\n" +
  "#  backupCount: 5\r\n" +
  "# 限制备份指定文件后缀（支持正则）\r\n" +
  "#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  "\r\n";

/**
 * 获取 socket 地址
 * @param {String} url 二级地址
 * @param {String} parameter 参数
 * @returns
 */
export function getWebSocketUrl(url, parameter) {
  const protocol = location.protocol === "https:" ? "wss://" : "ws://";
  const domain = window.routerBase;
  const fullUrl = (domain + url).replace(new RegExp("//", "gm"), "/");
  return `${protocol}${location.host}${fullUrl}?${parameter}`;
}
