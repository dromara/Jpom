<template></template>

<script>
export default {
  mounted() {
    setTimeout(() => {
      this.addExpand(40);
    }, 1000);
  },
  watch: {
    $route(to, from) {
      if (to.path != from.path || this.$route.hash == "") {
        setTimeout(() => {
          this.addExpand(40);
        }, 1000);
      }
    },
  },
  methods: {
    // 隐藏代码块后，保留 40 的代码块高度
    addExpand(hiddenHeight = 40) {
      let modes = document.getElementsByClassName("line-numbers-mode");
      // 遍历出每一个代码块
      Array.from(modes).forEach((item) => {
        // 首先获取 expand 元素
        let expand = item.getElementsByClassName("expand")[0];
        // expand 元素不存在，则进入 if 创建
        if (!expand) {
          // 获取代码块原来的高度，进行备份
          let modeHeight = item.offsetHeight;
          // display:none 的代码块需要额外处理，图文卡片列表本质是代码块，所以排除掉
          if (
            modeHeight == 0 &&
            item.parentNode.className != "cardImgListContainer"
          ) {
            modeHeight = this.getHiddenElementHight(item);
          }
          // modeHeight 比主题多 12，所以减掉，并显示赋值，触发动画过渡效果
          modeHeight -= 12;
          item.style.height = modeHeight + "px";
          // 获取代码块的各个元素
          let pre = item.getElementsByTagName("pre")[0];
          let wrapper = item.getElementsByClassName("line-numbers-wrapper")[0];
          // 创建箭头元素
          const div = document.createElement("div");
          div.className = "expand icon-xiangxiajiantou iconfont";
          // 箭头点击事件
          div.onclick = () => {
            // 代码块已经被隐藏，则进入 if 循环，如果没有被隐藏，则进入 else 循环
            if (parseInt(item.style.height) == hiddenHeight) {
              div.className = "expand icon-xiangxiajiantou iconfont";
              item.style.height = modeHeight + "px";
              setTimeout(() => {
                pre.style.display = "block";
                wrapper.style.display = "block";
              }, 80);
            } else {
              div.className = "expand icon-xiangxiajiantou iconfont closed";
              item.style.height = hiddenHeight + "px";
              setTimeout(() => {
                pre.style.display = "none";
                wrapper.style.display = "none";
              }, 300);
            }
          };
          item.append(div);
          item.append(this.addCircle());
        }
        // 解决某些代码块的语言不显示在页面上
        this.getLanguage(item);
        // 移动一键复制图标到正确的位置
        let flag = false;
        let interval = setInterval(() => {
          flag = this.moveCopyBlock(item);
          if (flag) {
            clearInterval(interval);
          }
        }, 1000);
      });
    },
    getHiddenElementHight(hiddenElement) {
      let modeHeight;
      if (
        hiddenElement.parentNode.style.display == "none" ||
        hiddenElement.parentNode.className !=
          "theme-code-block theme-code-block__active"
      ) {
        hiddenElement.parentNode.style.display = "block";
        modeHeight = hiddenElement.offsetHeight;
        hiddenElement.parentNode.style.display = "none";
        // 清除 vuepress 自带的 deetails 多选代码块
        if (
          hiddenElement.parentNode.className == "theme-code-block" ||
          hiddenElement.parentNode.className == "cardListContainer"
        ) {
          hiddenElement.parentNode.style.display = "";
        }
      }
      return modeHeight;
    },
    // 添加三个圆圈
    addCircle() {
      let div = document.createElement("div");
      div.className = "circle";
      return div;
    },
    // 移动一键复制图标
    moveCopyBlock(element) {
      let copyElement = element.getElementsByClassName("code-copy")[0];
      if (copyElement && copyElement.parentNode != element) {
        copyElement.parentNode.parentNode.insertBefore(
          copyElement,
          copyElement.parentNode
        );
        return true;
      } else {
        return false;
      }
    },
    // 解决某些代码块的语言不显示在页面上
    getLanguage(element) {
      // 动态获取 before 的 content 属性
      let content = getComputedStyle(element, ":before").getPropertyValue(
        "content"
      );
      // "" 的长度是 2，不是 0，"x" 的长度是 3
      if (content.length == 2 || content == "" || content == "none") {
        let language = element.className.substring(
          "language".length + 1,
          element.className.indexOf(" ")
        );
        element.setAttribute("data-language", language);
      }
    },
  },
};
</script>

<style>
/* 代码块元素 */
.line-numbers-mode {
  overflow: hidden;
  transition: height 0.3s;
  margin-top: 0.85rem;
}
.line-numbers-mode::before {
  content: attr(data-language);
}
/* 箭头元素 */
.expand {
  width: 16px;
  height: 16px;
  cursor: pointer;
  position: absolute;
  z-index: 3;
  top: 0.8em;
  right: 0.5em;
  color: rgba(238, 255, 255, 0.8);
  font-weight: 900;
  transition: transform 0.3s;
}

/* 代码块内容 */
div[class*="language-"].line-numbers-mode pre {
  margin: 30px 0 0.85rem 0;
}
/* 代码块的行数 */
div[class*="language-"].line-numbers-mode .line-numbers-wrapper,
.highlight-lines {
  margin-top: 30px;
}
/* 箭头关闭后旋转 -90 度 */
.closed {
  transform: rotate(90deg) translateY(-3px);
  transition: all 0.3s;
}
li .closed {
  transform: rotate(90deg) translate(5px, -8px);
}
/* 代码块的语言 */
div[class*="language-"]::before {
  position: absolute;
  z-index: 3;
  top: 0.3em;
  left: 4.7rem;
  font-size: 1.15em;
  color: rgba(238, 255, 255, 0.8);
  text-transform: uppercase;
  font-weight: bold;
  width: fit-content;
}
/* li 下的代码块的语言和 li 下的箭头 */
li div[class*="language-"]::before,
li .expand {
  margin-top: -4px;
}
/* 代码块行数的线条 */
div[class*="language-"].line-numbers-mode::after {
  margin-top: 35px;
}
/* 代码块的三个圆圈颜色 */
.circle {
  position: absolute;
  top: 0.8em;
  left: 0.9rem;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fc625d;
  -webkit-box-shadow: 20px 0 #fdbc40, 40px 0 #35cd4b;
  box-shadow: 20px 0 #fdbc40, 40px 0 #35cd4b;
}
/* 代码块一键复制图标 */
.code-copy {
  position: absolute;
  top: 0.8rem;
  right: 2rem;
  fill: rgba(238, 255, 255, 0.8);
  opacity: 1;
}
.code-copy svg {
  margin: 0;
}

/* 如果你浅色模式的代码块背景色是浅灰色，则取消下面的注释使代码生效，如果是黑色，则注释下面的三段代码（我注释了，因为是黑色背景） */
/* .theme-mode-light .expand {
  color: #666;
}
.theme-mode-light div[class*="language-"]::before {
  color: #666;
}
.theme-mode-light .code-copy {
  fill: #666;
} */
</style>
