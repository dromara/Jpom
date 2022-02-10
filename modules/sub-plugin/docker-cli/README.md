
## mac

> docker run -it -d --name=socat -p 2375:2375 -v    /var/run/docker.sock:/var/run/docker.sock bobrik/socat TCP4-LISTEN:2375,fork,reuseaddr UNIX-CONNECT:/var/run/docker.sock
> 
> lsof -i:2375


https://www.cnblogs.com/xiaoqi/p/docker-java.html

https://www.cnblogs.com/boshen-hzb/p/10714414.html

https://blog.csdn.net/qq_40321119/article/details/107951712

https://blog.csdn.net/u012946310/article/details/82315302

vim /usr/lib/systemd/system/docker.service

systemctl daemon-reload && systemctl restart docker

systemctl status docker.service

```
--tlsverify \
--tlscacert=/home/docker-ca/ca.pem \
--tlscert=/home/docker-ca/server-cert.pem \
--tlskey=/home/docker-ca/server-key.pem \
-H unix:///var/run/docker.sock -H tcp://0.0.0.0:2375
```

```
--tlsverify --tlscacert=/home/docker-ca/ca.pem --tlscert=/home/docker-ca/server-cert.pem --tlskey=/home/docker-ca/server-key.pem -H unix:///var/run/docker.sock -H tcp://0.0.0.0:2375
```

```
ExecStart=/usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock --tlsverify --tlscacert=/home/docker/tls-ca/ca.pem --tlscert=/home/docker/tls-ca/server-cert.pem --tlskey=/home/docker/tls-ca/server-key.pem -H tcp://0.0.0.0:2375
```