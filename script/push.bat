@REM
@REM The MIT License (MIT)
@REM
@REM Copyright (c) 2019 Code Technology Studio
@REM
@REM Permission is hereby granted, free of charge, to any person obtaining a copy of
@REM this software and associated documentation files (the "Software"), to deal in
@REM the Software without restriction, including without limitation the rights to
@REM use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
@REM the Software, and to permit persons to whom the Software is furnished to do so,
@REM subject to the following conditions:
@REM
@REM The above copyright notice and this permission notice shall be included in all
@REM copies or substantial portions of the Software.
@REM
@REM THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
@REM IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
@REM FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
@REM COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
@REM IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
@REM CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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