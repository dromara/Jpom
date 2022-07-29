// Head Config
module.exports = [
  ['link', {rel: 'icon', href: '/favicon.ico'}], //favicons，资源放在public文件夹
  ['meta', {name: 'description', content: 'Jpom,项目管理系统,CI/CD,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件,open source'}],
  ['meta', {name: 'keywords', content: 'Jpom,Jpom官网,Jpom文档,开源,项目管理,管理系统,运维,项目运维,服务器运维,CI/CD,devops,自动部署,在线构建'}],
  ['meta', {name: 'theme-color', content: '#11a8cd'}], // 移动浏览器主题颜色
  ['meta', {name: 'og:type', content: 'website'}],
  ['meta', {name: 'og:title', content: 'Jpom 项目管理系统文档'}],
  ['meta', {name: 'og:url', content: 'https://jpom.io/docs'}],
  ['meta', {name: 'og:site_name', content: 'Jpom 文档'}],
  ['meta', {name: 'og:description', content: 'Jpom-项目管理系统,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件'}],
  // <meta name="baidu-site-verification" content="code-te8iliecTI" />
  ['meta', {name: 'baidu-site-verification', content: 'code-te8iliecTI'}],
  // <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
  ['meta', {name: 'viewport', content: 'width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no'}],
  ['script', {src: 'https://unpkg.com/jquery@3.4.1/dist/jquery.min.js'}],
  ['script', {src: 'https://www.layuicdn.com/layer-v3.1.1/layer.js'}],
  ['style', {}, `
  .main-right{
      display: none;
  }
  `],
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

        $(function() {
         setTimeout(function () {
              imgHover();
         }, 2000);
       });
   function imgHover(){
     $(".friends-item-img[alt]").hover(function() {
          var msg = $(this).attr("alt");
          if (msg) {
              window.msgLayer = layer.tips(msg, $(this), {
                  tips: 1,
                  time: 0
              });
          }
      }, function() {
          var index = window.msgLayer;
          setTimeout(function() {
              layer.close(index);
          }, 1000);
      });
   }
   `],
];
