// import LastReadingPopup from './components/LastReadingPopup.vue'
// import { loginKeyConfig, firstLoginKeyConfig } from "./common/info";
// let busuanzi;

export default ({
                  Vue, // VuePress 正在使用的 Vue 构造函数
                  options, // 附加到根实例的一些选项
                  router, // 当前应用的路由实例
                  siteData, // 站点元数据
                  isServer // 当前应用配置是处于 服务端渲染 或 客户端
                }) => {
  /**
   * 百度 pv 统计，不使用的可以去掉或注释掉
   */
  // router.beforeEach((to, from, next) => {
  //   // 路由切换，触发百度的 pv 统计
  //   if (typeof _hmt != "undefined") {
  //     if (to.path) {
  //       _hmt.push(["_trackPageview", to.path]);
  //     }
  //   }
  //   next();
  // })
  /**
   * 私密文章验证
   */
  if (!isServer) {
    // 如果开启了私密文章验证
    router.beforeEach((to, from, next) => {
      next()
    })

    router.afterEach((to, form) => {
      docReady(function() {
        console.log(
          '\n %c Jpom %c ' + `${location.protocol}//${location.host}` + ' \n',
          'color: #ffffff; background: #f1404b; padding:5px 0;',
          'background: #030307; padding:5px 0;'
        )
        //check if wwads' fire function was blocked after document is ready with 3s timeout (waiting the ad loading)
        loopExecute(
          function() {
            if (
              window._AdBlockInit === undefined ||
              $('.wwads-cn').children().length === 0
            ) {
              return false
            }
            changeAdHideEvent()
            return true
          },
          10,
          function() {
            ABDetected()
            ABDetectedHeader()
            changeAdHideEvent()
          }
        )
        // 图片悬停显示描述
        imgAddLayerTip()
        isStarRepo(to.path)
      })
    })
  }

  function changeAdHideEvent() {
    // 删除事件改为隐藏事件
    setTimeout(() => {
      const pageAD = document.querySelector('.wwads-cn')
      if (!pageAD) return
      const btnEl = pageAD.querySelector('.wwads-hide')
      if (btnEl) {
        btnEl.onclick = () => {
          pageAD.style.display = 'none'
        }
      }
      // 显示广告模块
      if (pageAD.style.display === 'none') {
        $(pageAD).css('cssText', 'display:flex !important')
      }
    }, 900)
  }

  function ABDetectedHeader() {
    // 万维广告“禁止”广告拦截
    // function called if wwads is blocked
    // https://github.com/bytegravity/whitelist-wwads

    if (checkIsLocal()) {
      // 本地环境不显示
      return
    }
    var adDivDom = document.querySelector('.adBlockDetected_div')
    if (adDivDom) {
      // console.log("存在");
      return
    }
    // console.log(adDivDom);
    // console.error("_AdBlockInit");
    var adBlockDetected_div = document.createElement('div')
    adBlockDetected_div.className = 'adBlockDetected_div'
    document.body.appendChild(adBlockDetected_div)
    // console.log(document.querySelector(".adBlockDetected_div"));
    var navbar = document.querySelector('.navbar')
    var sidebar = document.querySelector('.sidebar')
    navbar && (navbar.style.cssText = 'transition:top 300ms;top:33px')
    sidebar && (sidebar.style.cssText = 'padding-top:33px')
    adBlockDetected_div.style.cssText =
      'position: fixed; top: 0; left: 0; width: 100%; background: #E01E5A; color: #fff; z-index: 9999999999; font-size: 14px; text-align: center; line-height: 1.5; font-weight: bold; padding-top: 6px; padding-bottom: 6px;'
    adBlockDetected_div.innerHTML =
      '我们的广告服务商 <a style=\'color:#fff;text-decoration:underline\' target=\'_blank\' href=\'https://wwads.cn/page/end-user-privacy\'>并不跟踪您的隐私</a>，为了<b>支持开源项目长期发展</b>，请将我们的网站 <a style=\'color: #fff;text-decoration:underline\' target=\'_blank\' href=\'https://wwads.cn/page/whitelist-wwads\'>加入广告拦截器的白名单</a>。（修改后需刷新页面生效）'
    document.getElementsByTagName('body')[0].appendChild(adBlockDetected_div)
    // add a close button to the right side of the div
    var adBlockDetected_close = document.createElement('div')
    adBlockDetected_close.style.cssText =
      'position: absolute; top: 0; right: 10px; width: 30px; height: 30px; background: #E01E5A; color: #fff; z-index: 9999999999; line-height: 30px; cursor: pointer;'
    adBlockDetected_close.innerHTML = '×'
    adBlockDetected_div.appendChild(adBlockDetected_close)
    // add a click event to the close button
    adBlockDetected_close.onclick = function() {
      this.parentNode.parentNode.removeChild(this.parentNode)
      navbar.style.cssText = 'transition:top 300ms;top:0'
      sidebar && (sidebar.style.cssText = 'padding-top:0')
    }
  }

  // 显示最新版本号
  function titleShowVersion() {
    var mainTitle = $('#main-title')
    if (mainTitle.length && window.JPOM_RELEASE_VERSION) {
      if ($(window).width() > 800) {
        mainTitle.css({
          display: 'inline-block'
        })
        const index = layer.tips(JPOM_RELEASE_VERSION, mainTitle.get(0), {
          tips: 2,
          time: 0,
          offset: 'auto',
          // tipsMore: true,
          anim: 1
        })
        $('#layui-layer' + index + ' .layui-layer-content').css({
          'background-color': 'rgba(0,0,0,.6)'
        })
        $('#layui-layer' + index + ' .layui-layer-TipsG').hide()
        window['mainTitleTipsIndex'] = index
      } else {
        layer.close(window['mainTitleTipsIndex'])
      }
    } else {
      layer.close(window['mainTitleTipsIndex'])
    }
    if (!$('.hero img').hasClass('animationLogo')) {
      $('.hero img').addClass('animationLogo')
    }
  }

  // 图片添加悬停提示
  function imgAddLayerTip() {
    loopExecute(function() {
      titleShowVersion()
      var $hover = $('.hover-alt[alt]')
      if (!$hover.length) {
        return false
      }
      $hover.hover(
        function() {
          var msg = $(this).attr('alt')
          if (msg) {
            window.msgLayer = layer.tips(msg, $(this), {
              tips: 1,
              time: 0
            })
          }
        },
        function() {
          var index = window.msgLayer
          setTimeout(function() {
            layer.close(index)
          }, 1000)
        }
      )
      return true
    }, 20)
  }


  function ABDetected() {
    const h =
      '<style>.wwads-horizontal,.wwads-vertical{background-color:var(--bodyBg);padding:5px;box-sizing:border-box;border-radius:3px;font-family:sans-serif;display:flex;min-width:150px;position:relative;overflow:hidden;}.wwads-horizontal{flex-wrap:wrap;justify-content:center}.wwads-vertical{flex-direction:column;align-items:center;padding-bottom:32px}.wwads-horizontal a,.wwads-vertical a{text-decoration:none}.wwads-horizontal .wwads-img,.wwads-vertical .wwads-img{margin:5px}.wwads-horizontal .wwads-content,.wwads-vertical .wwads-content{margin:5px}.wwads-horizontal .wwads-content{flex:130px}.wwads-vertical .wwads-content{margin-top:10px}.wwads-horizontal .wwads-text,.wwads-content .wwads-text{font-size:14px;line-height:1.4;color:var(--textColor);;-webkit-font-smoothing:antialiased}.wwads-horizontal .wwads-poweredby,.wwads-vertical .wwads-poweredby{display:block;font-size:11px;color:var(--textColor);margin-top:1em}.wwads-vertical .wwads-poweredby{position:absolute;left:10px;bottom:10px}.wwads-horizontal .wwads-poweredby span,.wwads-vertical .wwads-poweredby span{transition:all 0.2s ease-in-out;margin-left:-1em}.wwads-horizontal .wwads-poweredby span:first-child,.wwads-vertical .wwads-poweredby span:first-child{opacity:0}.wwads-horizontal:hover .wwads-poweredby span,.wwads-vertical:hover .wwads-poweredby span{opacity:1;margin-left:0}.wwads-horizontal .wwads-hide,.wwads-vertical .wwads-hide{position:absolute;right:-23px;bottom:-23px;width:46px;height:46px;border-radius:23px;transition:all 0.3s ease-in-out;cursor:pointer;}.wwads-horizontal .wwads-hide:hover,.wwads-vertical .wwads-hide:hover{background:rgb(0 0 0 /0.05)}.wwads-horizontal .wwads-hide svg,.wwads-vertical .wwads-hide svg{position:absolute;left:10px;top:10px;fill:#a6b7bf}.wwads-horizontal .wwads-hide:hover svg,.wwads-vertical .wwads-hide:hover svg{fill:#3E4546}</style>' +
      '<a href=\'https://wwads.cn/page/whitelist-wwads\' class=\'wwads-img\' target=\'_blank\' rel=\'nofollow\'><img src=\'/images/error/wwads.2a3pidhlh4ys.webp\' width=\'130\'></a><div class=\'wwads-content\'><a href=\'https://wwads.cn/page/whitelist-wwads\' class=\'wwads-text\' target=\'_blank\' rel=\'nofollow\'>为了支持开源项目长期发展，请将我们的网站加入广告拦截器的白名单，感谢您的支持！<span style=\'color: #11a8cd\'>如何添加白名单?</span></a><a href=\'https://wwads.cn/page/end-user-privacy\' class=\'wwads-poweredby\' title=\'万维广告 ～ 让广告更优雅，且有用\' target=\'_blank\'><span>万维</span><span>广告</span></a></div><a class=\'wwads-hide\' onclick=\'parentNode.remove()\' title=\'隐藏广告\'><svg xmlns=\'http://www.w3.org/2000/svg\' width=\'6\' height=\'7\'><path d=\'M.879.672L3 2.793 5.121.672a.5.5 0 11.707.707L3.708 3.5l2.12 2.121a.5.5 0 11-.707.707l-2.12-2.12-2.122 2.12a.5.5 0 11-.707-.707l2.121-2.12L.172 1.378A.5.5 0 01.879.672z\'></path></svg></a>'
    const wwadsEl = document.getElementsByClassName('wwads-cn')
    const wwadsContentEl = document.querySelector('.wwads-content')
    if (wwadsEl[0] && !wwadsContentEl) {
      wwadsEl[0].innerHTML = h
      // wwadsEl[0].style.display = "block";
    }
  }

  /**
   * 记录最后一次阅读位置模块，方便下次直接跳转
   * 目前不使用该插件
   */
  // 判断是否绑定时间是否绑定成功
  // let isMounted = false;
  // // 最后一次阅读位置跳转
  // Vue.component(LastReadingPopup.name, LastReadingPopup);
  // Vue.mixin({
  //   // 有多少个 Vue 组件（md 文档），就执行多少次 mounted()，所以利用 if 判断只允许执行一次
  //   mounted() {
  //     if (!isMounted) {
  //       window.addEventListener('unload', this.saveLastReading);  // 卸载窗口前，将数据存储，方便下次可以直接跳转位置
  //       isMounted = true;
  //     }
  //   },
  //   methods: {
  //     saveLastReading() {
  //       localStorage.setItem('lastReading', JSON.stringify({
  //         path: this.$route.path,
  //         scrollTop: document.documentElement.scrollTop,
  //         timestamp: new Date().getTime(),
  //       }))
  //     }
  //   }
  // });
};
