@REM Jpom Multi-branch, multi-remote merge code commands

@echo off

cd ../

echo Pull remote branch [master]
call git checkout dev

call git fetch github master:master

call git fetch gitee master:master

echo Pull remote branch [dev]
call git checkout master

call git fetch github dev:dev

call git fetch gitee dev:dev

echo Start merging branches [master]
call git checkout dev
call git merge master

echo Start merging branches [dev]
call git checkout master
call git merge dev

echo Push to gitee

call git push gitee dev

call git push gitee master

echo Push to github
call git push github dev

call git push github master

echo Push tags
call git push github --tags

call git push gitee --tags

call git checkout dev