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

-- @author Hotstrip
-- @Date 2021-07-31
-- add new tables for storage build info, repo info...

-- 仓库信息
CREATE TABLE IF NOT EXISTS PUBLIC.REPOSITORY
(
	id               VARCHAR(50) not null comment 'id',
	createTimeMillis BIGINT COMMENT '数据创建时间',
	modifyTimeMillis BIGINT COMMENT '数据修改时间',
	`name`           VARCHAR(50) comment '仓库名称',
	gitUrl           varchar(255) comment '仓库地址',
	repoType         int comment '仓库类型{0: GIT, 1: SVN}',
	protocol         int comment '拉取代码的协议{0: http, 1: ssh}',
	userName         VARCHAR(50) comment '登录用户',
	password         VARCHAR(50) comment '登录密码',
	rsaPub           VARCHAR(2048) comment 'SSH RSA 公钥',
	rsaPrv           VARCHAR(4096) comment 'SSH RSA 私钥',
	strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	CONSTRAINT REPOSITORY_PK PRIMARY KEY (id)
);
comment on table REPOSITORY is '仓库信息';

-- 构建信息
CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
	id                  VARCHAR(50) not null comment 'id',
	repositoryId        VARCHAR(50) not null comment '仓库 id',
	createTimeMillis    BIGINT COMMENT '数据创建时间',
	modifyTimeMillis    BIGINT COMMENT '数据修改时间',
	`name`              VARCHAR(50) comment '构建名称',
	buildId             int comment '构建 id',
	`group`             VARCHAR(50) comment '分组名称',
	branchName          VARCHAR(50) comment '分支',
	script              CLOB comment '构建命令',
	resultDirFile       VARCHAR(200) comment '构建产物目录',
	releaseMethod       int comment '发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}',
	modifyUser          VARCHAR(50) comment '修改人',
	`status`            int comment '状态',
	triggerToken        VARCHAR(20) comment '触发器token',
	extraData           CLOB comment '额外信息，JSON 字符串格式',
	releaseMethodDataId VARCHAR(200) comment '构建关联的数据ID',
	CONSTRAINT BUILD_INFO_PK PRIMARY KEY (id)
);
comment on table BUILD_INFO is '构建信息';

-- 备份数据库信息表 @author Hotstrip
CREATE TABLE IF NOT EXISTS PUBLIC.BACKUP_INFO
(
	id               VARCHAR(50) not null comment 'id',
	createTimeMillis BIGINT COMMENT '数据创建时间',
	modifyTimeMillis BIGINT COMMENT '数据修改时间',
	`name`           VARCHAR(50) comment '备份名称',
	filePath         VARCHAR(200) comment '文件地址',
	backupType       int comment '备份类型{0: 全量, 1: 部分}',
	fileSize         BIGINT comment '文件大小',
	sha1Sum          VARCHAR(50) comment 'SHA1 信息',
	`status`         int default '0' comment '状态{0: 默认; 1: 成功; 2: 失败}',
	CONSTRAINT BACKUP_INFO_PK PRIMARY KEY (id)
);
comment on table BACKUP_INFO is '备份数据库信息';

