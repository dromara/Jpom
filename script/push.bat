@REM Jpom多分支、多远端合并代码命令

@echo off

cd ../


echo 推送到gitee

call git fetch github master:master

call git fetch gitee master:master

call git push gitee master

echo 推送到github

call git push github master

echo 推送tags
call git push github --tags

call git push gitee --tags
