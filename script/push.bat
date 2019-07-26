@REM Jpom多分支、多远端合并代码命令

@echo off

cd ../

call git pull github master:master

call git pull gitee master:master

call git pull github master

call git pull gitee master

echo 推送到

call git push github master

call git push gitee master

echo 推送tags
call git push github --tags

call git push gitee --tags
