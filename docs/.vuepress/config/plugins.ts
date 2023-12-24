import { UserPlugins } from "vuepress/config";
import dayjs from "dayjs";

// 插件配置
export default <UserPlugins>[
  // 自定义插件，即本地插件
  [
    {
      name: "custom-plugins",
      globalUIComponents: ["PageInfo", "BlockToggle"], // 2.x 版本 globalUIComponents 改名为 clientAppRootComponentFiles
    },
  ],
  // 本地插件
  // [require('../plugins/love-me'), { // 鼠标点击爱心特效
  //   // color: '#11a8cd', // 爱心颜色，默认随机色
  //   excludeClassName: 'theme-vdoing-content' // 要排除元素的class, 默认空''
  // }],

  "vuepress-plugin-baidu-autopush", // 百度自动推送

  // 可以添加第三方搜索链接的搜索框（原官方搜索框的参数仍可用）
  // [
  //   'thirdparty-search',
  //   {
  //     thirdparty: [
  //       // 可选，默认 []
  //       {
  //         title: '在MDN中搜索',
  //         frontUrl: 'https://developer.mozilla.org/zh-CN/search?q=', // 搜索链接的前面部分
  //         behindUrl: '', // 搜索链接的后面部分，可选，默认 ''
  //       },
  //       {
  //         title: '在Runoob中搜索',
  //         frontUrl: 'https://www.runoob.com/?s=',
  //       },
  //       {
  //         title: '在Vue API中搜索',
  //         frontUrl: 'https://cn.vuejs.org/v2/api/#',
  //       },
  //       {
  //         title: '在Bing中搜索',
  //         frontUrl: 'https://cn.bing.com/search?q=',
  //       },
  //       {
  //         title: '通过百度搜索本站的',
  //         frontUrl: 'https://www.baidu.com/s?wd=',
  //       },
  //     ],
  //   },
  // ],
  // 官网：https://github.com/leo-buneev/vuepress-plugin-fulltext-search
  ["fulltext-search"],

  [
    "one-click-copy",
    {
      // 代码块复制按钮
      copySelector: [
        'div[class*="language-"] pre',
        'div[class*="aside-code"] aside',
      ], // String or Array
      copyMessage: "复制成功", // default is 'Copy successfully and then paste it for use.'
      duration: 1000, // prompt message display time.
      showInMobile: false, // whether to display on the mobile side, default: false.
    },
  ],
  // [
  //   'demo-block',
  //   {
  //     // demo演示模块 https://github.com/xiguaxigua/vuepress-plugin-demo-block
  //     settings: {
  //       // jsLib: ['http://xxx'], // 在线示例(jsfiddle, codepen)中的js依赖
  //       // cssLib: ['http://xxx'], // 在线示例中的css依赖
  //       // vue: 'https://unpkg.zhimg.com/vue/dist/vue.min.js', // 在线示例中的vue依赖
  //       jsfiddle: false, // 是否显示 jsfiddle 链接
  //       codepen: true, // 是否显示 codepen 链接
  //       horizontal: false, // 是否展示为横向样式
  //     },
  //   },
  // ],
  [
    "vuepress-plugin-zooming", // 放大图片
    {
      selector: ".theme-vdoing-content img:not(.no-zoom)", // 排除 class 是 no-zoom 的图片
      options: {
        bgColor: "rgba(0,0,0,0.6)",
      },
    },
  ],
  [
    "vuepress-plugin-baidu-tongji", // 百度统计
    {
      hm: "1acbde0eb1cc4225dcfe6d1143767a3d",
    },
  ],
  // [
  // 'vuepress-plugin-comment', // 评论插件
  // 选择 Gitalk 评论
  // {
  //   choosen: 'gitalk',
  //   options: {
  //     clientID: 'c04227eafac4679001c7',
  //     clientSecret: 'b77b38cf7272b5a01bf109c24de4b8cb5345d0a0',
  //     repo: 'notes-blog', // GitHub 仓库
  //     owner: 'Kele-Bingtang', // GitHub仓库所有者
  //     admin: ['Kele-Bingtang'], // 对仓库有写权限的人
  //     // distractionFreeMode: true,
  //     pagerDirection: 'last', // 'first'正序 | 'last'倒序
  //     id: '<%- (frontmatter.permalink || frontmatter.to.path).slice(-16) %>', //  页面的唯一标识,长度不能超过50
  //     title: '「评论」<%- frontmatter.title %>', // GitHub issue 的标题
  //     labels: ['Gitalk', 'Comment'], // GitHub issue 的标签
  //     body:
  //       '页面：<%- window.location.origin + (frontmatter.to.path || window.location.pathname) %>', // GitHub issue 的内容
  //   },
  // },
  // 选择 Valine 评论
  // {
  //   // 具体参数请看官网：https://valine.js.org/
  //   choosen: 'valine',
  //   // options 选项中的所有参数，会传给 Valine 的配置
  //   options: {
  //     el: '#valine-vuepress-comment',
  //     appId: 'aNoBTRmpqQLewUkEgcLQG648-gzGzoHsz',
  //     appKey: 'bBXuTfFx7q5UHNbCFg8QUzF7',
  //     placeholder: '请留下你的足迹 ~~',
  //     // 有 URL、''、mp、identicon、monsterid、wavatar、retro、robohash、hide 头像选择，具体头像是什么样子，请访问 https://valine.js.org/avatar.html
  //     avatar: 'mp',
  //     pageSize: 10,   // 评论列表分页，每页条数
  //     visitor: true,    // 文章访问量统计
  //     recordIP: false,   // 是否记录评论者 IP
  //     enableQQ: true,   // 是否启用昵称框自动获取 QQ 昵称和 QQ 头像, 默认关闭
  //   }
  // }
  // ],
  [
    "@vuepress/last-updated", // "上次更新"时间格式
    {
      transformer: (timestamp, lang) => {
        return dayjs(timestamp).format("YYYY/MM/DD, HH:mm:ss");
      },
    },
  ],
  // 顶部阅读进度插件
  ["reading-progress"],
  // 音乐插件，官网：https://moefyit.github.io/moefy-vuepress/packages/meting.html
  // [
  //   'meting',
  //   {
  //     // 这个 API 是不可用的，只是作为示例而已 https://music.163.com/playlist?id=2555412439&userid=1490434176
  //     // metingApi: 'https://music.163.com/',
  //     meting: {   // 不配置该项的话不会出现全局播放器
  //       server: 'netease',    // 音乐平台，可选值： "netease" | "tencent" | "kuwo" | "kugou" | "baidu" | "xiami"，netease 是网易云
  //       type: 'playlist',   // 资源类型（播放列表、单曲、专辑等），可选值： "song" | "album" | "artist" | "playlist"
  //       mid: '2555412439',    // 资源 ID
  //       // auto: 'https://music.163.com/playlist?id=2555412439',  // 资源 url，填写后可通过资源 url 自动解析资源平台、类型、ID，上述三个选项将被覆盖（本参数仅支持 netease、tencent、xiami 三平台）
  //     },
  //     aplayer: {
  //       fixed: true,   // 是否开启吸底模式，即自动隐藏在屏幕边框
  //       mini: true,    // 是否开启迷你模式
  //       autoplay: false,   // 是否开启自动播放
  //       theme: '#b7daff', // 设置播放器默认主题颜色
  //       loop: 'all',   // 设置播放器的播放循环模式，可选值：'all' | 'one' | 'none'
  //       order: 'list',  // 设置播放器的播放顺序模式，可选值：'list' | 'random'
  //       preload: 'auto',  // 设置音频的预加载模式，可选值：'none' | 'metadata' | 'auto'
  //       volume: 0.7,    // 设置播放器的音量
  //       additionalAudios: [],  // 除 Meting 解析的 audio 外额外添加的 audio，一般官方自带的就行
  //       mutex: true,   // 是否开启互斥模式，即该播放器播放音乐后，停止其他正在播放的播放器
  //       lrcType: 0,   // 设置 lrc 歌词解析模式，可选值：3 | 1 | 0（0：禁用 lrc 歌词，1：lrc 格式的字符串，3：lrc 文件 url）
  //       listFolded: false,   // 刚打开播放器时，是否折叠播放列表
  //       listMaxHeight: 250,  // 设置播放列表最大高度，单位为像素
  //       storageName: 'vuepress-plugin-meting', // 设置存储播放器设置的 localStorage key
  //     },
  //     mobile: {   // 移动端配置
  //       cover: true,  // 是否显示封面图，如果隐藏的话可以防止播放器遮挡移动设备上的文字内容
  //       lrc: true,       // 是否显示歌词
  //     }
  //   },
  // ],
  // 鼠标点击页面的爆炸效果，官网：https://moefyit.github.io/moefy-vuepress/packages/cursor-effects.html
  // [
  //   'cursor-effects',
  //   {
  //     size: 2, // 粒子的大小，默认值：2
  //     shape: ['star'], // 粒子的形状，star：星形，circle：圆形。默认值：star
  //     zIndex: 999999999, // 页面的索引属性，默认值：99999999，
  //   },
  // ],
  // 动态标题，官网：https://moefyit.github.io/moefy-vuepress/packages/dynamic-title.html
  [
    "dynamic-title",
    {
      showIcon: "/favicon.ico",
      showText: "欢迎回来！继续阅读",
      hideIcon: "/failure.ico",
      hideText: "不要走呀！还没有读完呢",
      recoverTime: 2000, //  持续时间
    },
  ],
  // vuepress-plugin-tabs。官网：https://github.com/pskordilakis/vuepress-plugin-tabs
  ["tabs"],
  [
    "sitemap",
    {
      hostname: "https://jpom.top",
    },
  ],
];
