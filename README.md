<p align="center">
	<a href="https://jpom.top/"  target="_blank">
	    <img src="https://jpom.top/images/logo/jpom_logo.svg" width="400" alt="logo">
	</a>
</p>
<p align="center">
	<strong>简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件</strong>
</p>

<p align="center">
	<a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src='https://gitee.com/dromara/Jpom/badge/star.svg?theme=gvp' alt='gitee star'/>
    </a>
 	<a target="_blank" href="https://github.com/dromara/Jpom">
		<img src="https://img.shields.io/github/stars/dromara/Jpom.svg?style=social" alt="github star"/>
    </a>
    <a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src='https://img.shields.io/github/license/dromara/Jpom?style=flat' alt='license'/>
    </a>
    <a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src='https://img.shields.io/badge/JDK-1.8.0_40+-green.svg' alt='jdk'/>
    </a>
</p>

<p align="center">
    <a target="_blank" href="https://travis-ci.org/dromara/Jpom">
        <img src='https://travis-ci.org/dromara/Jpom.svg?branch=master' alt='travis'/>
    </a>
    <a target="_blank" href="https://www.codacy.com/gh/dromara/Jpom/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dromara/Jpom&amp;utm_campaign=Badge_Grade">
      <img src="https://app.codacy.com/project/badge/Grade/843b953f1446449c9a075e44ea778336" alt="codacy"/>
    </a>
</p>

<p align="center">
	👉 <a target="_blank" href="https://jpom.top/">https://jpom.top/</a> 👈
</p>
<p align="center">
	备用地址：<a target="_blank" href="https://jpom.top/">https://jpom.top/</a> 
</p>

# 使用 git submodule 合并管理代码和文档仓库

```shell
[submodule "jpom-parent"]
	path = jpom-parent
	url = git@gitee.com:dromara/Jpom.git
	branch = master
[submodule "docs"]
	path = docs
	url = git@gitee.com:dromara/Jpom.git
	branch = docs
[submodule "download-link"]
	path = download-link
	url = git@gitee.com:dromara/Jpom.git
	branch = download_link
```

```shell
git submodule add -b master git@gitee.com:dromara/Jpom.git jpom-parent

git submodule add -b docs git@gitee.com:dromara/Jpom.git docs
git submodule add -b download_link git@gitee.com:dromara/Jpom.git download-link
git submodule add -b aliyun-computenest git@gitee.com:dromara/Jpom.git aliyun-computenest
```

### 执行后需要重启 idea 才能生效

```shell
git submodule init
git submodule sync 
git submodule update --init
git submodule update --remote
```

```shell
# 创建空白分支 (docs-pages)
git checkout --orphan docs-pages
git rm -rf .
git push --set-upstream origin docs-pages
```

## 换行符不生效

```shell
git config --global core.autocrlf false
```

## git config

```shell
git config --global user.name "bwcx_jzy"
git config --global user.email bwcx_jzy@163.com 
git config --global core.autocrlf false
```

```shell
chmod 600 ~/.ssh/id_rsa ~/.ssh/id_rsa.pub
```

## 同步 tag

git tag -l | xargs git tag -d #删除所有本地分支
git fetch origin --prune #从远程拉取所有信息

## 版权

```shell
mvn license:format
```

| emoji                              | emoji 代码                   | 参考实体 | commit 说明       | 
|------------------------------------|----------------------------|------|-----------------|
| :art: (调色板)                        | :art:                      | 🎨   | 改进代码结构/代码格式     |
| :zap:(闪电)                          | :zap:                      | ⚡    | 提高性能            |
| :fire: (火焰)                        | :fire:                     | 🔥   | 移除代码或文件         |
| :bug: (bug)                        | :bug:                      | 🐞   | 修复 bug          |
| :ambulance: (急救车)                  | :ambulance:                | 🚑   | 重要补丁            |
| :sparkles: (火花)                    | :sparkles:                 | ✨    | 引入新功能           |
| :memo: (备忘录)                       | :memo:                     | 📝   | 撰写文档            |
| :rocket: (火箭)                      | :rocket:                   | 🚀   | 部署功能            |
| :lipstick: (口红)                    | :lipstick:                 | 💄   | 更新 UI 和样式文件     |
| :tada: (庆祝)                        | :tada:                     | 🥳   | 初次提交            |
| :white_check_mark: (白色复选框)         | :white_check_mark:         | ☑    | 增加测试            |
| :lock: (锁)                         | :lock:                     | 🔒   | 修复安全问题          |
| :apple: (苹果)                       | :apple:                    | 🍎   | 修复 macOS 下的问题   |
| :penguin: (企鹅)                     | :penguin:                  | 🐧   | 修复 Linux 下的问题   |
| :checkered_flag: (旗帜)              | :checked_flag:             | 🚩   | 修复 Windows 下的问题 |
| :bookmark: (书签)                    | :bookmark:                 | 🔖   | 发行/版本标签         |
| :rotating_light: (警车灯)             | :rotating_light:           | 🚨   | 移除 linter 警告    |
| :construction: (施工)                | :construction:             | 🏗   | 工作进行中           |
| :green_heart: (绿心)                 | :green_heart:              | 💚   | 修复 CI 构建问题      |
| :arrow_down: (下降箭头)                | :arrow_down:               | ⬇    | 降级依赖            |
| :arrow_up: (上升箭头)                  | :arrow_up:                 | ⬆    | 升级依赖            |
| :construction_worker: (工人)         | :construction_worker:      | 👷   | 添加 CI 构建系统      |
| :chart_with_upwards_trend: (上升趋势图) | :chart_with_upwards_trend: | 📈   | 添加分析或跟踪代码       |
| :hammer: (锤子)                      | :hammer:                   | 🔨   | 重大重构            |
| :heavy_minus_sign: (减号)            | :heavy_minus_sign:         | ➖    | 减少一个依赖          |
| :whale: (鲸鱼)                       | :whale:                    | 🐋   | Docker 相关工作     |
| :heavy_plus_sign: (加号)             | :heavy_plug_sign:          | ➕    | 增加一个依赖          |
| :wrench: (扳手)                      | :wrench:                   | 🔧   | 修改配置文件          |
| :globe_with_meridians: (地球)        | :globe_with_meridians:     | 🌎   | 国际化与本地化         |
| :pencil2: (铅笔)                     | :pencil2:                  | ✏    | 修复错别字           |
| :ok_hand: (OK 手势)                  | :ok_hand:                  | 👌   | 由于代码审查更改而更新代码   |
|                                    |                            | 🔙   | 回退              |