// vue.config.js
module.exports = {
  // 输出目录
  outputDir: '../modules/server/src/main/resources/dist',
  // 代理设置
  devServer: {
    proxy: 'http://localhost:2122'
  },
  chainWebpack: config => {
    config
        .plugin('html')
        .tap(args => {
          args[0].title= 'Jpom项目管理系统'
          return args
        })
  }
}