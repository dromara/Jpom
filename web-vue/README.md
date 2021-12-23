# JPOM 前端 VUE 项目（server）

## 介绍

这是 Jpom项目的前端部分，基于 [Vue](https://cn.vuejs.org/) 构建，UI框架则是基于 [Ant Design Vue](https://www.antdv.com/docs/vue/introduce-cn/).



## 快速开始

如果您是第一次运行这个项目，您需要保证您的电脑里已经装好了 [Node](http://nodejs.cn/) 环境，Node默认携带`npm`管理器， 无需额外安装。 可通过`npm -v` 查看当前环境是否OK。

然后进入当前目录 `web-vue`

**运行命令**

1. 打包和运行都需要安装依赖

```
npm install
```

2. 本地开发测试？

```
npm run serve
```

3. 项目开发好了，需要打包发布？

```
npm run build
```

> 也可以使用 `script/release.bat` `script/release.sh` 快速打包


## 目录结构

```
.
├── package.json
├── vue.config.js
├── babel.config.js
├── dist
├── public
└── src
    ├── api => 接口管理
    ├── assets => 静态资源
    ├── pages  => 路由级页面
    ├── router => 路由配置
    ├── store => 状态管理
    ├── utils => 共用方法
    ├── App.vue 
    └── main.js => 入口文件
```

### package.json

管理项目所依赖的各种包，运行脚本等配置

### vue.config.js

项目配置文件，可以配置运行端口、代理、项目输出目录......

### babel.config.js

babel配置文件， 关于项目编译期间的一些配置

### dist

项目打包后的输出目录

### public

项目共用文件，此目录中的文件不会被webpack编译

### src

项目主目录



## 开发工具建议

> 使用 Visual Studio Code (VS)

> 需要插件：
> 1. prettier (代码格式化)


