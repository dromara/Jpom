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

--  创建操作记录表
CREATE TABLE IF NOT EXISTS PUBLIC.USEROPERATELOGV1
(
	id        VARCHAR(50) not null comment 'id',
	reqId     VARCHAR(50) COMMENT '请求ID',
	ip        VARCHAR(30) COMMENT '客户端IP地址',
	userId    VARCHAR(30) COMMENT '操作的用户ID',
	resultMsg CLOB COMMENT '操作的结果信息',
	optType   INTEGER COMMENT '操作类型',
	optStatus INTEGER COMMENT '操作状态 成功/失败',
	optTime   BIGINT COMMENT '操作时间',
	nodeId    VARCHAR(30) COMMENT '节点ID',
	dataId    VARCHAR(50) COMMENT '操作的数据ID',
	userAgent CLOB COMMENT '浏览器标识',
	reqData   CLOB COMMENT '用户请求参数',
	CONSTRAINT USEROPERATELOGV1_PK PRIMARY KEY (id)
);
COMMENT ON TABLE USEROPERATELOGV1 is '操作日志';

-- 监控异常记录表
CREATE TABLE IF NOT EXISTS PUBLIC.MONITORNOTIFYLOG
(
	id           VARCHAR(50) NOT NULL COMMENT '记录id',
	monitorId    varchar(50) COMMENT '监控id',
	nodeId       VARCHAR(30) COMMENT '节点id',
	projectId    VARCHAR(30) COMMENT '项目id',
	createTime   BIGINT COMMENT '异常时间',
	title        VARCHAR(100) COMMENT '异常描述',
	content      CLOB COMMENT '异常内容',
	status       TINYINT COMMENT '当前状态',
	notifyStyle  TINYINT COMMENT '通知方式',
	notifyStatus TINYINT COMMENT '通知状态',
	notifyObject CLOB COMMENT '通知对象',
	notifyError  CLOB COMMENT '通知异常内容',
	CONSTRAINT MONITORNOTIFYLOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE MONITORNOTIFYLOG is '监控异常日志记录';

-- 构建历史
CREATE TABLE IF NOT EXISTS PUBLIC.BUILDHISTORYLOG
(
	id                  VARCHAR(50) not null COMMENT '表id',
	buildDataId         VARCHAR(50) COMMENT '构建的数据id',
	buildNumberId       INTEGER COMMENT '构建编号',
	status              TINYINT COMMENT '构建状态',
	startTime           BIGINT COMMENT '开始时间',
	endTime             BIGINT COMMENT '结束时间',
	resultDirFile       VARCHAR(200) COMMENT '构建产物目录',
	//BUILDUSER           VARCHAR(50) COMMENT '构建人',
	releaseMethod       TINYINT COMMENT '发布方式',
	releaseMethodDataId VARCHAR(200) COMMENT '发布的数据id',
	afterOpt            TINYINT COMMENT '发布后操作',
	CONSTRAINT BUILDHISTORYLOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE BUILDHISTORYLOG is '构建历史记录';


-- 分发日志
CREATE TABLE IF NOT EXISTS PUBLIC.OUTGIVINGLOG
(
	id          VARCHAR(50) not null comment 'id',
	outGivingId VARCHAR(50) comment '分发id',
	status      TINYINT comment '状态',
	startTime   BIGINT comment '开始时间',
	endTime     BIGINT comment '结束时间',
	result      CLOB comment '消息',
	nodeId      VARCHAR(100) comment '节点id',
	projectId   VARCHAR(100) comment '项目id',
	CONSTRAINT OUTGIVINGLOG_PK PRIMARY KEY (id)
);
comment on table OUTGIVINGLOG is '分发日志';

-- 系统监控记录
CREATE TABLE IF NOT EXISTS PUBLIC.SYSTEMMONITORLOG
(
	id           VARCHAR(50) not null comment 'id',
	nodeId       VARCHAR(100) comment '节点id',
	monitorTime  BIGINT comment '监控时间',
	occupyCpu    DOUBLE comment '占用cpu',
	occupyMemory DOUBLE comment '占用内存',
	occupyDisk   DOUBLE comment '占用磁盘',
	CONSTRAINT SYSTEMMONITORLOG_PK PRIMARY KEY (id)
);
comment on table SYSTEMMONITORLOG is '系统监控记录';

ALTER TABLE SYSTEMMONITORLOG
	ALTER COLUMN id VARCHAR(50) COMMENT 'id';

ALTER TABLE SYSTEMMONITORLOG
	ADD IF NOT EXISTS createTimeMillis BIGINT COMMENT '数据创建时间';
ALTER TABLE SYSTEMMONITORLOG
	ADD IF NOT EXISTS modifyTimeMillis BIGINT COMMENT '数据修改时间';

ALTER TABLE SYSTEMMONITORLOG
	ALTER COLUMN monitorTime BIGINT;


CREATE UNIQUE INDEX IF NOT EXISTS SYSTEMMONITORLOG_INDEX1 ON PUBLIC.SYSTEMMONITORLOG (nodeId, monitorTime);

-- @author jzy
ALTER TABLE SYSTEMMONITORLOG
	ADD COLUMN IF NOT EXISTS occupyMemoryUsed DOUBLE COMMENT '占用内存 (使用)';

--  ssh 终端操作记录表
CREATE TABLE IF NOT EXISTS PUBLIC.SSHTERMINALEXECUTELOG
(
	id        VARCHAR(50) NOT NULL COMMENT 'id',
	ip        VARCHAR(30) COMMENT '客户端IP地址',
	userId    VARCHAR(30) COMMENT '操作的用户ID',
	optTime   BIGINT COMMENT '操作时间',
	userAgent CLOB COMMENT '浏览器标识',
	commands  CLOB comment '操作的命令',
	sshId     varchar(50) comment '操作的sshid',
	sshName   varchar(50) comment '操作的ssh name',
	refuse    INTEGER COMMENT '拒绝执行',
	CONSTRAINT SSHTERMINALEXECUTELOG_PK PRIMARY KEY (id)
);
COMMENT ON TABLE SSHTERMINALEXECUTELOG is 'ssh 终端操作记录表';





