export default [
  {
    text: "🔖首页",
    link: "/",
  },
  {
    text: "💻演示",
    link: "/pages/2e4ffc/",
  },
  {
    text: "📖文档",
    link: "/pages/install/",
    items: [
      {
        text: "上手安装",
        items: [
          { text: "一键安装", link: "/pages/fe28e9/" },
          { text: "下载安装", link: "/pages/db3065/" },
          { text: "容器安装", link: "/pages/820286/" },
        ],
      },
      {
        text: "大版本升级",
        items: [
          {
            text: "v2.10.X迁移到v2.11.X指南",
            link: "/pages/upgrade/2.10.x-to-2.11.x/",
          },
          {
            text: "v2.9.X迁移到v2.10.X指南",
            link: "/pages/upgrade/2.9.x-to-2.10.x/",
          },
          {
            text: "v2.8.X迁移到v2.9.X指南",
            link: "/pages/upgrade/2.8.x-to-2.9.x/",
          },
          { text: "数据库迁移到 mysql", link: "/pages/4cfb46/" },
        ],
      },
      {
        text: "文档碎片",
        items: [
          { text: "分类", link: "/categories/" },
          { text: "标签", link: "/tags/" },
          { text: "归档", link: "/archives/" },
        ],
      },
    ],
  },
  {
    text: "🔥实践案例",
    link: "/pages/practice/",
    items: [
      { text: "实践案例目录", link: "/pages/practice/catalogue/" },
      {
        text: "安装实践",
        items: [
          { text: "一键安装教程", link: "/pages/15b7a2/" },
          { text: "Docker 容器安装教程", link: "/pages/c846d3/" },
          { text: "离线安装教程", link: "/pages/af288b/" },
        ],
      },
    ],
  },
  {
    text: "❓常见问题",
    link: "/pages/FQA/",
    items: [
      { text: "名词解释", link: "/pages/FQA/proper-noun/" },
      { text: "账户相关", link: "/pages/839836/" },
      { text: "构建相关", link: "/pages/6a1f29/" },
      { text: "工作空间", link: "/pages/d3f985/" },
      { text: "权限说明", link: "/pages/ca90a5/" },
      { text: "资产说明", link: "/pages/350118/" },
    ],
  },
  {
    text: "💡DSL说明",
    link: "/pages/FQA/DSL/",
  },
  {
    text: "💖周边",
    link: "/pages/shop/",
    items: [
      { text: "赞赏支持", link: "/pages/praise/" },
      { text: "赞赏记录", link: "/pages/praise/publicity/" },
      { text: "加入社区讨论", link: "/pages/praise/join/" },
      { text: "如何贡献", link: "/pages/dc18b8/" },
      { text: "贡献者们", link: "/pages/praise/friends/" },
      {
        text: " 🚩用户",
        link: "/pages/user/",
      },
      {
        text: "🤝企业服务",
        link: "/pages/enterprise-service/",
      },
    ],
  },

  {
    text: "📰更新记录",
    link: "/pages/changelog/new/",
    items: [{ text: "下载中心", link: "/pages/all-downloads/" }],
  },
  {
    text: "📦仓库",
    items: [
      { text: "Gitee", link: "https://gitee.com/dromara/Jpom" },
      { text: "Github", link: "https://github.com/dromara/Jpom" },
    ],
  },
];
