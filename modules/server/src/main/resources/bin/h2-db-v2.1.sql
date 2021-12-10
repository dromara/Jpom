-- @author bwcx_jzy

ALTER TABLE BUILD_INFO
	ADD IF NOT EXISTS webhook VARCHAR (255) comment 'webhook';
