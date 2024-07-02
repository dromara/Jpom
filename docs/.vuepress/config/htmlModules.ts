// 目前该文件没有用到

import { VdoingThemeConfig } from "vuepress-theme-vdoing/types";
/** 插入自定义html模块 (可用于插入广告模块等)
 * {
 *   homeSidebarB: htmlString, 首页侧边栏底部
 *
 *   sidebarT: htmlString, 所有左侧边栏顶部
 *   sidebarB: htmlString, 所有左侧边栏底部
 *
 *   pageT: htmlString, 页面顶部
 *   pageB: htmlString, 页面底部
 *   pageTshowMode: string, 页面顶部-显示方式：未配置默认所有页面；'article' => 仅文章页①； 'custom' => 仅自定义页①
 *   pageBshowMode: string, 页面底部-显示方式：未配置默认所有页面；'article' => 仅文章页①； 'custom' => 仅自定义页①
 *
 *   windowLB: htmlString, 全局窗口左下角②
 *   windowRB: htmlString, 全局窗口右下角②
 * }
 *
 * ①注：在.md文件front matter配置`article: false`的页面是自定义页，未配置的默认是文章页（首页除外）。
 * ②注：windowLB 和 windowRB：1.展示区块宽高最大是200*200px。2.请给自定义元素定一个不超过200px的固定宽高。3.在屏宽小于960px时无论如何都不会显示。
 */

const htmlModule: VdoingThemeConfig["htmlModules"] = {
  homeSidebarB: `<div style="padding: 0.95rem">
         <p style="
           color: var(--textColor);
           opacity: 0.9;
           font-size: 20px;
           font-weight: bold;
           margin: 0 0 8px 0;
         ">社区讨论</p>
         <img  loading="lazy" src="/images/wx_qrcode2.png"  style="width:100%;" />
         添加小助手：备注 <b>Jpom</b> 进群
        </div>`,
  sidebarT: `
    <a href="https://gitee.com/dromara/MaxKey" target="_blank">
      <img  loading="lazy" class="no-zoom" height="60" width="224" src="/images/friends/ad/maxkey-banner.png">
    </a>
    <a href="https://www.xiaonuo.vip" target="_blank">
      <img  loading="lazy" class="no-zoom" height="60" width="224" src="/images/friends/ad/snowy-banner.jpg">
    </a>
     <a href="https://doc.zyplayer.com/#/integrate/zyplayer-doc?utm=jpom" target="_blank">
      <img  loading="lazy" class="no-zoom" height="60" width="224"  style="background-color: #c6ddff;color: #ffffff" src="/images/friends/ad/zyplayer-doc-banner.png">
    </a>
     <a href="https://gitee.com/qingqinkj/mdp-lcode-ui-web" target="_blank" >
      <img  loading="lazy" class="no-zoom" height="60" width="224" src="/images/friends/ad/mdp-banner2.png">
    </a>
    <a href="https://www.jnpfsoft.com/index.html?from=jpom" target="_blank"  style="background:#dcf3ff">
          <img  loading="lazy" class="no-zoom" height="60" width="224" src="/images/friends/ad/jnpfsoft-banner.png">
        </a>
    <div>
    <span style='color: gray;font-size: smaller;'></span>
            <span style='color: #E01E5A;font-size: smaller;font-weight: bolder;float: right'>❤️<a href='/pages/589c8e/'>成为赞助商</a></span>
            </div>
  `,
  // 万维广告
  pageT: `
  <div class="wwads-cn wwads-horizontal page-wwads" data-id="188" style="display: flex !important"></div>
  `,
  //
  pageB: `

  `,
  windowRB: `
  <div class="custom-block danger"><p class="custom-block-title">系统公告</p> <p>近期我们下载中心受到持续的恶意刷流量，给我们的长期运营带来了很大的挑战。我们已经将下载中心迁移至 cloudflare，部分网络环境下使用可能不便还请谅解。</p>
  <p><a href="/pages/malice-fraudulent/" target="_blank">点击查看详情</a></p>
  </div>
  `,
};

// const htmlModule = {
//   homeSidebarB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   sidebarT: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   sidebarB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   pageT: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   pageB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   windowLB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
//   windowRB: `<div style="width:100%;height:100px;color:#fff;background: #eee;">自定义模块测试</div>`,
// }

export default htmlModule;
