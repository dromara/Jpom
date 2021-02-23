// vue.config.js
module.exports = {
  // 输出目录
  outputDir: '../modules/server/src/main/resources/dist',
  // 代理设置
  devServer: {
    port: 3000,
    proxy: {
      // websocket
      '/ssh': {
        target: 'ws://localhost:2122',
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      '/tomcat_log': {
        target: 'ws://localhost:2122',
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      '/console': {
        target: 'ws://localhost:2122',
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      // http
      '/*': {
        target: 'http://localhost:2122',
        timeout: 10 * 60 * 1000
      }
    },
  },
  chainWebpack: config => {
    config.plugin('html')
    .tap(args => {
      args[0].title= 'Jpom项目管理系统'
      return args
    })
  }
}