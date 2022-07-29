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
    </p>

    <div class="wwads-cn wwads-vertical" style="max-width:200px;"> <link rel="stylesheet" href="https://cdn.wwads.cn/css/wwads.css">
    <a href="https://wwads.cn?aff_id=217" class="wwads-img" target="_blank" rel="nofollow">
    <img src="https://cdn.wwads.cn/images/placeholder/wwads-friendly-ads.png" width="130" ></a>
    <div class="wwads-content"><a href="https://wwads.cn?aff_id=217" class="wwads-text" target="_blank" rel="nofollow" >B2B Advertising Made Easy —— 我们帮助 to B 企业轻松投放更精准 & 友好的广告</a>
    <a href="https://wwads.cn?aff_id=217" class="wwads-poweredby" title="万维广告——让广告交易像网购一样简单" target="_blank" rel="nofollow"><span><img class="wwads-logo"><span class="wwads-logo-text">广告</span></a> </div></div>

   </div>
`,
  //
  pageB: `

  `,
  // windowRB: `

  // `
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
