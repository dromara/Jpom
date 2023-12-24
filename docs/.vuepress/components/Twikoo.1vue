<template>
    <div id="tcomment"></div>
</template>
<script>
let waitTime = 700; // 页面加载后多少毫秒后加载评论区（如果是 0ms，可能会报错）
export default {
  data() {
    return {
      twikoo: "",
      firstLoad: true,
    };
  },
  mounted() {
    // 不初始化评论区的页面：frontmatter 的 comment: false 的文章页、首页、归档页、404 页面
    if (
      (this.$frontmatter.comment == undefined || this.$frontmatter.comment) &&
      this.$route.path != "/" &&
      !this.otherPage(this.$route) &&
      !this.isFourZeroFour(this.$route)
    ) {
      setTimeout(() => {
        this.twikooInit();
      }, waitTime);
    }
  },
  watch: {
    $route(to, from) {
      // 404 页面、不同的标题会触发路由，禁止掉
      if (
        this.$route.path == "/" ||
        this.otherPage(this.$route) ||
        this.$route.hash != "" ||
        this.isFourZeroFour(to)
      ) {
        return;
      }
      // 进入首页、进入 frontmatter 的 comment: false 页面，删除评论区
      if (to.path == "/" || this.getCommentByFrontmatter(to) == false) {
        this.deleteComment();
        return;
      }
      // 初始化评论条件：来自首页，来自归档页、来自 frontmatter 的 comment: true 的文章页
      if (
        from.path == "/" ||
        this.otherPage(from) ||
        !this.getCommentByFrontmatter(from)
      ) {
        this.firstLoad
          ? setTimeout(() => {
              this.twikooInit();
              this.firstLoad = false;
            }, waitTime)
          : this.twikooInit(); // 如果加载过一次评论区，则直接获取
      } else if (this.$route.path != "/" && this.$route.hash == "") {
        // 文章页之间跳转，重新获取评论
        setTimeout(() => {
          this.updateComment();
        }, waitTime);
      }
    },
  },
  methods: {
    twikooInit() {
      twikoo
        .init({
          // envId 要切换成自己的，这是评论区的 ID，一个博客只能有一个评论区 ID，用别人的评论区 ID，导致读者评论时或发送到别人的评论区里
          envId: "https://twikoo.youngkbt.cn/",
          el: "#tcomment",
          // region: 'ap-guangzhou', // 环境地域，默认为 ap-shanghai，如果您的环境地域不是上海，需传此参数
          // path: 'window.location.pathname', // 用于区分不同文章的自定义 js 路径，如果您的文章路径不是 location.pathname，需传此参数
          // lang: 'zh-CN', // 用于手动设定评论区语言，支持的语言列表 https://github.com/imaegoo/twikoo/blob/dev/src/js/utils/i18n/index.js
          // onCommentLoaded: function () {
          // console.log("评论加载或评论成功");
          // },
        })
        .then(() => {
          this.loadTwikoo();
        });
    },
    // 初始化加载或者跳转新页面重新加载 Twikoo 评论区
    loadTwikoo() {
      let page = document.getElementsByClassName("page")[0];
      let comment = document.getElementById("twikoo");
      // comment 不存在代表曾初始化过，后面被删除
      comment ? (this.twikoo = comment) : (comment = this.twikoo);
      page
        ? comment
          ? page.appendChild(comment)
          : page.appendChild(this.twikoo)
        : "";
      this.updateComment();
    },
    // 跳转新页面，重新获取当前页面的评论信息
    updateComment() {
      let tk_icon = document.getElementsByClassName("tk-icon")[0];
      tk_icon ? tk_icon.click() : undefined;
    },
    // 删除 frontmatter:comment: false 页面的数据
    deleteComment() {
      let comment = document.getElementById("twikoo");
      comment ? comment.parentNode.removeChild(comment) : "";
    },
    // 获取 frontmatter 的 comment
    getCommentByFrontmatter(route) {
      let comment = true;
      this.$site.pages.forEach((item) => {
        if (item.path == route.path) {
          comment = item.frontmatter.comment;
        }
      });
      return comment;
    },
    // 判定当前页面是不是 404
    isFourZeroFour(route) {
      let flag = true;
      this.$site.pages.forEach((item) => {
        if (item.path == route.path) {
          flag = false;
        }
      });
      return flag;
    },
    // 其他页面
    otherPage(route) {
      if(
        route.path == "/archives/" ||
        route.path == "/categories/" ||
        route.path == "/tags/"
      ) {
        return true;
      }else {
        return false;
      }
    }
  },
};
</script>

<style>
.twikoo .tk-comments {
    margin-top: 20px;
}
</style>
