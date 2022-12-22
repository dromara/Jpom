# 使用 buildx 构建多平台 Docker 镜像

[https://blog.bwcxtech.com/posts/43dd6afb/](https://blog.bwcxtech.com/posts/43dd6afb/)

## 1. 启用 buildx 插件

开启 Dockerd 的实验特性

要想使用 buildx，首先要确保 Docker 版本不低于 19.03，同时还要通过设置环境变量 DOCKER_CLI_EXPERIMENTAL
来启用。可以通过下面的命令来为当前终端启用 buildx 插件：

编辑 /etc/docker/daemon.json，新增如下条目

```json
{
  "experimental": true
}
```

## 2 启用 binfmt_misc

如果你使用的是 Docker 桌面版（MacOS 和 Windows），默认已经启用了 binfmt_misc，可以跳过这一步。

如果你使用的是 Linux，需要手动启用 binfmt_misc。大多数 Linux 发行版都很容易启用，不过还有一个更容易的办法，直接运行一个特权容器，容器里面写好了设置脚本：

```shell
docker run --rm --privileged docker/binfmt:a7996909642ee92942dcd6cff44b9b95f08dad64
```

建议将 Linux 内核版本升级到 4.x 以上，特别是 CentOS 用户，你可能会遇到错误。

在软件依赖中我们提到需要 Linux 内核版本>= 4.8.0；如果在内核版本为3.10.0的系统（比如 CentOS）上运行 docker/binfmt，会出现报错
Cannot write to /proc/sys/fs/binfmt_misc/register: write /proc/sys/fs/binfmt_misc/register: invalid
argument，这是由于内核不支持（F）标志造成的。出现这种情况，建议您升级系统内核或者换使用较高版本内核的 Linux 发行版。

验证是 binfmt_misc 否开启：

```shell
ls -al /proc/sys/fs/binfmt_misc/
```

## 3 从默认的构建器切换到多平台构建器

Docker 默认会使用不支持多 CPU 架构的构建器，我们需要手动切换。

先创建一个新的构建器：

```shell
docker buildx create --use --name mybuilder
docker buildx use mybuilder
```
启动构建器：

```shell
docker buildx inspect mybuilder --bootstrap
```

