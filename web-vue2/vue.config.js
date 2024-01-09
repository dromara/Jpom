/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
// vue.config.js
const HOST = process.env.proxy_host;
console.log(process.env.proxy_host, HOST);
const Timestamp = new Date().getTime();
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
module.exports = {
  // 输出目录
  outputDir: "../modules/server/src/main/resources/dist2",
  // 控制静态资源使用相对路径
  publicPath: "./",
  // 代理设置
  devServer: {
    port: 3001,
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
  //打包后是否让每个js文件都生成一个.map文件？true代表生成，false代表不生成。
  productionSourceMap: false,
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
      args[0].title = "Jpom项目运维系统";
      args[0].build = new Date().getTime();
      args[0].env = process.env.NODE_ENV;
      args[0].buildVersion = process.env.npm_package_version;
      return args;
    });
  },
};
