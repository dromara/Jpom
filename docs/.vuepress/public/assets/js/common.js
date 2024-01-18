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
  window.JPOM_RELEASE_VERSION = "2.11.0";
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
  openTipStar();

  function openTipStar() {
    layer.close(window["tipStarIndex"]);
    window["tipStarIndex"] = layer.msg(
      '欢迎您 Star Jpom <a href="https://gitee.com/dromara/Jpom/stargazers" target="_blank">Gitee</a>/' +
        '<a href="https://github.com/dromara/Jpom" target="_blank">Github</a>',
      {
        offset: "rb",
        time: 0,
        anim: 6,
      }
    );
  }

  window.addEventListener("resize", openTipStar);
});

function docReady(t) {
  "complete" === document.readyState || "interactive" === document.readyState
    ? setTimeout(t, 1)
    : document.addEventListener("DOMContentLoaded", t);
}

function loopExecute(fn, loopCount, fail) {
  if (fn && fn()) {
    // 执行成功
    return;
  }
  if (loopCount <= 0) {
    fail && fail();
    // 结束执行
    return;
  }
  setTimeout(() => {
    loopExecute(fn, loopCount - 1, fail);
  }, 500);
}
