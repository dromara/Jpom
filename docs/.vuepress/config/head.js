// Head Config
module.exports = [
    ['link', {rel: 'icon', href: '/favicon.ico'}], //favicons，资源放在public文件夹
    ['meta', {name: 'description', content: 'Jpom,项目管理系统,CI/CD,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件,open source'}],
    ['meta', {name: 'keywords', content: 'Jpom,Jpom官网,Jpom文档,开源,Java项目管理,Jar管理,Java管理系统,服务器项目运维,CI/CD,devops'}],
    ['meta', {name: 'theme-color', content: '#11a8cd'}], // 移动浏览器主题颜色
    ['meta', {name: 'og:type', content: 'website'}],
    ['meta', {name: 'og:title', content: 'Jpom 项目管理系统文档'}],
    ['meta', {name: 'og:url', content: 'https://jpom.io/docs'}],
    ['meta', {name: 'og:site_name', content: 'Jpom 文档'}],
    ['meta', {name: 'og:description', content: 'Jpom-项目管理系统,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件'}],

    ['script', {}, `
    (function () {
        var bp = document.createElement('script');
        var curProtocol = window.location.protocol.split(':')[0];
        if (curProtocol === 'https') {
            bp.src = 'https://zz.bdstatic.com/linksubmit/push.js';
        } else {
            bp.src = 'http://push.zhanzhang.baidu.com/push.js';
        }
        var s = document.getElementsByTagName("script")[0];
        s.parentNode.insertBefore(bp, s);
    })();
  `],
    ['script', {}, `
        var bp = document.createElement('script');
        bp.src = 'https://www.googletagmanager.com/gtag/js?id=G-FYSG66S4HQ';
        var s = document.getElementsByTagName("script")[0];
        s.parentNode.insertBefore(bp, s);
        (function() {
            window.dataLayer = window.dataLayer || [];
        
            function gtag() {
                dataLayer.push(arguments);
            }
            gtag('js', new Date());
            gtag('config', 'G-FYSG66S4HQ');
        })();
   `]
];
