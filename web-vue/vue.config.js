// vue.config.js
const HOST = "127.0.0.1:2122";
const Timestamp = new Date().getTime();
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
module.exports = {
  // 输出目录
  outputDir: "../modules/server/src/main/resources/dist",
  // 控制静态资源使用相对路径
  publicPath: "./",
  // 代理设置
  devServer: {
    port: 3000,
    proxy: {
      // websocket
      "/ssh": {
        target: `wss://${HOST}`,
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      "/tomcat_log": {
        target: `wss://${HOST}`,
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      "/console": {
        target: `wss://${HOST}`,
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      "/script_run": {
        target: `wss://${HOST}`,
        //  true/false: if you want to proxy websockets
        ws: false,
        secure: false,
      },
      // http
      "/*": {
        target: `http://${HOST}`,
        timeout: 10 * 60 * 1000,
      },
    },
  },
  configureWebpack: {
    // name: name,
    // 修改打包后的js文件名称
    output: {
      // 输出重构  打包编译后的 文件名称  【模块名称.版本号.时间戳】
      filename: `js/[name].[hash].${Timestamp}.js`,
      chunkFilename: `js/[name].[hash].${Timestamp}.js`,
    },
    // 修改打包后的css文件名称
    plugins: [
      new MiniCssExtractPlugin({
        filename: `css/[name].[contenthash].${Timestamp}.css`,
      }),
    ],
    // resolve: {
    //   alias: {
    //     "@": resolve("src")
    //   }
    // }
  },
  chainWebpack: (config) => {
    config.plugin("html").tap((args) => {
      args[0].title = "Jpom项目管理系统";
      args[0].build = new Date().getTime();
      args[0].env = process.env.NODE_ENV;
      return args;
    });
  },
};
