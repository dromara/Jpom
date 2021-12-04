-- @author bwcx_jzy
-- @Date 2021-12-02
-- add new tables for storage other info...

-- 系统参数名称
CREATE TABLE IF NOT EXISTS PUBLIC.SYSTEM_PARAMETERS
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	MODIFYUSER       VARCHAR(50) comment '修改人',
	STRIKE           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	`VALUE`          CLOB comment '参数值，JSON 字符串格式',
	DESCRIPTION      varchar(255) comment '参数描述',
	CONSTRAINT SYSTEM_PARAMETERS_PK PRIMARY KEY (ID)
);
comment on table SYSTEM_PARAMETERS is '系统参数表';

-- 用户信息表
CREATE TABLE IF NOT EXISTS PUBLIC.USER_INFO
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	MODIFYUSER       VARCHAR(50) comment '修改人',
	STRIKE           int    DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	parent           VARCHAR(50) not null comment '创建人',
	`NAME`           VARCHAR(50) comment '昵称',
	systemUser       int    default 0 comment '是否为系统管理员 {1，是，0 否(默认)}',
	password         varchar(100) comment '密码',
	salt             varchar(50) comment '盐值',
	pwdErrorCount    int    default 0 comment '密码错误次数',
	lastPwdErrorTime BIGINT default 0 COMMENT '最后登录失败时间',
	lockTime         BIGINT default 0 comment '锁定时长',
	email            varchar(255) comment '邮箱地址',
	dingDing         varchar(255) comment '钉钉地址',
	workWx           varchar(255) comment '企业微信地址',
	CONSTRAINT USER_INFO_PK PRIMARY KEY (ID)
);
comment on table USER_INFO is '用户信息表';

-- 用户的盐值保证唯一
CREATE UNIQUE INDEX IF NOT EXISTS USER_INF_SALT_INDEX1 ON PUBLIC.USER_INFO (salt);

-- 工作空间表
CREATE TABLE IF NOT EXISTS PUBLIC.WORKSPACE
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	MODIFYUSER       VARCHAR(50) comment '修改人',
	STRIKE           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	`NAME`           VARCHAR(50) comment '名称',
	DESCRIPTION      varchar(255) comment '参数描述',
	CONSTRAINT WORKSPACE_PK PRIMARY KEY (ID)
);
comment on table WORKSPACE is '用户信息表';

-- 用户绑定工作空间表
CREATE TABLE IF NOT EXISTS PUBLIC.USER_BIND_WORKSPACE
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	userId           VARCHAR(50) not null comment '用户ID',
	workspaceId      VARCHAR(50) not null comment '工作空间ID',
	CONSTRAINT USER_BIND_WORKSPACE_PK PRIMARY KEY (ID)
);
comment on table USER_BIND_WORKSPACE is '用户工作空间绑定表';

