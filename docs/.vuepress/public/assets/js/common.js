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

$(function() {
  window.JPOM_RELEASE_VERSION = '2.11.4'
  // 检查域名
  const localHosts = [
    'localhost',
    '127.0.0.1',
    'jpom.top',
    'jops.top',
    'webcache.googleusercontent.com',
    '192.168.'
  ]

  function checkDomain() {
    if (localHosts.includes(location.hostname)) {
      return
    }
    for (let item in localHosts) {
      if (location.hostname.indexOf(localHosts[item]) > -1) {
        return
      }
    }
    console.log(location.host + '  =>  jpom.top')
    layer.msg('当前访问的地址不是主站，2秒后自动切换到主站', {
      offset: 't',
      anim: 2
    })
    setTimeout(function() {
      location.href = `https://jpom.top${location.pathname}${location.search}${location.hash}`
    }, 2000)
  }

  checkDomain()

  // 滚动左边菜单到可视区域
  loopExecute(function() {
    const $dom = $('.sidebar-links .active')
    if (!$dom.length) {
      return false
    }
    $dom.get(0).scrollIntoView({ block: 'center' })
    return true
  }, 20)

  // // 提醒 star
  // docReady(() => {
  //   setTimeout(() => {
  //     openTipStar()
  //   }, 2000)
  // })

  function openTipStar() {
    window['tipStarIndex'] && layer.close(window['tipStarIndex'] || -1)
    window['tipStarIndex'] = layer.msg(
      '欢迎您 Star Jpom <a href="https://gitee.com/dromara/Jpom/stargazers" target="_blank">Gitee</a>/' +
      '<a href="https://github.com/dromara/Jpom" target="_blank">Github</a>',
      {
        offset: 'rb',
        time: 0,
        anim: 6
      }
    )
  }

  // window.addEventListener('resize', openTipStar)
})

function docReady(t) {
  'complete' === document.readyState || 'interactive' === document.readyState
    ? setTimeout(t, 1)
    : document.addEventListener('DOMContentLoaded', t)
}

function loopExecute(fn, loopCount, fail) {
  if (fn && fn()) {
    // 执行成功
    return
  }
  if (loopCount <= 0) {
    fail && fail()
    // 结束执行
    return
  }
  setTimeout(() => {
    loopExecute(fn, loopCount - 1, fail)
  }, 500)
}

function checkIsLocal() {
  return location.hostname === '127.0.0.1' ||
    location.hostname === 'localhost' ||
    location.hostname.indexOf('192.168.') > -1

}


// https://gitee.com/dromara/sa-token/blob/dev/sa-token-doc/static/is-star-plugin.js

// 检查成功后，多少天不再检查
const allowDisparity = 1000 * 60 * 60 * 24 * 30 * 3
const client_id = 'ea3bda02d8f2a6ab9dd7f337b7df1318714c5e3d867c3b59d293c1acbb360b30'
const client_secret = 'xxx'
const redirect_uri = 'https://jpom.top'


// 判断当前是否已 star
function isStarRepo(url) {
  // console.log(url)
  var code = getParam('code')
  // 非PC端不检查
  if (document.body.offsetWidth < 800) {
    console.log('small screen ...')
    return
  }

  // 判断是否在主域名下
  if (checkIsLocal()) {
    console.log('not domain, no check...')
    // return
  }

  // 判断是否近期已经判断过了
  try {
    const isStarRepo = localStorage.isStarRepo
    if (isStarRepo) {
      // 记录 star 的时间，和当前时间的差距
      const disparity = new Date().getTime() - parseInt(isStarRepo)

      // 差距小于一月，不再检测，大于一月，再检测一下
      if (disparity < allowDisparity) {
        console.log('checked ...')
        return
      }
    }
  } catch (e) {
    console.error(e)
  }
  if (code) {
    // 携带了 code
    getAccessToken(code)
    return
  }

  // 需要验证的路由关键词
  const verifyList = ['/fqa/', '/practice/', '/db/', '/downloads/', 'downloads']
  let needCheck = false
  for (let i = 0; i < verifyList.length; i++) {
    if (url.toLowerCase().indexOf(verifyList[i])) {
      needCheck = true
      break
    }
  }
  if (!needCheck && code === null) {
    console.log('white route ...')
    return
  }

  // 开始获取 code
  $('body').css({ 'overflow': 'hidden' })
  getCode(url)
}

// 去请求授权
function getCode(url) {

  // 检查url中是否有code
  const code = getParam('code')
  if (code) {
    // 有 code，进一步去请求 access_token
    getAccessToken(code)
  } else {
    // 不存在code，弹窗提示询问
    confirmStar(url)
  }
}

// 弹窗提示点 star
function confirmStar(url) {

  // 弹窗提示文字
  const tipStr = `
		<div>
			<p><b>嗨，同学，来支持一下 Jpom 吧，为项目点个 star ！</b></p>
			<div>仅需两步即可完成：<br>
				<div>1、打开 Jpom <a href="https://gitee.com/dromara/Jpom" target="_blank">开源仓库主页</a>，在右上角点个 star 。</div>
				<div>2、点击下方 [ 同意授权检测 ] 按钮，同意 Jpom 获取 API 权限进行检测。<a href="javascript:authDetails();" style="text-decoration: none;">？</a></div>
			</div>
			<p><b>本章节文档将在 star 后正常开放展示。</b></p>
			<p style="color: green;">开源不易，希望您不吝支持，激励开源项目走的更加长远 😇😇😇</p>
		</div>
		`

  const index = layer.confirm(tipStr, {
      title: '提示',
      btn: ['同意授权检测'],
      // btn: ['同意授权检测', '暂时不要，我先看看文档'],
      area: '460px',
      offset: '25%',
      closeBtn: false
    },
    function(index) {
      //
      //layer.close(index)
      // 用户点了确认，去 gitee 官方请求授权获取
      goAuth(url)
    }
  )

  // 源码注释提示
  const closeLayer = ``
  $('#layui-layer' + index).prepend(closeLayer)
}


// 跳转到 gitee 授权界面
function goAuth(url) {
  localStorage.toStarBeforePath = url
  location.href = 'https://gitee.com/oauth/authorize' +
    '?client_id=' + client_id +
    '&redirect_uri=' + redirect_uri +
    '&response_type=code'
}


// 获取 access_token
function getAccessToken(code) {
  // 根据 code 获取 access_token
  $.ajax({
    url: 'https://jpom.top/tools/api/gitee/oauth/token',
    method: 'post',
    data: {
      grant_type: 'authorization_code',
      code: code,
      client_id: client_id,
      redirect_uri: redirect_uri,
      client_secret: client_secret
    },
    success: function(res) {
      // 如果返回的不是 200
      if (res.code !== 200) {
        return layer.alert(res.msg, { closeBtn: false }, function() {
          // 刷新url，去掉 code 参数
          toStarBeforePath()
        })
      }

      // 拿到 access_token
      const access_token = res.data.access_token

      // 根据 access_token 判断是否 star 了仓库
      $.ajax({
        url: 'https://gitee.com/api/v5/user/starred/dromara/Jpom',
        method: 'get',
        data: {
          access_token: access_token
        },
        success: function(res) {
          // success 回调即代表已经 star，gitee API 请求体不返回任何数据
          console.log('-> stared ...')
          // 记录本次检查时间
          localStorage.isStarRepo = new Date().getTime()
          //
          layer.alert('感谢您的支持  ❤️ ❤️ ❤️ ，Jpom 将努力变得更加完善！', function(index) {
            layer.close(index)
            // 刷新url，去掉 code 参数
            toStarBeforePath()
          })
        },
        error: function(e) {
          // console.log('ff请求错误 ', e);
          // 如下返回，代表没有 star
          if (e.statusText === 'Not Found') {
            console.log('not star ...')
            layer.alert('未检测到 star 数据...,可能是网络不稳定请稍后再试 ...', { closeBtn: false }, function() {
              // 刷新url，去掉 code 参数
              toStarBeforePath()
            })
          }
        }
      })

    },
    error: function(e) {
      console.log('请求错误 ', e)
      // 如果请求地址有错，可能是服务器宕机了，暂停一天检测
      if (e.status === 0 || e.status === 502) {
        return layer.alert(JSON.stringify(e), { closeBtn: false }, function() {
          // 一天内不再检查
          const ygTime = allowDisparity - (1000 * 60 * 60 * 24)
          localStorage.isStarRepo = new Date().getTime() - ygTime
          // 刷新 url，去掉 code 参数
          toStarBeforePath()
        })
      }

      // 无效授权，可能是 code 无效
      const errorMsg = (e.responseJSON && e.responseJSON.error) || JSON.stringify(e)
      if (errorMsg === 'invalid_grant') {
        console.log('无效code', code)
      }
      layer.alert('check error... ' + errorMsg, function(index) {
        layer.close(index)
        // 刷新url，去掉 code 参数
        toStarBeforePath()
      })
    }
  })
}

function toStarBeforePath() {
  const toStarBeforePath = localStorage.toStarBeforePath
  if (toStarBeforePath) {
    localStorage.toStarBeforePath = ''
    location.href = toStarBeforePath
  } else {
    location.href = '/'
  }
}

// 疑问
function authDetails() {
  const str = '用于检测的凭证信息将仅保存你的浏览器本地，Jpom 文档已完整开源，源码可查'
  alert(str)
}

// 获取 url 携带的参数
function getParam(name, defaultValue) {
  var query = window.location.search.substring(1)
  var vars = query.split('&')
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=')
    if (pair[0] === name) {
      return pair[1]
    }
  }
  return (defaultValue === undefined ? null : defaultValue)
}
