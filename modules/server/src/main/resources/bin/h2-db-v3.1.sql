-- @author bwcx_jzy

ALTER TABLE OUT_GIVING
	ADD IF NOT EXISTS intervalTime int DEFAULT 10 comment '分发间隔时间';
