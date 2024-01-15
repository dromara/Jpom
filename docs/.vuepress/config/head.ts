import { HeadTags } from "vuepress/config";
import baidutj from "../common/baidutj.js";

// head
export default <HeadTags>[
  ["link", { rel: "icon", href: "/favicon.ico" }], //favicons，资源放在public文件夹
  ["link", { rel: "shortcut icon", href: "/favicon.ico" }], //favicons，资源放在public文件夹
  [
    "meta",
    {
      name: "keywords",
      content:
        "Jpom,Jpom官网,Jpom文档,开源,管理系统,运维,项目运维,服务器运维,原生 ops,CI/CD,devops,自动部署,在线构建,项目管理系统,open source",
    },
  ],
  ["meta", { name: "theme-color", content: "#11a8cd" }], // 移动浏览器主题颜色
  ["meta", { name: "og:type", content: "website" }],
  ["meta", { name: "og:title", content: "Jpom 项目管理系统文档" }],
  ["meta", { name: "og:url", content: "https://jpom.top/" }],
  ["meta", { name: "og:site_name", content: "Jpom 文档" }],
  [
    "meta",
    {
      name: "og:description",
      content:
        "Jpom-项目管理系统,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件",
    },
  ],
  // 阿里在线矢量库
  [
    "link",
    {
      rel: "stylesheet",
      href: "//at.alicdn.com/t/font_3114978_qe0b39no76.css",
    },
  ],
  // 解决 Chrome 网站统计不准确问题
  ["meta", { name: "referrer", content: "no-referrer-when-downgrade" }],
  [
    "noscript",
    {},
    '<meta http-equiv="refresh" content="0; url=https://jpom.io/noscript.html"><style>.theme-vdoing-content { display:none }',
  ],
  // <meta name="baidu-site-verification" content="code-te8iliecTI" />
  ["meta", { name: "baidu-site-verification", content: "code-te8iliecTI" }],
  // <meta name="360-site-verification" content="0a5e4a367ff77232a3c1e9bc83edf7ba" />
  [
    "meta",
    {
      name: "360-site-verification",
      content: "0a5e4a367ff77232a3c1e9bc83edf7ba",
    },
  ],
  // <meta name="wwads-cn-verify" content="6da9003b743b65f4c0ccd295cc484e57" />
  [
    "meta",
    { name: "wwads-cn-verify", content: "6da9003b743b65f4c0ccd295cc484e57" },
  ],
  // <meta name="google-site-verification" content="MA1Gbd51TsfBQa-4bZiaJfOnFiufsvLf5groklfa138" />
  [
    "meta",
    {
      name: "google-site-verification",
      content: "MA1Gbd51TsfBQa-4bZiaJfOnFiufsvLf5groklfa138",
    },
  ],
  [
    "script",
    {
      async: true,
      src: "https://cdn.wwads.cn/js/makemoney.js",
      type: "text/javascript",
    },
  ], // 广告相关，你可以去掉
  // <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
  [
    "meta",
    {
      name: "viewport",
      content:
        "width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no",
    },
  ],
  ["script", { src: "/assets/js/jquery.3.4.1.min.js" }],
  ["script", { src: "/assets/js/layer-3.1.1.js" }],
  ["script", { src: "/assets/js/common.js" }],
  // 百度统计 js
  ["script", {}, baidutj],
];
