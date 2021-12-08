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
	RSAPRV           VARCHAR(4096) comment 'SSH RSA 私钥',
	STRIKE           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	CONSTRAINT REPOSITORY_PK PRIMARY KEY (ID)
);
comment on table REPOSITORY is '仓库信息';

-- 构建信息
CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
	ID                  VARCHAR(50) not null comment 'id',
	REPOSITORYID        VARCHAR(50) not null comment '仓库 ID',
	CREATETIMEMILLIS    BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS    BIGINT COMMENT '数据修改时间',
	`NAME`              VARCHAR(50) comment '构建名称',
	BUILDID             int comment '构建 ID',
	`GROUP`             VARCHAR(50) comment '分组名称',
	BRANCHNAME          VARCHAR(50) comment '分支',
	SCRIPT              VARCHAR(200) comment '构建命令',
	RESULTDIRFILE       VARCHAR(50) comment '构建产物目录',
	RELEASEMETHOD       int comment '发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}',
	MODIFYUSER          VARCHAR(50) comment '修改人',
	`STATUS`            int comment '状态',
	TRIGGERTOKEN        VARCHAR(20) comment '触发器token',
	EXTRADATA           CLOB comment '额外信息，JSON 字符串格式',
	RELEASEMETHODDATAID VARCHAR(200) comment '构建关联的数据ID',
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

-- @author Hotstrip -> add REPOSITORYID in case it's not exists
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS REPOSITORYID VARCHAR(50) COMMENT '仓库 ID';

-- @author jzy 字段类型修改为 json
-- @author Hotstrip 字段类型修改为 CLOB
ALTER TABLE BUILD_INFO
	ALTER COLUMN EXTRADATA CLOB COMMENT '额外信息，JSON 字符串格式';

-- @author jzy 增加构建产物字段长度
ALTER TABLE BUILD_INFO
	ALTER COLUMN RESULTDIRFILE VARCHAR(200) comment '构建产物目录';

-- @author Hotstrip 增加 MODIFYUSER
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS MODIFYUSER VARCHAR(50) comment '修改人';

-- @author Hotstrip 增加 STATUS
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS `STATUS` int comment '状态';

-- @author Hotstrip 增加 TRIGGERTOKEN
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS TRIGGERTOKEN VARCHAR(20) comment '触发器token';

-- @author bwcx_jzy 2021-12-06
ALTER TABLE BUILD_INFO
	ALTER COLUMN TRIGGERTOKEN VARCHAR(100) comment '触发器token';

-- @author jzy 增加 RSAPRV
ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS RSAPRV VARCHAR(4096) comment 'SSH RSA 私钥';

-- @author Hotstrip update RSAPRV field length
ALTER TABLE REPOSITORY
	ALTER COLUMN RSAPRV VARCHAR(4096) comment 'SSH RSA 私钥';

-- @author bwcx_jzy 增加 RELEASEMETHODDATAID
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS RELEASEMETHODDATAID VARCHAR(200) comment '构建关联的数据ID';

-- @author lidaofu 增加 STRIKE
ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS STRIKE int DEFAULT 0 comment '逻辑删除';


-- @author bwcx_jzy 增加 MODIFYUSER
ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS MODIFYUSER VARCHAR(50) comment '修改人';

-- @author jzy
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS BRANCHTAGNAME VARCHAR(50) comment '标签';

-- @author hjk 增加字段长度，200->500
ALTER TABLE BUILD_INFO
	ALTER COLUMN SCRIPT VARCHAR(500) comment '构建命令';

-- 备份数据库信息表 @author Hotstrip
CREATE TABLE IF NOT EXISTS PUBLIC.BACKUP_INFO
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	`NAME`           VARCHAR(50) comment '备份名称',
	FILEPATH         VARCHAR(200) comment '文件地址',
	BACKUPTYPE       int comment '备份类型{0: 全量, 1: 部分}',
	FILESIZE         BIGINT comment '文件大小',
	SHA1SUM          VARCHAR(50) comment 'SHA1 信息',
	`STATUS`         int default '0' comment '状态{0: 默认; 1: 成功; 2: 失败}',
	CONSTRAINT BACKUP_INFO_PK PRIMARY KEY (ID)
);
comment on table BACKUP_INFO is '备份数据库信息';

-- @author Hotstrip 增加字段 status
ALTER TABLE BACKUP_INFO
	ADD IF NOT EXISTS `STATUS` int default '0' comment '状态{0: 默认; 1: 成功; 2: 失败}';

-- @author bwcx_jzy
ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS workspaceId VARCHAR(50) comment '所属工作空间';
ALTER TABLE REPOSITORY
	ADD IF NOT EXISTS workspaceId VARCHAR(50) comment '所属工作空间';
