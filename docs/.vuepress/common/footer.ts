import { penName, footerTitle } from "./info";

interface Footer {
  createYear: number; // 博客创建年份
  copyrightInfo: string; // 博客版权信息，支持 a 标签
}

export default <Footer>{
  // 页脚信息
  createYear: 2019,
  copyrightInfo:
    " | " +
    penName +
    footerTitle +
    " | " +
    "<span id='busuanzi_container_site_pv'>本站访问量：<span id='busuanzi_value_site_pv'>-</span></span>" +
    " | " +
    ' <a href="http://beian.miit.gov.cn/" target="_blank">京ICP备17044819号</a>',
};
