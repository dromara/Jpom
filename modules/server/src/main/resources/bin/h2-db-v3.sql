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

