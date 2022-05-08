--
-- The MIT License (MIT)
--
-- Copyright (c) 2019 Code Technology Studio
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy of
-- this software and associated documentation files (the "Software"), to deal in
-- the Software without restriction, including without limitation the rights to
-- use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
-- the Software, and to permit persons to whom the Software is furnished to do so,
-- subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
-- FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
-- COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
-- IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
-- CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--

-- @author bwcx_jzy

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS webhook VARCHAR(255) comment 'webhook';

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS extraData CLOB COMMENT '额外信息，JSON 字符串格式';

-- @author jzy 字段类型修改为 json
-- @author Hotstrip 字段类型修改为 CLOB
ALTER TABLE BUILD_INFO
	ALTER COLUMN extraData CLOB COMMENT '额外信息，JSON 字符串格式';

-- @author jzy 增加构建产物字段长度
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS resultDirFile VARCHAR(200) comment '构建产物目录';
ALTER TABLE BUILD_INFO
	ALTER COLUMN resultDirFile VARCHAR(200) comment '构建产物目录';
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS buildId int comment '构建 id';

ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS gitUrl varchar(255) comment '仓库地址';

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS autoBuildCron VARCHAR(100) comment '自动构建表达式';

--
ALTER TABLE BACKUP_INFO
	ADD IF NOT EXISTS baleTimeStamp BIGINT comment '打包时间';
ALTER TABLE BACKUP_INFO
	ADD IF NOT EXISTS version varchar(255) comment '服务版本';
ALTER TABLE BACKUP_INFO
	ADD IF NOT EXISTS modifyUser VARCHAR(50) comment '操作人';

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS buildMode int comment '构建方式 {0 本地构建, 1 docker 构建}';

ALTER TABLE BUILD_INFO
	ALTER COLUMN releaseMethodDataId CLOB COMMENT '构建关联的数据ID';

ALTER TABLE BUILD_INFO
    ADD IF NOT EXISTS repositoryLastCommitId varchar(255) comment '仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）';
