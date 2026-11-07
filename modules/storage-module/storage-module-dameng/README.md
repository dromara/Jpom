# dameng

https://juejin.cn/post/7318446303536594953

https://eco.dameng.com/community/training/afa88aa3975bf73c08a7d9058310e511

```shell
docker run -d -p 5236:5236 --restart=always --name dameng --privileged=true -e PAGE_SIZE=16 -e LD_LIBRARY_PATH=/opt/dmdbms/bin -e INSTANCE_NAME=dm8db xuxuclassmate/dameng
```
