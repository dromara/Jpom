-- @author bwcx_jzy

ALTER TABLE OUT_GIVING
	ADD IF NOT EXISTS intervalTime int DEFAULT 10 comment '分发间隔时间';

ALTER TABLE NODE_INFO
	ADD IF NOT EXISTS unLockType VARCHAR(50) comment '锁定类型';

ALTER TABLE COMMAND_INFO
	ADD IF NOT EXISTS sshIds CLOB comment '绑定的ssh id';

ALTER TABLE COMMAND_INFO
	ADD IF NOT EXISTS autoExecCron VARCHAR(100) comment '自动执行表达式';

ALTER TABLE COMMAND_EXEC_LOG
	ADD IF NOT EXISTS triggerExecType int DEFAULT 0 comment '触发类型{0，手动，1 自动触发}';

-- 分发状态
ALTER TABLE OUT_GIVING
	ADD IF NOT EXISTS `status` int default '0' comment '状态{0: 未分发; 1: 分发中; 2: 分发结束}';

ALTER TABLE SCRIPT_EXECUTE_LOG
	ADD IF NOT EXISTS scriptName VARCHAR(100) comment '脚本名称';

ALTER TABLE SCRIPT_INFO
	ADD IF NOT EXISTS autoExecCron VARCHAR(100) comment '自动执行表达式';

ALTER TABLE SCRIPT_INFO
	ADD IF NOT EXISTS defArgs CLOB comment '默认参数';

ALTER TABLE SCRIPT_EXECUTE_LOG
	ADD IF NOT EXISTS triggerExecType int DEFAULT 0 comment '触发类型{0，手动，1 自动触发}';

ALTER TABLE NODE_INFO
	ADD IF NOT EXISTS `group` VARCHAR(50) comment '分组名称';

ALTER TABLE SCRIPT_INFO
	ADD IF NOT EXISTS description varchar(255) comment '描述';

ALTER TABLE SCRIPT_INFO
	ADD IF NOT EXISTS scriptType varchar(100) comment '脚本类型';

ALTER TABLE SERVER_SCRIPT_INFO
	ADD IF NOT EXISTS nodeIds CLOB comment '绑定的节点 id';

ALTER TABLE MONITOR_INFO
	ADD IF NOT EXISTS execCron VARCHAR(100) comment '自动执行表达式';

ALTER TABLE USER_BIND_WORKSPACE
	ALTER COLUMN workspaceId VARCHAR(100) comment '工作空间ID';

ALTER TABLE USER_INFO
	ADD IF NOT EXISTS twoFactorAuthKey VARCHAR(100) comment '两步验证';

ALTER TABLE NODE_STAT
    ADD IF NOT EXISTS `group` VARCHAR(50) comment '分组名称';

ALTER TABLE MONITOR_INFO
    ADD IF NOT EXISTS webhook VARCHAR(255) comment 'webhook';