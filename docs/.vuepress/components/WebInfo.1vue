<template>
  <!-- Young Kbt -->
  <div class="web-info card-box">
    <div class="webinfo-title">
      <i
        class="iconfont icon-award"
        style="font-size: 0.875rem; font-weight: 900; width: 1.25em"
      ></i>
      <span>站点信息</span>
    </div>
    <div class="webinfo-item">
      <div class="webinfo-item-title">文章数目：</div>
      <div class="webinfo-content">{{ mdFileCount }} 篇</div>
    </div>

    <div class="webinfo-item">
      <div class="webinfo-item-title">已运行时间：</div>
      <div class="webinfo-content">
        {{ createToNowDay != 0 ? createToNowDay + " 天" : "不到一天" }}
      </div>
    </div>

    <div class="webinfo-item">
      <div class="webinfo-item-title">本站总字数：</div>
      <div class="webinfo-content">{{ totalWords }} 字</div>
    </div>

    <div class="webinfo-item">
      <div class="webinfo-item-title">最后活动时间：</div>
      <div class="webinfo-content">
        {{ lastActiveDate == "刚刚" ? "刚刚" : lastActiveDate + "前" }}
      </div>
    </div>

    <div v-if="indexView" class="webinfo-item">
      <div class="webinfo-item-title">本站被访问了：</div>
      <div class="webinfo-content">
        <span id="busuanzi_value_site_pv" class="web-site-pv"
          ><i title="正在获取..." class="loading iconfont icon-loading"></i>
        </span>
        次
      </div>
    </div>

    <div v-if="indexView" class="webinfo-item">
      <div class="webinfo-item-title">您的访问排名：</div>
      <div class="webinfo-content busuanzi">
        <span id="busuanzi_value_site_uv" class="web-site-uv"
          ><i title="正在获取..." class="loading iconfont icon-loading"></i>
        </span>
        名
      </div>
    </div>
  </div>
</template>

<script>
import { dayDiff, timeDiff, lastUpdatePosts } from "../webSiteInfo/utils";
import fetch from "../webSiteInfo/busuanzi"; // 统计量
export default {
  data() {
    return {
      // Young Kbt
      mdFileCount: 0, // markdown 文档总数
      createToNowDay: 0, // 博客创建时间距今多少天
      lastActiveDate: "", // 最后活动时间
      totalWords: 0, // 本站总字数
      indexView: true, // 开启访问量和排名统计
    };
  },
  computed: {
    $lastUpdatePosts() {
      return lastUpdatePosts(this.$filterPosts);
    },
  },
  mounted() {
    // Young Kbt
    if (Object.keys(this.$themeConfig.blogInfo).length > 0) {
      const {
        blogCreate,
        mdFileCountType,
        totalWords,
        moutedEvent,
        eachFileWords,
        indexIteration,
        indexView,
      } = this.$themeConfig.blogInfo;
      this.createToNowDay = dayDiff(blogCreate);
      if (mdFileCountType != "archives") {
        this.mdFileCount = mdFileCountType.length;
      } else {
        this.mdFileCount = this.$filterPosts.length;
      }
      if (totalWords == "archives" && eachFileWords) {
        let archivesWords = 0;
        eachFileWords.forEach((itemFile) => {
          if (itemFile.wordsCount < 1000) {
            archivesWords += itemFile.wordsCount;
          } else {
            let wordsCount = itemFile.wordsCount.slice(
              0,
              itemFile.wordsCount.length - 1
            );
            archivesWords += wordsCount * 1000;
          }
        });
        this.totalWords = Math.round(archivesWords / 100) / 10 + "k";
      } else if (totalWords == "archives") {
        this.totalWords = 0;
        console.log(
          "如果 totalWords = 'archives'，必须传入 eachFileWords，显然您并没有传入！"
        );
      } else {
        this.totalWords = totalWords;
      }
      // 最后一次活动时间
      this.lastActiveDate = timeDiff(this.$lastUpdatePosts[0].lastUpdated);
      this.mountedWebInfo(moutedEvent);
      // 获取访问量和排名
      this.indexView = indexView == undefined ? true : indexView;
      if (this.indexView) {
        this.getIndexViewCouter(indexIteration);
      }
    }
  },
  methods: {
    /**
     * 挂载站点信息模块
     */
    mountedWebInfo(moutedEvent = ".tags-wrapper") {
      let interval = setInterval(() => {
        const tagsWrapper = document.querySelector(moutedEvent);
        const webInfo = document.querySelector(".web-info");
        if (tagsWrapper && webInfo) {
          if (!this.isSiblilngNode(tagsWrapper, webInfo)) {
            tagsWrapper.parentNode.insertBefore(
              webInfo,
              tagsWrapper.nextSibling
            );
            clearInterval(interval);
          }
        }
      }, 200);
    },
    /**
     * 挂载在兄弟元素后面，说明当前组件是 siblingNode 变量
     */
    isSiblilngNode(element, siblingNode) {
      if (element.siblingNode == siblingNode) {
        return true;
      } else {
        return false;
      }
    },
    /**
     * 首页的统计量
     */
    getIndexViewCouter(iterationTime = 3000) {
      fetch();
      var i = 0;
      var defaultCouter = "9999";
      // 如果只需要第一次获取数据（可能获取失败），可注释掉 setTimeout 内容，此内容是第一次获取失败后，重新获取访问量
      // 可能会导致访问量再次 + 1 原因：取决于 setTimeout 的时间（需求调节），setTimeout 太快导致第一个获取的数据没返回，就第二次获取，导致结果返回 + 2 的数据
      setTimeout(() => {
        let indexUv = document.querySelector(".web-site-pv");
        let indexPv = document.querySelector(".web-site-uv");
        if (
          indexPv &&
          indexUv &&
          indexPv.innerText == "" &&
          indexUv.innerText == ""
        ) {
          let interval = setInterval(() => {
            // 再次判断原因：防止进入 setInterval 的瞬间，访问量获取成功
            if (
              indexPv &&
              indexUv &&
              indexPv.innerText == "" &&
              indexUv.innerText == ""
            ) {
              i += iterationTime;
              if (i > iterationTime * 5) {
                indexPv.innerText = defaultCouter;
                indexUv.innerText = defaultCouter;
                clearInterval(interval); // 5 次后无法获取，则取消获取
              }
              if (indexPv.innerText == "" && indexUv.innerText == "") {
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
    beforeMount() {
      let webInfo = document.querySelector(".web-info");
      webInfo && webInfo.parentNode.removeChild(webInfo);
    },
  },
};
</script>

<style scoped>
.web-info {
  font-size: 0.875rem;
  padding: 0.95rem;
}
.webinfo-title {
  text-align: center;
  color: #888;
  font-weight: bold;
  padding: 0 0 10px 0;
}
.webinfo-item {
  padding: 8px 0 0;
  margin: 0;
}
.webinfo-item-title {
  display: inline-block;
}
.webinfo-content {
  display: inline-block;
  float: right;
}
@keyframes turn {
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
}
</style>
