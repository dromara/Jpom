
-- 命令行信息
CREATE TABLE IF NOT EXISTS PUBLIC.COMMAND_MODEL
(
	ID               VARCHAR(50) not null comment 'id',
	CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
	MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
	`NAME`           VARCHAR(100) comment '命令名称',
	DESC             VARCHAR(500) comment '命令描述',
	COMMAND          CLOB comment '指令内容',
	EXECUTIONROLE    VARCHAR(100) comment '执行用户，默认为root',
	executionPath    VARCHAR(200) comment '执行路径，默认为/root',
	TIMEOUT          int DEFAULT 60 comment '超时时间,单位：秒，默认60',
	TYPE             int DEFAULT 0 comment '命令类型，0-shell，1-powershell',
	PARAMS           CLOB comment '命令参数',
	MODIFYUSER       VARCHAR(50) comment '修改人',
	STRIKE           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
	CONSTRAINT COMMAND_MODEL_PK PRIMARY KEY (ID)
	);
comment on table COMMAND_MODEL is '命令行信息';
