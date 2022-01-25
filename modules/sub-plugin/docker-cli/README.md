
## mac

> docker run -it -d --name=socat -p 2375:2375 -v    /var/run/docker.sock:/var/run/docker.sock bobrik/socat TCP4-LISTEN:2375,fork,reuseaddr UNIX-CONNECT:/var/run/docker.sock
> 
> lsof -i:2375


https://www.cnblogs.com/xiaoqi/p/docker-java.html