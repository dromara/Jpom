import { defineConfig4CustomTheme } from "vuepress/config";

import plugins from "./config/plugins";
import head from "./config/head";
import themeConfig from "./config/themeConfig";
import { penName, title } from "./common/info";

export default defineConfig4CustomTheme({
  theme: "vdoing", // 使用npm包主题
  // 如果使用 locales 的 title 和 description，则下方的 title 和 description 失效
  // title: penName + title,
  // description: 'Young Kbt个人博客, VuePress搭建, 使用了 Vdoing 主题, 学习Java, Web, 框架, 微服务, 工具, 前端等相关知识, 记录生活和技术路程, 同时分享编程技巧。',
  // lang: "zh-CN",
  // base: "/", // 格式：'/<仓库名>/'， 默认'/'

  port: 2216,
  markdown: {
    lineNumbers: true, // 显示代码块的行号
    extractHeaders: ["h2", "h3", "h4"], // 支持 h2、h3、h4 标题
  },
  // 多语言支持
  locales: {
    "/": {
      lang: "zh-CN",
      title: penName + title,
      description:
        "一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控开源软件",
    },
    // '/en/': {
    //   lang: 'en-US', // 将会被设置为 <html> 的 lang 属性
    //   title: penName + title,
    //   description: 'Young Kbt personal blog, built by vuepress, uses the vdoing theme to learn Java, web, framework, microservices, tools, front-end and other related knowledge, record life and technology journey, and share programming skills at the same time'
    // }
  },
  // 监听文件变化并重新构建
  extraWatchFiles: [".vuepress/config.ts"],
  dest: "dist",
  themeConfig,

  head,

  plugins,
});
