/**
 * to主题使用者：你可以去掉本文件的所有代码
 */
export default ({
                    Vue, // VuePress 正在使用的 Vue 构造函数
                    options, // 附加到根实例的一些选项
                    router, // 当前应用的路由实例
                    siteData, // 站点元数据
                    isServer // 当前应用配置是处于 服务端渲染 还是 客户端
                }) => {

    // 用于监控在路由变化时检查广告拦截器 (to主题使用者：你可以去掉本文件的所有代码)
    if (!isServer) {
        router.afterEach(() => {
            //check if wwads' fire function was blocked after document is ready with 3s timeout (waiting the ad loading)
            docReady(function () {
              console.log("\n %c Jpom %c https://jpom.top/ \n", "color: #ffffff; background: #f1404b; padding:5px 0;", "background: #030307; padding:5px 0;");
            });


        })
    }
}


//check document ready
function docReady(t) {
    "complete" === document.readyState ||
    "interactive" === document.readyState
        ? setTimeout(t, 1)
        : document.addEventListener("DOMContentLoaded", t);
}
