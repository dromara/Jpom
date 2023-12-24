<template></template>

<script>
import fetch from "../webSiteInfo/busuanzi";
export default {
  mounted() {
    // 首页不初始页面信息
    if (this.$route.path != "/") {
      this.initPageInfo();
    }
  },
  watch: {
    $route(to, from) {
      // 如果页面是非首页，# 号也会触发路由变化，这里要排除掉
      if (
        to.path !== "/" &&
        to.path !== from.path &&
        this.$themeConfig.blogInfo
      ) {
        this.initPageInfo();
      }
    },
  },
  methods: {
    /**
     * 初始化页面信息
     */
    initPageInfo() {
      if (this.$frontmatter.article == undefined || this.$frontmatter.article) {
        // 排除掉 article 为 false 的文章
        const { eachFileWords, pageView, pageIteration, readingTime } =
          this.$themeConfig.blogInfo;
        // 下面两个 if 可以调换位置，从而让文章的浏览量和字数交换位置
        if (eachFileWords) {
          try {
            eachFileWords.forEach((itemFile) => {
              if (itemFile.permalink == this.$frontmatter.permalink) {
                // this.addPageWordsCount 和 if 可以调换位置，从而让文章的字数和预阅读时间交换位置
                this.addPageWordsCount(itemFile.wordsCount);
                if (readingTime || readingTime == undefined) {
                  this.addReadTimeCount(itemFile.readingTime);
                }
                throw new Error();
              }
            });
          } catch (error) {}
        }
        if (pageView || pageView == undefined) {
          this.addPageView();
          this.getPageViewCouter(pageIteration);
        }
        return;
      }
    },
    /**
     * 文章页的访问量
     */
    getPageViewCouter(iterationTime = 3000) {
      fetch();
      let i = 0;
      var defaultCouter = "9999";
      // 如果只需要第一次获取数据（可能获取失败），可注释掉 setTimeout 内容，此内容是第一次获取失败后，重新获取访问量
      // 可能会导致访问量再次 + 1 原因：取决于 setTimeout 的时间（需求调节），setTimeout 太快导致第一个获取的数据没返回，就第二次获取，导致结果返回 + 2 的数据
      setTimeout(() => {
        let pageView = document.querySelector(".view-data");
        if (pageView && pageView.innerText == "") {
          let interval = setInterval(() => {
            // 再次判断原因：防止进入 setInterval 的瞬间，访问量获取成功
            if (pageView && pageView.innerText == "") {
              i += iterationTime;
              if (i > iterationTime * 5) {
                pageView.innerText = defaultCouter;
                clearInterval(interval); // 5 次后无法获取，则取消获取
              }
              if (pageView.innerText == "") {
                // 手动获取访问量
                fetch();
              } else {
                clearInterval(interval);
              }
            } else {
              clearInterval(interval);
            }
          }, iterationTime);
          // 绑定 beforeDestroy 生命钩子，清除定时器
          this.$once("hook:beforeDestroy", () => {
            clearInterval(interval);
            interval = null;
          });
        }
      }, iterationTime);
    },
    /**
     * 添加浏览量元素
     */
    addPageView() {
      let pageView = document.querySelector(".page-view");
      if (pageView) {
        pageView.innerHTML =
          '<a style="color: #888; margin-left: 3px" href="javascript:;" id="busuanzi_value_page_pv" class="view-data"><i title="正在获取..." class="loading iconfont icon-loading"></i></a>';
      } else {
        // 创建访问量的元素
        let template = document.createElement("div");
        template.title = "浏览量";
        template.className = "page-view iconfont icon-view";
        template.style.float = "left";
        template.style.marginLeft = "20px";
        template.style.fontSize = "0.8rem";
        template.innerHTML =
          '<a style="color: #888; margin-left: 3px" href="javascript:;" id="busuanzi_value_page_pv" class="view-data"><i title="正在获取..." class="loading iconfont icon-loading"></i></a>';
        // 添加 loading 效果
        let style = document.createElement("style");
        style.innerHTML = `@keyframes turn {
        0% {
          transform: rotate(0deg);
        }
        100% {
          transform: rotate(360deg);
        }
      }
      .loading {
        display: inline-block;
        animation: turn 1s linear infinite;
        -webkit-animation: turn 1s linear infinite;
      }`;
        document.head.appendChild(style);
        this.mountedView(template);
      }
    },
    /**
     * 添加当前文章页的字数元素
     */
    addPageWordsCount(wordsCount = 0) {
      let words = document.querySelector(".book-words");
      if (words) {
        words.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${wordsCount}</a>`;
      } else {
        let template = document.createElement("div");
        template.title = "文章字数";
        template.className = "book-words iconfont icon-book";
        template.style.float = "left";
        template.style.marginLeft = "20px";
        template.style.fontSize = "0.8rem";

        template.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${wordsCount}</a>`;
        this.mountedView(template);
      }
    },
    /**
     * 添加预计的阅读时间
     */
    addReadTimeCount(readTimeCount = 0) {
      let reading = document.querySelector(".reading-time");
      if (reading) {
        reading.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${readTimeCount}</a>`;
      } else {
        let template = document.createElement("div");
        template.title = "预阅读时长";
        template.className = "reading-time iconfont icon-shijian";
        template.style.float = "left";
        template.style.marginLeft = "20px";
        template.style.fontSize = "0.8rem";
        template.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${readTimeCount}</a>`;
        this.mountedView(template);
      }
    },
    /**
     * 挂载目标到页面上
     */
    mountedView(
      template,
      mountedIntervalTime = 100,
      moutedParentEvent = ".articleInfo-wrap > .articleInfo > .info"
    ) {
      let i = 0;
      let parentElement = document.querySelector(moutedParentEvent);
      if (parentElement) {
        if (!this.isMountedView(template, parentElement)) {
          parentElement.appendChild(template);
        }
      } else {
        let interval = setInterval(() => {
          parentElement = document.querySelector(moutedParentEvent);
          if (parentElement) {
            if (!this.isMountedView(template, parentElement)) {
              parentElement.appendChild(template);
              clearInterval(interval);
            }
          } else if (i > 1 * 10) {
            // 10 秒后清除
            clearInterval(interval);
          }
        }, mountedIntervalTime);
        // 绑定 beforeDestroy 生命钩子，清除定时器
        this.$once("hook:beforeDestroy", () => {
          clearInterval(interval);
          interval = null;
        });
      }
    },
    /**
     * 如果元素存在，则删除
     */
    removeElement(selector) {
      var element = document.querySelector(selector);
      element && element.parentNode.removeChild(element);
    },
    /**
     * 目标是否已经挂载在页面上
     */
    isMountedView(element, parentElement) {
      if (element.parentNode == parentElement) {
        return true;
      } else {
        return false;
      }
    },
  },
  // 防止重写编译时，导致页面信息重复出现问题
  beforeMount() {
    this.removeElement(".page-view");
    this.removeElement(".book-words");
    this.removeElement(".reading-time");
  },
};
</script>

<style></style>
