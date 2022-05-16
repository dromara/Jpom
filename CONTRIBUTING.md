# Jpom 贡献说明

## 目录说明

```
.
├── .gitee    		=> gitee 配置
├── docs      		=> 一键安装的命令脚本以及版本号文件
├── modules   		=> java 后端目录（agent、server）
   ├── agent 		=> 插件端代码
   ├── common 		=> 这个项目的公共模块（插件端、服务端都依赖该模块）
   ├── server 		=> 服务端代码
   ├── sub-plugin 	=> 插件模块
├── script    		=> 一些通用脚本
├── web-vue   		=> 前端 vue 目录
   ├── .editorconfig 	=> 前端（vue）代码格式配置
├── .editorconfig   	=> 全局代码格式配置
├── .gitattributes   	=> 文件编码格式配置
└── ....      		=> 仓库一些默认配置
```

## 一些规范说明

1. 写完代码后在保证不影响其他的人的代码情况下尽量统一格式化一下代码
	1. 采用 4 个空格缩进，禁止使用 tab 字符
	2. 如果使用 tab 缩进，必须设置 1 个 tab 为 4 个空格。IDEA 设置 tab 为 4 个空格时，
	   请勿勾选 Use tab character；而在 eclipse 中，必须勾选 insert spaces for tabs
2. Java 代码需要保证新增方法都有充足、标准的 JavaDoc 注释
3. 在修改 Bug、新增功能尽量保证最小提交的方式提交代码，减少多个功能一个 commit
4. 所有接口 url 都需要遵循下划线模式
5. Java 代码、方法需要遵循小驼峰法
6. Java 类名需要遵循大驼峰法
7. 前端项目统一采用 `prettier` 方式来格式化（需要安装插件）
8. 所有 controller 层的接口都需要添加文档注释（至少包含接口的作用说明、参数说明、返回值说明及添加 apiDoc 文档注释）

> 注：由于旧代码存在很多不规范问题，会逐步调整为新规范。在新写的代码都需要需要遵循上面说明
>
>
### 类的文档注释规范（Javadoc）

```
/**
 * xxxxxxxx
 * @author xxxx
 * @since ${DATE}
 */
```

> 这里采用 `@since` 声明创建日期是因为 `Javadoc` 规范里面并没有 `@date` 标记所以采用 `@since` 代替

### Java 代码规范

> 推荐安装 `Alibaba Java Coding Guidelines`（`p3c`） 插件

##### 代码级别的多行注释

[https://www.e-learn.cn/topic/3680721](https://www.e-learn.cn/topic/3680721)

## changelog 更新规范

> 在新加功能、修复bug、优化功能在完成时候都需要在 [CHANGELOG.md](./CHANGELOG.md) 记录

1. 如果是使用者反馈的bug，在修复后需要备注反馈人的昵称
2. 如果是 issue 需要备注 issue 地址以及平台（Gitee、GitHub）
3. 如果是 pr 需要备注 pr 地址以及平台（Gitee、GitHub）
4. 根据变动情况确定影响范围：如果影响 只：`agent`、`server` 其中一个，就使用【agent】、【server】开头，如果都影响就不用
5. 可以视情况添加其他说明：如提交记录
6. emoji 表情参考：[https://emojixd.com/](https://emojixd.com/)

## apiDoc 文档注释规范
### 【强制】所有需要包含在 apiDoc 文档中的接口，都必须有 `@api` 文档标记
说明：如果没有 `@api` 文档标记，则定义的文档不会出现在生成后的 apiDoc 文档中。

### 【强制】所有 apiDoc 的文档标记必须定义在 javaDoc 标记的后面
说明：如果先定义 javaDoc 文档标记，再定义 apiDoc 的文档标记，则 javaDoc 的标记可能会包含在 apiDoc 的标记属性中，这并不是我们想要的结果。

正例：
```
/**
* @author hjk
* @api {method} path title
* @apiParam {Number} id Users unique ID.
*/
```

反例：

说明：参数 id 的说明应该是 Users unique ID. 如果这样定义则变成了 Users unique ID.@author hjk
```
/**
* @api {method} path title
* @apiParam {Number} id Users unique ID.
* @author hjk
*/
```

### 【强制】定义通用文档块

说明：使用 `@apiDefine` 定义通用的文档块，然后使用 `@apiUse` 来引用，增强文档块的复用性。

所有的文档块统一定义在 `server` 模块下的 `io.jpom.ApiDoc`


## 分支说明

1. 新功能都提交到 dev 分支, 不能提交到 master 分支
2. PR 提交到 dev 分支
3. 一般功能开发可以直接提交到 dev 分支，较大功能开发需要新建分支提交

## 需要的小组

1. 后端小组 （主要任务：根据需求开发对应的接口）
2. 前端小组 （主要任务：优化前端 UI 交互和对接部分接口）
3. 文档小组 （主要任务：完善、补充 Jpom 使用文档）
4. 视频小组 （主要任务：录制 Jpom 相关的使用视频）
5. 测试小组 （主要任务：参与 Jpom 新版内测、日常开发测试相关任务）