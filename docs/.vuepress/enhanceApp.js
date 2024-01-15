// import LastReadingPopup from './components/LastReadingPopup.vue'
// import { loginKeyConfig, firstLoginKeyConfig } from "./common/info";
// let busuanzi;
const loginKeyConfig = "jpom_manager";
const firstLoginKeyConfig = "jpom_first_login";

export default ({
  Vue, // VuePress 正在使用的 Vue 构造函数
  options, // 附加到根实例的一些选项
  router, // 当前应用的路由实例
  siteData, // 站点元数据
  isServer, // 当前应用配置是处于 服务端渲染 或 客户端
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
      if (
        siteData.themeConfig.privatePage &&
        siteData.themeConfig.privatePage.openPrivate
      ) {
        try {
          let {
            username,
            password,
            loginPath,
            loginKey,
            loginSession,
            loginInfo,
            firstLogin,
            firstLoginKey,
          } = siteData.themeConfig.privatePage;
          !loginKey && (loginKey = loginKeyConfig); // 默认为 vdoing_manager
          !firstLoginKey && (firstLoginKey = firstLoginKeyConfig); // 默认为 vdoing_first_login
          // 网站关闭或者刷新后，清除登录状态
          if (loginSession) {
            window.addEventListener("unload", function () {
              localStorage.removeItem(loginKey);
              localStorage.removeItem(firstLoginKey);
            });
          }
          // 如果是登录页面，不需要验证
          if (loginPath == to.path || !loginPath) {
            throw new Error("无需验证");
          }
          // 尝试获取管理员曾经登录的用户信息
          let globalInfo = JSON.parse(localStorage.getItem(loginKey));
          // 管理员用户名密码验证
          if (
            globalInfo &&
            globalInfo.username == username &&
            globalInfo.password == password
          ) {
            // 存在曾经登录信息，如果登录状态过期
            if (new Date() - globalInfo.time > globalInfo.expire) {
              localStorage.removeItem(loginKey);
            } else {
              throw new Error("管理员验证成功！");
            }
          }
          // 整个网站进入前需要验证
          let isAgainLogin = true;
          if (parseInt(firstLogin) == 1 || parseInt(firstLogin) == 2) {
            parseInt(firstLogin) == 2 && (isAgainLogin = false);
            // 尝试获取第一次访问网站曾经登录的用户信息
            let firstLoginInfo = JSON.parse(
              localStorage.getItem(firstLoginKey)
            );
            !firstLoginInfo && jumpToLogin(loginPath, to.path, "first");
            if (firstLoginInfo) {
              // 先判断 loginInfo 是否存在，然后判断 loginInfo 是否对象，最后判断 loginInfo 是否有 firstLoginKey
              if (loginInfo && loginInfo.hasOwnProperty(firstLoginKey)) {
                // 进行 loginInfo 验证
                checkLoginInfo(loginInfo[firstLoginKey], firstLoginInfo) &&
                  jumpToLogin(loginPath, to.path, "first");
              } else {
                jumpToLogin(loginPath, to.path, "first");
              }
            }
          }
          if (to.path == "/") {
            throw new Error("首页不需要验证！");
          }
          // 如果 firstLogin 不等于 2
          if (isAgainLogin) {
            siteData.pages.forEach((item) => {
              // 找出带有 private 的文章
              if (item.path == to.path) {
                if (
                  item.frontmatter.private &&
                  item.frontmatter.private == true
                ) {
                  // 网站关闭或者刷新后，清除登录状态
                  if (loginSession) {
                    window.addEventListener("unload", function () {
                      localStorage.removeItem(item.frontmatter.permalink);
                    });
                  }
                  // 尝试获取该私密文章曾经登录的用户信息
                  let singleInfo = JSON.parse(
                    localStorage.getItem(item.frontmatter.permalink)
                  );
                  // 都不存在登录信息
                  !singleInfo &&
                    jumpToLogin(
                      loginPath,
                      to.path,
                      item.frontmatter.loginInfo ||
                        item.frontmatter.username ||
                        item.frontmatter.password ||
                        item.frontmatter.expire
                        ? "single"
                        : "all"
                    );

                  // 单个文章私密验证
                  if (
                    (item.frontmatter.username && item.frontmatter.password) ||
                    item.frontmatter.loginInfo
                  ) {
                    // 不存在登录信息，则跳转到登录页面
                    !singleInfo && jumpToLogin(loginPath, to.path, "single");
                    // 存在曾经登录信息，如果登录状态过期
                    if (new Date() - singleInfo.time > singleInfo.expire) {
                      localStorage.removeItem(item.frontmatter.permalink);
                      jumpToLogin(loginPath, to.path, "single");
                    }
                    // 是否需要登录
                    let isLogin = true;
                    // 对 loginInfo 进行验证
                    if (Array.isArray(item.frontmatter.loginInfo)) {
                      isLogin = checkLoginInfo(
                        item.frontmatter.loginInfo,
                        singleInfo
                      );
                    }
                    // 如果 loginInfo 不存在，则进行单文章的用户名密码验证
                    if (
                      isLogin &&
                      singleInfo.username !== item.frontmatter.username &&
                      singleInfo.password !== item.frontmatter.password
                    ) {
                      jumpToLogin(loginPath, to.path, "single");
                    }
                  } else {
                    // 全局私密验证
                    let isLogin = true;
                    // 先判断 loginInfo 是否存在，然后判断 loginInfo 是否对象，最后判断 loginInfo 是否有该文章的 permalink
                    if (loginInfo && loginInfo.hasOwnProperty(to.path)) {
                      isLogin = checkLoginInfo(loginInfo[to.path], singleInfo);
                    }
                    // 如果 loginInfo 验证失败
                    isLogin && jumpToLogin(loginPath, to.path, "all");
                  }
                }
              }
            });
          }
        } catch (e) {}
      }

      next();
      docReady(function () {
        console.log(
          "\n %c Jpom %c " + `${location.protocol}//${location.host}` + " \n",
          "color: #ffffff; background: #f1404b; padding:5px 0;",
          "background: #030307; padding:5px 0;"
        );
        //check if wwads' fire function was blocked after document is ready with 3s timeout (waiting the ad loading)
        loopExecute(
          function () {
            if (
              window._AdBlockInit === undefined ||
              $(".wwads-cn").children().length === 0
            ) {
              return false;
            }
            changeAdHideEvent();
            return true;
          },
          10,
          function () {
            ABDetected();
            changeAdHideEvent();
          }
        );
      });
    });

    router.afterEach((to, form) => {
      docReady(function () {
        // 图片悬停显示描述
        imgAddLayerTip();
      });
    });
  }

  function changeAdHideEvent() {
    // 删除事件改为隐藏事件
    setTimeout(() => {
      const pageAD = document.querySelector(".wwads-cn");
      if (!pageAD) return;
      const btnEl = pageAD.querySelector(".wwads-hide");
      if (btnEl) {
        btnEl.onclick = () => {
          pageAD.style.display = "none";
        };
      }
      // 显示广告模块
      if (pageAD.style.display === "none") {
        $(pageAD).css("cssText", "display:flex !important");
      }
    }, 900);
  }

  function titleShowVersion() {
    var mainTitle = $("#main-title");
    if (mainTitle.length && window.JPOM_RELEASE_VERSION) {
      if ($(window).width() > 800) {
        mainTitle.css({
          display: "inline-block",
        });
        const index = layer.tips(JPOM_RELEASE_VERSION, mainTitle.get(0), {
          tips: 2,
          time: 0,
          offset: "auto",
          // tipsMore: true,
          anim: 1,
        });
        $("#layui-layer" + index + " .layui-layer-content").css({
          "background-color": "rgba(0,0,0,.6)",
        });
        $("#layui-layer" + index + " .layui-layer-TipsG").hide();
        window["mainTitleTipsIndex"] = index;
      } else {
        layer.close(window["mainTitleTipsIndex"]);
      }
    } else {
      layer.close(window["mainTitleTipsIndex"]);
    }
    if (!$(".hero img").hasClass("animationLogo")) {
      $(".hero img").addClass("animationLogo");
    }
  }

  function imgAddLayerTip() {
    loopExecute(function () {
      titleShowVersion();
      var $hover = $(".hover-alt[alt]");
      if (!$hover.length) {
        return false;
      }
      $hover.hover(
        function () {
          var msg = $(this).attr("alt");
          if (msg) {
            window.msgLayer = layer.tips(msg, $(this), {
              tips: 1,
              time: 0,
            });
          }
        },
        function () {
          var index = window.msgLayer;
          setTimeout(function () {
            layer.close(index);
          }, 1000);
        }
      );
      return true;
    }, 20);
  }
  /**
   * 检查 loginInfo 里的用户名和密码，userInfo 为曾经登录的信息
   * 匹配成功返回 false，失败返回 true
   */
  function checkLoginInfo(loginInfo, userInfo) {
    try {
      loginInfo.forEach((info) => {
        if (
          userInfo.username == info.username &&
          userInfo.password == info.password
        ) {
          // 利用异常机制跳出 forEach 循环，break、return、continue 不会起作用
          throw new Error();
        }
      });
    } catch (error) {
      return false;
    }
    return true;
  }
  /**
   * 跳转到登录页面
   * loginPath：登录页面的 permalink
   * toPath：当前页面的 permalink，verifyMode：验证方式
   */
  function jumpToLogin(loginPath, toPath, verifyMode) {
    router.push({
      path: loginPath,
      query: {
        toPath: toPath,
        verifyMode: verifyMode, // 单个文章验证（single）或全局验证（all）或网站验证（first）
      },
    });
    throw new Error("请先登录！");
  }

  function ABDetected() {
    const h =
      "<style>.wwads-horizontal,.wwads-vertical{background-color:var(--bodyBg);padding:5px;box-sizing:border-box;border-radius:3px;font-family:sans-serif;display:flex;min-width:150px;position:relative;overflow:hidden;}.wwads-horizontal{flex-wrap:wrap;justify-content:center}.wwads-vertical{flex-direction:column;align-items:center;padding-bottom:32px}.wwads-horizontal a,.wwads-vertical a{text-decoration:none}.wwads-horizontal .wwads-img,.wwads-vertical .wwads-img{margin:5px}.wwads-horizontal .wwads-content,.wwads-vertical .wwads-content{margin:5px}.wwads-horizontal .wwads-content{flex:130px}.wwads-vertical .wwads-content{margin-top:10px}.wwads-horizontal .wwads-text,.wwads-content .wwads-text{font-size:14px;line-height:1.4;color:var(--textColor);;-webkit-font-smoothing:antialiased}.wwads-horizontal .wwads-poweredby,.wwads-vertical .wwads-poweredby{display:block;font-size:11px;color:var(--textColor);margin-top:1em}.wwads-vertical .wwads-poweredby{position:absolute;left:10px;bottom:10px}.wwads-horizontal .wwads-poweredby span,.wwads-vertical .wwads-poweredby span{transition:all 0.2s ease-in-out;margin-left:-1em}.wwads-horizontal .wwads-poweredby span:first-child,.wwads-vertical .wwads-poweredby span:first-child{opacity:0}.wwads-horizontal:hover .wwads-poweredby span,.wwads-vertical:hover .wwads-poweredby span{opacity:1;margin-left:0}.wwads-horizontal .wwads-hide,.wwads-vertical .wwads-hide{position:absolute;right:-23px;bottom:-23px;width:46px;height:46px;border-radius:23px;transition:all 0.3s ease-in-out;cursor:pointer;}.wwads-horizontal .wwads-hide:hover,.wwads-vertical .wwads-hide:hover{background:rgb(0 0 0 /0.05)}.wwads-horizontal .wwads-hide svg,.wwads-vertical .wwads-hide svg{position:absolute;left:10px;top:10px;fill:#a6b7bf}.wwads-horizontal .wwads-hide:hover svg,.wwads-vertical .wwads-hide:hover svg{fill:#3E4546}</style>" +
      "<a href='https://wwads.cn/page/whitelist-wwads' class='wwads-img' target='_blank' rel='nofollow'><img src='/images/error/wwads.2a3pidhlh4ys.webp' width='130'></a><div class='wwads-content'><a href='https://wwads.cn/page/whitelist-wwads' class='wwads-text' target='_blank' rel='nofollow'>为了本站的长期运营，请将我们的网站加入广告拦截器的白名单，感谢您的支持！<span style='color: #11a8cd'>如何添加白名单?</span></a><a href='https://wwads.cn/page/end-user-privacy' class='wwads-poweredby' title='万维广告 ～ 让广告更优雅，且有用' target='_blank'><span>万维</span><span>广告</span></a></div><a class='wwads-hide' onclick='parentNode.remove()' title='隐藏广告'><svg xmlns='http://www.w3.org/2000/svg' width='6' height='7'><path d='M.879.672L3 2.793 5.121.672a.5.5 0 11.707.707L3.708 3.5l2.12 2.121a.5.5 0 11-.707.707l-2.12-2.12-2.122 2.12a.5.5 0 11-.707-.707l2.121-2.12L.172 1.378A.5.5 0 01.879.672z'></path></svg></a>";
    const wwadsEl = document.getElementsByClassName("wwads-cn");
    const wwadsContentEl = document.querySelector(".wwads-content");
    if (wwadsEl[0] && !wwadsContentEl) {
      wwadsEl[0].innerHTML = h;
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
