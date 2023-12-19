## 项目介绍（未开发完成请勿使用）

本项目采用 [Vue3](https://cn.vuejs.org/guide/introduction.html#what-is-vue) + [Vite](https://vitejs.dev/) + [TypeScript](https://www.typescriptlang.org/) + [Antdv](https://antdv.com/docs/vue/getting-started-cn) + [Pinia](https://pinia.vuejs.org/)构建。

项目采用Vue 3 `<script setup>` SFC写法，请查看[script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup)了解更多信息。

### 构建运行

```bash
pnpm dev # 本地运行
pnpm build # 打包
```



##  参与贡献

### 环境准备

#### Node.js 和 pnpm

开发需要 Node.js 16+ 和 `pnpm` v8。

推荐使用 [`nvm`](https://github.com/nvm-sh/nvm) 管理 Node.js，避免权限问题的同时，还能够随时切换当前使用的 Node.js 的版本。在 Windows 系统下的开发者可以使用 [`nvm-windows`](https://github.com/coreybutler/nvm-windows)。

推荐使用`pnpm`，节约内存。 在 `pnpm` 的[官网](https://pnpm.io/installation)选择一种方式安装即可。

#### 编辑器

这边我们推荐使用 VSCode， 我们我们尽量采用工具化方式来约束开发规范和编码风格， 使用VSCode即可应用现有配置和推荐你安装适合项目的插件。 具体配置看`/.vscode` 目录

⚠️：因为我们在升级vue3的过程中vue2版本也在不断迭代， 为了确保我们始终是在最新的代码基础上开发，编写某个页面前记得先从`web-vue`  目录中找到同名文件先替换下。

## 目录结构

```
.
├── .vscode
│   └── setting.json
├── dist
├── mock
│   └── app.ts｜tsx
├── src
│   ├── components # 公共组件
│   ├── assets # 静态资源
│   ├── interface # 类型定义
│   ├── router # 路由配置
│   ├── stores # 状态管理器
│   │   └── index.ts
│   ├── pages # 页面
│   │   ├── login
│   │   └── user
│   ├── utils # 工具文件
│   │   └── index.ts
│   ├── api # 接口文件
│   │   └── api.ts
│   ├── app.vue
│   ├── main.ts
├── node_modules
├── .env
├── eslint.json
├── vite.config.ts // vite配置
├── package.json
├── tsconfig.json
└── type.d.ts
```











