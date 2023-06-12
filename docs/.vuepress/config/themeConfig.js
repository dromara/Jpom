const nav = require("./nav.js");
const htmlModules = require("./htmlModules.js");

// Theme Config
module.exports = {
  nav,
  sidebarDepth: 2, // 侧边栏显示深度，默认1，最大2（显示到h3标题）
  logo: '/images/logo/jpom_logo_small.svg', // 导航栏logo
  searchMaxSuggestions: 10, // 搜索结果显示最大数
  lastUpdated: '上次更新', // 更新的时间，及前缀文字   string | boolean (取值为git提交时间)

  docsRepo: "dromara/Jpom",
  // docsRepo: "https://gitee.com/dromara/Jpom",
  docsDir: 'docs', // 编辑的文件夹
  docsBranch: 'docs',
  editLinks: true, // 编辑链接
  editLinkText: '帮助我们改善此文档',

  // 以下配置是Vdoing主题改动的和新增的配置
  sidebar: {mode: 'structuring', collapsable: false}, // 侧边栏  'structuring' | { mode: 'structuring', collapsable: Boolean} | 'auto' | 自定义    温馨提示：目录页数据依赖于结构化的侧边栏数据，如果你不设置为'structuring',将无法使用目录页

  // sidebarOpen: false, // 初始状态是否打开侧边栏，默认true
  updateBar: { // 最近更新栏
    showToArticle: true, // 显示到文章页底部，默认true
    // moreArticle: '/archives' // “更多文章”跳转的页面，默认'/archives'
  },

  // titleBadge: false, // 文章标题前的图标是否显示，默认true
  // titleBadgeIcons: [ // 文章标题前图标的地址，默认主题内置图标
  //   '图标地址1',
  //   '图标地址2'
  // ],
  // bodyBgImg: [
  //   'https://cdn.jsdelivr.net/gh/xugaoyi/image_store/blog/20200507175828.jpeg',
  //   'https://cdn.jsdelivr.net/gh/xugaoyi/image_store/blog/20200507175845.jpeg',
  //   'https://cdn.jsdelivr.net/gh/xugaoyi/image_store/blog/20200507175846.jpeg'
  // ], // body背景大图，默认无。 单张图片 String || 多张图片 Array, 多张图片时每隔15秒换一张。

  // bodyBgImgOpacity: 0,
  // categoryText: '随笔', // 碎片化文章（_posts文件夹的文章）预设生成的分类值，默认'随笔'

  // contentBgStyle: 1,

  category: true, // 是否打开分类功能，默认true。 如打开，会做的事情有：1. 自动生成的frontmatter包含分类字段 2.页面中显示与分类相关的信息和模块 3.自动生成分类页面（在@pages文件夹）。如关闭，则反之。
  tag: true, // 是否打开标签功能，默认true。 如打开，会做的事情有：1. 自动生成的frontmatter包含标签字段 2.页面中显示与标签相关的信息和模块 3.自动生成标签页面（在@pages文件夹）。如关闭，则反之。
  archive: true, // 是否打开归档功能，默认true。 如打开，会做的事情有：1.自动生成归档页面（在@pages文件夹）。如关闭，则反之。

  author: { // 文章默认的作者信息，可在md文件中单独配置此信息 String | {name: String, href: String}
    name: 'bwcx_jzy', // 必需
    href: 'https://gitee.com/bwcx-jzy' // 可选的
  },
  social: { // 社交图标，显示于博主信息栏和页脚栏
    // iconfontCssFile: '//at.alicdn.com/t/font_1678482_u4nrnp8xp6g.css', // 可选，阿里图标库在线css文件地址，对于主题没有的图标可自由添加
    icons: [
      // {
      //   iconClass: 'icon-youjian',
      //   title: '发邮件',
      //   link: 'mailto:keepbx@jiangzeyin.cn'
      // },
      {
        iconClass: 'icon-gitee',
        title: 'Gitee',
        link: 'https://gitee.com/dromara/Jpom'
      },
      {
        iconClass: 'icon-github',
        title: 'GitHub',
        link: 'https://github.com/dromara/Jpom'
      }
    ]
  },
  footer: { // 页脚信息
    createYear: 2017, //年份
    copyrightInfo: '<span>MIT License &nbsp;<a target="_blank" href="https://beian.miit.gov.cn">京ICP备17044819号</a></span>', // 博客版权信息，支持a标签
  },
  htmlModules,
}
