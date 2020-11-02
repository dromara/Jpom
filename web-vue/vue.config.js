// vue.config.js
module.exports = {
  // 输出目录
  outputDir: '../modules/server/src/main/resources/dist',
  // 代理设置
  devServer: {
    proxy: 'http://localhost:2122'
  }
}