-- @author bwcx_jzy

ALTER TABLE OUT_GIVING
	ADD IF NOT EXISTS intervalTime int DEFAULT 10 comment '分发间隔时间';

ALTER TABLE NODE_INFO
	ADD IF NOT EXISTS unLockType VARCHAR(50) comment '锁定类型';
