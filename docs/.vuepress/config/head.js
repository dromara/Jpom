// Head Config
module.exports = [
  ['link', {rel: 'icon', href: '/favicon.ico'}], //favicons，资源放在public文件夹
  ['meta', {name: 'description', content: 'Jpom,项目管理系统,CI/CD,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件,open source'}],
  ['meta', {name: 'keywords', content: 'Jpom,Jpom官网,Jpom文档,开源,项目管理,管理系统,运维,项目运维,服务器运维,CI/CD,devops,自动部署,在线构建'}],
  ['meta', {name: 'theme-color', content: '#11a8cd'}], // 移动浏览器主题颜色
  ['meta', {name: 'og:type', content: 'website'}],
  ['meta', {name: 'og:title', content: 'Jpom 项目管理系统文档'}],
  ['meta', {name: 'og:url', content: 'https://jpom.top/'}],
  ['meta', {name: 'og:site_name', content: 'Jpom 文档'}],
  ['meta', {name: 'og:description', content: 'Jpom-项目管理系统,一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件'}],
  // <meta name="baidu-site-verification" content="code-te8iliecTI" />
  ['meta', {name: 'baidu-site-verification', content: 'code-te8iliecTI'}],
  // <meta name="360-site-verification" content="0a5e4a367ff77232a3c1e9bc83edf7ba" />
  ['meta', {name: '360-site-verification', content: '0a5e4a367ff77232a3c1e9bc83edf7ba'}],
  // <meta name="wwads-cn-verify" content="6da9003b743b65f4c0ccd295cc484e57" />
  ['meta', {name: 'wwads-cn-verify', content: '6da9003b743b65f4c0ccd295cc484e57'}],
  ['script', {async: true, src: 'https://cdn.wwads.cn/js/makemoney.js', type: 'text/javascript'}], // 广告相关，你可以去掉
  // <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
  ['meta', {name: 'viewport', content: 'width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no'}],
  ['script', {src: '/assets/js/jquery.3.4.1.min.js'}],
  ['script', {src: '/assets/js/layer-3.1.1.js'}],
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
  ['script', {}, `
    // 万维广告“禁止”广告拦截
    // function called if wwads is blocked
    // https://github.com/bytegravity/whitelist-wwads
    function ABDetected() {
      var adBlockDetected_div = document.createElement("div");
      document.body.appendChild(adBlockDetected_div);
      var navbar = document.querySelector(".navbar");
      navbar.style.cssText="transition:top 300ms;top:33px";
      adBlockDetected_div.style.cssText = "position: fixed; top: 0; left: 0; width: 100%; background: #E01E5A; color: #fff; z-index: 9999999999; font-size: 14px; text-align: center; line-height: 1.5; font-weight: bold; padding-top: 6px; padding-bottom: 6px;";
      adBlockDetected_div.innerHTML = "我们的广告服务商 <a style='color:#fff;text-decoration:underline' target='_blank' href='https://wwads.cn/page/end-user-privacy'>并不跟踪您的隐私</a>，为了支持本站的长期运营，请将我们的网站 <a style='color: #fff;text-decoration:underline' target='_blank' href='https://wwads.cn/page/whitelist-wwads'>加入广告拦截器的白名单</a>。";
      document.getElementsByTagName("body")[0].appendChild(adBlockDetected_div);
      // add a close button to the right side of the div
      var adBlockDetected_close = document.createElement("div");
      adBlockDetected_close.style.cssText = "position: absolute; top: 0; right: 10px; width: 30px; height: 30px; background: #E01E5A; color: #fff; z-index: 9999999999; line-height: 30px; cursor: pointer;";
      adBlockDetected_close.innerHTML = "×";
      adBlockDetected_div.appendChild(adBlockDetected_close);
      // add a click event to the close button
      adBlockDetected_close.onclick = function() {
      this.parentNode.parentNode.removeChild(this.parentNode);
      navbar.style.cssText="transition:top 300ms;top:0";
      };
    }

    function docReady(t) {
      "complete" === document.readyState ||
      "interactive" === document.readyState
        ? setTimeout(t, 1)
        : document.addEventListener("DOMContentLoaded", t);
    }

    //check if wwads' fire function was blocked after document is ready with 3s timeout (waiting the ad loading)
    docReady(function () {
      setTimeout(function () {
        if( window._AdBlockInit === undefined ){
            ABDetected();
        }
      }, 3000);
    });
  `],
  ['style', {}, `
  .buttons{
      bottom: 15.5rem !important;
  }
  .custom-html-window {
      right: 2px !important;
  }
  `],
];
