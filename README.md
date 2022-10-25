<p align="center">
	<a href="https://jpom.top/"  target="_blank">
	    <img src="https://jpom.top/images/jpom_logo.png" width="400" alt="logo">
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
	<a target="_blank" href="https://jpom.top/images/wx-qrcode-praise.png">
		<img src='https://img.shields.io/badge/%E5%BE%AE%E4%BF%A1%E7%BE%A4(%E8%AF%B7%E5%A4%87%E6%B3%A8%3AJpom)-jpom66-yellowgreen.svg' alt='jpom66 请备注jpom'/>
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
```

### 执行后需要重启 idea 才能生效

```shell
git submodule init
git submodule sync 
git submodule update --init
git submodule update --remote
```

```shell
# 创建空白分支 
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