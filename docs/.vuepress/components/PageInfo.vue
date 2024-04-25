<template></template>

<script>
import fetch from '../webSiteInfo/busuanzi'

const busuanziKey = [
  {
    className: 'page-view',
    id: 'busuanzi_value_page_pv',
    title: '本文浏览量'
  }
  // {
  //   className: "page-site-view",
  //   id: "busuanzi_value_site_pv",
  //   title: "本站访问量",
  // },
]
export default {
  mounted() {
    // 首页不初始页面信息
    if (this.$route.path != '/') {
      this.initPageInfo()
    } else {
      this.getPageViewCouter()
    }
    setTimeout(() => {
      this.addExpand(40)
    }, 1000);
    //

    (function() {
      var hm = document.createElement('script')
      hm.src = 'https://hm.baidu.com/hm.js?1acbde0eb1cc4225dcfe6d1143767a3d'
      var s = document.getElementsByTagName('script')[0]
      s.parentNode.insertBefore(hm, s)
    })();

    (function() {
      var bp = document.createElement('script')
      bp.type = 'text/javascript'
      bp.async = true
      bp.crossorigin = 'anonymous'
      bp.src = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-8458956478992681'
      var s = document.getElementsByTagName('script')[0]
      s.parentNode.insertBefore(bp, s)
    })();

    (function() {
      var bp = document.createElement('script')
      bp.type = 'text/javascript'
      bp.async = true
      bp.src = 'https://www.googletagmanager.com/gtag/js?id=G-FYSG66S4HQ'
      var s = document.getElementsByTagName('script')[0]
      s.parentNode.insertBefore(bp, s)

      window.dataLayer = window.dataLayer || []

      function gtag() {
        dataLayer.push(arguments)
      }

      gtag('js', new Date())
      gtag('config', 'G-FYSG66S4HQ')
    })()
  },
  watch: {
    $route(to, from) {
      // 如果页面是非首页，# 号也会触发路由变化，这里要排除掉
      if (
        to.path !== '/' &&
        to.path !== from.path &&
        this.$themeConfig.blogInfo
      ) {
        this.initPageInfo()
      }

      if (to.path != from.path || this.$route.hash == '') {
        setTimeout(() => {
          this.addExpand(40)
        }, 1000)
      }
    }
  },
  methods: {
    /**
     * 初始化页面信息
     */
    initPageInfo() {
      if (this.$frontmatter.article == undefined || this.$frontmatter.article) {
        // 排除掉 article 为 false 的文章
        const { eachFileWords, pageView, pageIteration, readingTime } =
          this.$themeConfig.blogInfo
        // 下面两个 if 可以调换位置，从而让文章的浏览量和字数交换位置
        if (eachFileWords) {
          try {
            eachFileWords.forEach((itemFile) => {
              if (itemFile.permalink == this.$frontmatter.permalink) {
                // this.addPageWordsCount 和 if 可以调换位置，从而让文章的字数和预阅读时间交换位置
                this.addPageWordsCount(itemFile.wordsCount)
                if (readingTime || readingTime == undefined) {
                  this.addReadTimeCount(itemFile.readingTime)
                }
                throw new Error()
              }
            })
          } catch (error) {
          }
        }
        if (pageView || pageView == undefined) {
          this.addPageView()
          this.getPageViewCouter(pageIteration)
        }
        return
      }
    },
    /**
     * 文章页的访问量
     */
    getPageViewCouter(iterationTime = 3000) {
      fetch()
      let i = 0
      var defaultCouter = '9999'
      // 如果只需要第一次获取数据（可能获取失败），可注释掉 setTimeout 内容，此内容是第一次获取失败后，重新获取访问量
      // 可能会导致访问量再次 + 1 原因：取决于 setTimeout 的时间（需求调节），setTimeout 太快导致第一个获取的数据没返回，就第二次获取，导致结果返回 + 2 的数据
      setTimeout(() => {
        let pageViewAll = document.querySelectorAll('.view-data')
        if (pageViewAll) {
          let pageView = pageViewAll[0]
          // console.log(pageView);
          if (pageView && pageView.innerText == '') {
            let interval = setInterval(() => {
              // 再次判断原因：防止进入 setInterval 的瞬间，访问量获取成功
              if (pageView && pageView.innerText == '') {
                i += iterationTime
                if (i > iterationTime * 5) {
                  pageViewAll.forEach((item) => {
                    item.innerText = defaultCouter
                  })

                  clearInterval(interval) // 5 次后无法获取，则取消获取
                }
                if (pageView.innerText == '') {
                  // 手动获取访问量
                  fetch()
                } else {
                  clearInterval(interval)
                }
              } else {
                clearInterval(interval)
              }
            }, iterationTime)
            // 绑定 beforeDestroy 生命钩子，清除定时器
            this.$once('hook:beforeDestroy', () => {
              clearInterval(interval)
              interval = null
            })
          }
        }
      }, iterationTime)
    },
    /**
     * 添加浏览量元素
     */
    addPageView() {
      busuanziKey.forEach((item) => {
        let pageView = document.querySelector('.' + item.className)
        if (pageView) {
          pageView.innerHTML =
            '<a style="color: #888; margin-left: 3px" href="javascript:;" id="' +
            item.id +
            '" class="view-data"><i title="正在获取..." class="loading iconfont icon-loading"></i></a>'
        } else {
          // 创建访问量的元素
          let template = document.createElement('div')
          template.title = item.title
          template.className = item.className + ' iconfont icon-view'
          template.style.float = 'left'
          template.style.marginLeft = '20px'
          template.style.fontSize = '0.8rem'
          template.innerHTML =
            '<a style="color: #888; margin-left: 3px" href="javascript:;" id="' +
            item.id +
            '" class="view-data"><i title="正在获取..." class="loading iconfont icon-loading"></i></a>'
          // 添加 loading 效果
          let style = document.createElement('style')
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
      }`
          document.head.appendChild(style)
          this.mountedView(template)
        }
      })
    },
    /**
     * 添加当前文章页的字数元素
     */
    addPageWordsCount(wordsCount = 0) {
      let words = document.querySelector('.book-words')
      if (words) {
        words.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${wordsCount}</a>`
      } else {
        let template = document.createElement('div')
        template.title = '文章字数'
        template.className = 'book-words iconfont icon-book'
        template.style.float = 'left'
        template.style.marginLeft = '20px'
        template.style.fontSize = '0.8rem'

        template.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${wordsCount}</a>`
        this.mountedView(template)
      }
    },
    /**
     * 添加预计的阅读时间
     */
    addReadTimeCount(readTimeCount = 0) {
      let reading = document.querySelector('.reading-time')
      if (reading) {
        reading.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${readTimeCount}</a>`
      } else {
        let template = document.createElement('div')
        template.title = '预阅读时长'
        template.className = 'reading-time iconfont icon-shijian'
        template.style.float = 'left'
        template.style.marginLeft = '20px'
        template.style.fontSize = '0.8rem'
        template.innerHTML = `<a href="javascript:;" style="margin-left: 3px; color: #888">${readTimeCount}</a>`
        this.mountedView(template)
      }
    },
    /**
     * 挂载目标到页面上
     */
    mountedView(
      template,
      mountedIntervalTime = 100,
      moutedParentEvent = '.articleInfo-wrap > .articleInfo > .info'
    ) {
      let i = 0
      let parentElement = document.querySelector(moutedParentEvent)
      if (parentElement) {
        if (!this.isMountedView(template, parentElement)) {
          parentElement.appendChild(template)
        }
      } else {
        let interval = setInterval(() => {
          parentElement = document.querySelector(moutedParentEvent)
          if (parentElement) {
            if (!this.isMountedView(template, parentElement)) {
              parentElement.appendChild(template)
              clearInterval(interval)
            }
          } else if (i > 1 * 10) {
            // 10 秒后清除
            clearInterval(interval)
          }
        }, mountedIntervalTime)
        // 绑定 beforeDestroy 生命钩子，清除定时器
        this.$once('hook:beforeDestroy', () => {
          clearInterval(interval)
          interval = null
        })
      }
    },
    /**
     * 如果元素存在，则删除
     */
    removeElement(selector) {
      var element = document.querySelector(selector)
      element && element.parentNode.removeChild(element)
    },
    /**
     * 目标是否已经挂载在页面上
     */
    isMountedView(element, parentElement) {
      if (element.parentNode == parentElement) {
        return true
      } else {
        return false
      }
    },
    // 隐藏代码块后，保留 40 的代码块高度
    addExpand(hiddenHeight = 40) {
      let modes = document.getElementsByClassName('line-numbers-mode')
      // 遍历出每一个代码块
      Array.from(modes).forEach((item) => {
        // 首先获取 expand 元素
        let expand = item.getElementsByClassName('expand')[0]
        // expand 元素不存在，则进入 if 创建
        if (!expand) {
          // 获取代码块原来的高度，进行备份
          let modeHeight = item.offsetHeight
          // display:none 的代码块需要额外处理，图文卡片列表本质是代码块，所以排除掉
          if (
            modeHeight == 0 &&
            item.parentNode.className != 'cardImgListContainer'
          ) {
            modeHeight = this.getHiddenElementHight(item)
          }
          // modeHeight 比主题多 12，所以减掉，并显示赋值，触发动画过渡效果
          modeHeight -= 12
          item.style.height = modeHeight + 'px'
          // 获取代码块的各个元素
          let pre = item.getElementsByTagName('pre')[0]
          let wrapper = item.getElementsByClassName('line-numbers-wrapper')[0]
          // 创建箭头元素
          const div = document.createElement('div')
          div.className = 'expand icon-xiangxiajiantou iconfont'
          // 箭头点击事件
          div.onclick = () => {
            // 代码块已经被隐藏，则进入 if 循环，如果没有被隐藏，则进入 else 循环
            if (parseInt(item.style.height) == hiddenHeight) {
              div.className = 'expand icon-xiangxiajiantou iconfont'
              item.style.height = modeHeight + 'px'
              setTimeout(() => {
                pre.style.display = 'block'
                wrapper.style.display = 'block'
              }, 80)
            } else {
              div.className = 'expand icon-xiangxiajiantou iconfont closed'
              item.style.height = hiddenHeight + 'px'
              setTimeout(() => {
                pre.style.display = 'none'
                wrapper.style.display = 'none'
              }, 300)
            }
          }
          item.append(div)
          item.append(this.addCircle())
        }
        // 解决某些代码块的语言不显示在页面上
        this.getLanguage(item)
        // 移动一键复制图标到正确的位置
        let flag = false
        let interval = setInterval(() => {
          flag = this.moveCopyBlock(item)
          if (flag) {
            clearInterval(interval)
          }
        }, 1000)
      })
    },
    getHiddenElementHight(hiddenElement) {
      let modeHeight
      if (
        hiddenElement.parentNode.style.display == 'none' ||
        hiddenElement.parentNode.className !=
        'theme-code-block theme-code-block__active'
      ) {
        hiddenElement.parentNode.style.display = 'block'
        modeHeight = hiddenElement.offsetHeight
        hiddenElement.parentNode.style.display = 'none'
        // 清除 vuepress 自带的 deetails 多选代码块
        if (
          hiddenElement.parentNode.className == 'theme-code-block' ||
          hiddenElement.parentNode.className == 'cardListContainer'
        ) {
          hiddenElement.parentNode.style.display = ''
        }
      }
      return modeHeight
    },
    // 添加三个圆圈
    addCircle() {
      let div = document.createElement('div')
      div.className = 'circle'
      return div
    },
    // 移动一键复制图标
    moveCopyBlock(element) {
      let copyElement = element.getElementsByClassName('code-copy')[0]
      if (copyElement && copyElement.parentNode != element) {
        copyElement.parentNode.parentNode.insertBefore(
          copyElement,
          copyElement.parentNode
        )
        return true
      } else {
        return false
      }
    },
    // 解决某些代码块的语言不显示在页面上
    getLanguage(element) {
      // 动态获取 before 的 content 属性
      let content = getComputedStyle(element, ':before').getPropertyValue(
        'content'
      )
      // "" 的长度是 2，不是 0，"x" 的长度是 3
      if (content.length == 2 || content == '' || content == 'none') {
        let language = element.className.substring(
          'language'.length + 1,
          element.className.indexOf(' ')
        )
        element.setAttribute('data-language', language)
      }
    }
  },
  // 防止重写编译时，导致页面信息重复出现问题
  beforeMount() {
    busuanziKey.forEach((item) => {
      this.removeElement('.' + item.className)
    })

    this.removeElement('.book-words')
    this.removeElement('.reading-time')
  }
}
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
