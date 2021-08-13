-- @author Hotstrip
-- @Date 2021-07-31
-- add new tables for storage build info, repo info...

-- 仓库信息
CREATE TABLE IF NOT EXISTS PUBLIC.REPOSITORY
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	`NAME`           VARCHAR(50) comment '仓库名称',
	GITURL           varchar(255) comment '仓库地址',
	REPOTYPE         int comment '仓库类型{0: GIT, 1: SVN}',
	PROTOCOL         int comment '拉取代码的协议{0: http, 1: ssh}',
	USERNAME         VARCHAR(50) comment '登录用户',
	PASSWORD         VARCHAR(50) comment '登录密码',
	RSAPUB           VARCHAR(2048) comment 'SSH RSA 公钥',
	MODIFYTIME       datetime comment '修改时间',
	CONSTRAINT REPOSITORY_PK PRIMARY KEY (ID)
);
comment on table REPOSITORY is '仓库信息';

-- 构建信息
CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	`NAME`           VARCHAR(50) comment '构建名称',
	BUILDID          int comment '构建 ID',
	`GROUP`          VARCHAR(50) comment '分组名称',
	BRANCHNAME       VARCHAR(50) comment '分支',
	SCRIPT           VARCHAR(200) comment '构建命令',
	RESULTDIRFILE    VARCHAR(50) comment '构建产物目录',
	RELEASEMETHOD    int comment '发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}',
	EXTRADATA        VARCHAR(200) comment '额外信息，JSON 字符串格式',
	CONSTRAINT BUILD_INFO_PK PRIMARY KEY (ID)
);
comment on table BUILD_INFO is '构建信息';

-- @author jzy 2021-08-13 添加基础字段

ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS CREATETIMEMILLIS BIGINT COMMENT '数据创建时间';
ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间';

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS CREATETIMEMILLIS BIGINT COMMENT '数据创建时间';
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间';

-- @author jzy 字段类型修改为 json
ALTER TABLE BUILD_INFO
	ALTER COLUMN EXTRADATA JSON COMMENT '额外信息，JSON 字符串格式';
