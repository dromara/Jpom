@REM
@REM Copyright (c) 2019 Code Technology Studio
@REM Jpom is licensed under Mulan PSL v2.
@REM You can use this software according to the terms and conditions of the Mulan PSL v2.
@REM You may obtain a copy of Mulan PSL v2 at:
@REM 			http://license.coscl.org.cn/MulanPSL2
@REM THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
@REM See the Mulan PSL v2 for more details.
@REM

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