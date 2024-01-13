(function () {
  var bp = document.createElement("script");
  bp.type = "text/javascript";
  bp.async = true;
  bp.src = "https://www.googletagmanager.com/gtag/js?id=G-FYSG66S4HQ";
  var s = document.getElementsByTagName("script")[0];
  s.parentNode.insertBefore(bp, s);

  window.dataLayer = window.dataLayer || [];

  function gtag() {
    dataLayer.push(arguments);
  }

  gtag("js", new Date());
  gtag("config", "G-FYSG66S4HQ");
})();

$(function () {
  // 图片悬停显示描述
  loopExecute(function () {
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

  // 检查域名
  const localHosts = [
    "localhost",
    "127.0.0.1",
    "jpom.top",
    "webcache.googleusercontent.com",
    "192.168.",
  ];

  function checkDomain() {
    if (localHosts.includes(location.hostname)) {
      return;
    }
    for (let item in localHosts) {
      if (location.hostname.indexOf(localHosts[item]) > -1) {
        return;
      }
    }
    console.log(location.host + "  =>  jpom.top");
    layer.msg("当前访问的地址不是主站，2秒后自动切换到主站", {
      offset: "t",
      anim: 2,
    });
    setTimeout(function () {
      location.href = `https://jpom.top${location.pathname}${location.search}${location.hash}`;
    }, 2000);
  }

  checkDomain();

  // 滚动左边菜单到可视区域
  loopExecute(function () {
    const $dom = $(".sidebar-links .active");
    if (!$dom.length) {
      return false;
    }
    $dom.get(0).scrollIntoView({ block: "center" });
    return true;
  }, 20);

  // 提醒 star
  loopExecute(function () {
    let $themDom = $(".theme-mode-but");
    if (!$themDom.length) {
      return false;
    }
    layer.msg(
      '欢迎您 Star Jpom <a href="https://gitee.com/dromara/Jpom/stargazers" target="_blank">Gitee</a>/' +
        '<a href="https://github.com/dromara/Jpom" target="_blank">Github</a>',
      {
        offset: "rb",
        time: 1000 * 60 * 60,
        anim: 6,
      }
    );
    return true;
  }, 20);
});

(function () {
  // 万维广告“禁止”广告拦截
  // function called if wwads is blocked
  // https://github.com/bytegravity/whitelist-wwads
  function ABDetected() {
    if (
      location.hostname === "127.0.0.1" ||
      location.hostname === "localhost" ||
      location.hostname.indexOf("192.168.") > -1
    ) {
      // 本地环境不显示
      return;
    }
    console.error("_AdBlockInit");
    var adBlockDetected_div = document.createElement("div");
    document.body.appendChild(adBlockDetected_div);
    var navbar = document.querySelector(".navbar");
    navbar.style.cssText = "transition:top 300ms;top:33px";
    adBlockDetected_div.style.cssText =
      "position: fixed; top: 0; left: 0; width: 100%; background: #E01E5A; color: #fff; z-index: 9999999999; font-size: 14px; text-align: center; line-height: 1.5; font-weight: bold; padding-top: 6px; padding-bottom: 6px;";
    adBlockDetected_div.innerHTML =
      "我们的广告服务商 <a style='color:#fff;text-decoration:underline' target='_blank' href='https://wwads.cn/page/end-user-privacy'>并不跟踪您的隐私</a>，为了支持本站的长期运营，请将我们的网站 <a style='color: #fff;text-decoration:underline' target='_blank' href='https://wwads.cn/page/whitelist-wwads'>加入广告拦截器的白名单</a>。";
    document.getElementsByTagName("body")[0].appendChild(adBlockDetected_div);
    // add a close button to the right side of the div
    var adBlockDetected_close = document.createElement("div");
    adBlockDetected_close.style.cssText =
      "position: absolute; top: 0; right: 10px; width: 30px; height: 30px; background: #E01E5A; color: #fff; z-index: 9999999999; line-height: 30px; cursor: pointer;";
    adBlockDetected_close.innerHTML = "×";
    adBlockDetected_div.appendChild(adBlockDetected_close);
    // add a click event to the close button
    adBlockDetected_close.onclick = function () {
      this.parentNode.parentNode.removeChild(this.parentNode);
      navbar.style.cssText = "transition:top 300ms;top:0";
    };
  }

  //check if wwads' fire function was blocked after document is ready with 3s timeout (waiting the ad loading)
  docReady(function () {
    setTimeout(function () {
      if (window._AdBlockInit === undefined) {
        ABDetected();
      }
    }, 3000);
  });
})();

function docReady(t) {
  "complete" === document.readyState || "interactive" === document.readyState
    ? setTimeout(t, 1)
    : document.addEventListener("DOMContentLoaded", t);
}

function loopExecute(fn, loopCount = 20) {
  if (fn && fn()) {
    // 执行成功
    return;
  }
  if (loopCount <= 0) {
    // 结束执行
    return;
  }
  setTimeout(() => {
    loopExecute(fn, loopCount - 1);
  }, 500);
}
