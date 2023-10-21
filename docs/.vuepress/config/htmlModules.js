/** 插入自定义html模块 (可用于插入广告模块等)
 * {
 *   homeSidebarB: htmlString, 首页侧边栏底部
 *
 *   sidebarT: htmlString, 全局左侧边栏顶部
 *   sidebarB: htmlString, 全局左侧边栏底部
 *
 *   pageT: htmlString, 全局页面顶部
 *   pageB: htmlString, 全局页面底部
 *   pageTshowMode: string, 页面顶部-显示方式：未配置默认全局；'article' => 仅文章页①； 'custom' => 仅自定义页①
 *   pageBshowMode: string, 页面底部-显示方式：未配置默认全局；'article' => 仅文章页①； 'custom' => 仅自定义页①
 *
 *   windowLB: htmlString, 全局左下角②
 *   windowRB: htmlString, 全局右下角②
 * }
 *
 * ①注：在.md文件front matter配置`article: false`的页面是自定义页，未配置的默认是文章页（首页除外）。
 * ②注：windowLB 和 windowRB：1.展示区块最大宽高200px*400px。2.请给自定义元素定一个不超过200px*400px的宽高。3.在屏幕宽度小于960px时无论如何都不会显示。
 */


module.exports = {
  homeSidebarB:
    `<div style="padding: 0.95rem">
    <p style="
      color: var(--textColor);
      opacity: 0.9;
      font-size: 20px;
      font-weight: bold;
      margin: 0 0 8px 0;
    ">社区讨论</p>
    <img src="/images/wx_qrcode2.png"  style="width:100%;" />
    添加小助手：备注 <b>Jpom</b> 进群
   </div>
`,
  sidebarT: `
    <a href="https://gitee.com/dromara/MaxKey" target="_blank">
      <img class="no-zoom" height="60" width="224" src="/images/friends/ad/maxkey-banner.png">
    </a>
    <a href="https://www.xiaonuo.vip" target="_blank">
      <img class="no-zoom" height="60" width="224" src="/images/friends/ad/snowy-banner.jpg">
    </a>
     <a href="https://doc.zyplayer.com/#/integrate/zyplayer-doc?utm=jpom" target="_blank">
      <img class="no-zoom" height="60" width="224"  style="background-color: #c6ddff;color: #ffffff" src="/images/friends/ad/zyplayer-doc-banner.png">
    </a>
     <a href="https://gitee.com/qingqinkj/mdp-lcode-ui-web" target="_blank" >
      <img class="no-zoom" height="60" width="224" src="/images/friends/ad/mdp-banner.png">
    </a>
    <div>
    <span style='color: gray;font-size: smaller;'></span>
            <span style='color: #E01E5A;font-size: smaller;font-weight: bolder;float: right'>❤️<a href='/pages/589c8e/'>成为赞助商</a></span>
            </div>
  `,
  // 万维广告
  pageT: `
  <div class="wwads-cn wwads-horizontal page-wwads" data-id="188"></div>
  <style>
      .page-wwads{
        width:100%!important;
        min-height: 0;
        margin: 0;
      }
      .page-wwads .wwads-img img{
        width:80px!important;
      }
      .page-wwads .wwads-poweredby{
        width: 40px;
        position: absolute;
        right: 25px;
        bottom: 3px;
      }
      .wwads-content .wwads-text, .page-wwads .wwads-text{
        height: 100%;
        padding-top: 5px;
        display: block;
      }
  </style>
  `,
  //
  pageB: `

  `,
  windowRB: `

  `
}


// module.exports = {
//   homeSidebarB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   sidebarT: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   sidebarB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   pageT: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   pageB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   windowLB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   windowRB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
// }
