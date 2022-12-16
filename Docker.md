```shell
docker build --no-cache -t jpom-server-test -f ./DockerfileServerTest .

```

```shell
docker rm -f jpom-server-test && docker run --name jpom-server-test -v D:/data/jpom-server/conf:/usr/local/jpom-server/conf -p 2122:2122 jpom-server-test
```

```shell
docker rm -f jpom-server-test && docker run --name jpom-server-test -p 2122:2122 jpom-server-test
```