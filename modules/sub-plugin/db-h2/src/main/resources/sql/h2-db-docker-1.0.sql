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

-- docker
CREATE TABLE IF NOT EXISTS PUBLIC.DOCKER_INFO
(
	id                VARCHAR(50)  not null comment 'id',
	createTimeMillis  BIGINT COMMENT '数据创建时间',
	modifyTimeMillis  BIGINT COMMENT '数据修改时间',
	modifyUser        VARCHAR(50) comment '修改人',
	strike            int     DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	workspaceId       varchar(50)  not null comment '所属工作空间',
	name              varchar(255) not null comment '名称',
	host              VARCHAR(255) comment 'docker host',
	apiVersion        VARCHAR(100) comment '接口版本',
	tlsVerify         TINYINT DEFAULT 0 comment 'tls 认证{1，启用，0 未启用}',
	status            TINYINT DEFAULT 0 comment '状态{1，启用，0 未启用}',
	failureMsg        VARCHAR(255) comment '错误消息',
	heartbeatTimeout  int comment '心跳超时时间',
	lastHeartbeatTime BIGINT COMMENT '最后心跳时间',
	dockerVersion     CLOB COMMENT 'docker 版本信息',
	CONSTRAINT DOCKER_INFO_PK PRIMARY KEY (id)
);
comment on table DOCKER_INFO is 'docker 信息';

ALTER TABLE DOCKER_INFO
	ADD IF NOT EXISTS tags VARCHAR(255) comment '容器标签';

ALTER TABLE DOCKER_INFO
	ADD IF NOT EXISTS swarmId VARCHAR(50) comment '集群ID';

ALTER TABLE DOCKER_INFO
	ADD IF NOT EXISTS swarmNodeId VARCHAR(50) comment '集群 节点ID';

-- docker swarm
CREATE TABLE IF NOT EXISTS PUBLIC.DOCKER_SWARM_INFO
(
	id               VARCHAR(50)  not null comment 'id',
	createTimeMillis BIGINT COMMENT '数据创建时间',
	modifyTimeMillis BIGINT COMMENT '数据修改时间',
	modifyUser       VARCHAR(50) comment '修改人',
	strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	workspaceId      varchar(50)  not null comment '所属工作空间',
	name             varchar(255) not null comment '名称',
	dockerId         VARCHAR(50)  not null COMMENT 'docker Id',
	swarmId          VARCHAR(50)  not null COMMENT 'swarm Id',
	swarmCreatedAt   BIGINT COMMENT '数据创建时间',
	swarmUpdatedAt   BIGINT COMMENT '数据创建时间',
	tag              VARCHAR(255) comment '容器标签',
	CONSTRAINT DOCKER_SWARM_INFO_PK PRIMARY KEY (id)
);
comment on table DOCKER_SWARM_INFO is 'docker 集群信息';

ALTER TABLE DOCKER_SWARM_INFO
	ADD IF NOT EXISTS nodeAddr VARCHAR(100) comment '集群 节点地址';


ALTER TABLE DOCKER_SWARM_INFO
	ADD IF NOT EXISTS status TINYINT comment '状态{1，启用，0 未启用}';
ALTER TABLE DOCKER_SWARM_INFO
	ADD IF NOT EXISTS failureMsg VARCHAR(255) comment '错误消息';


