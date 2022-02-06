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
	ADD IF NOT EXISTS tags VARCHAR(255) comment '容器标签Ï';


