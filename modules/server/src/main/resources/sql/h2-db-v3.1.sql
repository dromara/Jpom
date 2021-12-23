-- @author bwcx_jzy

ALTER TABLE OUT_GIVING
	ADD IF NOT EXISTS intervalTime int DEFAULT 10 comment '分发间隔时间';

ALTER TABLE NODE_INFO
	ADD IF NOT EXISTS unLockType VARCHAR (50) comment '锁定类型';

ALTER TABLE COMMAND_INFO
	ADD IF NOT EXISTS sshIds CLOB comment '绑定的ssh id';

ALTER TABLE COMMAND_INFO
	ADD IF NOT EXISTS autoExecCron VARCHAR (100) comment '自动执行表达式';

ALTER TABLE COMMAND_EXEC_LOG
	ADD IF NOT EXISTS triggerExecType int DEFAULT 0 comment '触发类型{0，手动，1 自动触发}';
