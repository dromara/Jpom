-- @author bwcx_jzy
-- @Date 2021-12-05
-- add new tables for convert other info...

ALTER TABLE MONITORNOTIFYLOG
	ADD IF NOT EXISTS LOGID VARCHAR (50) default '' comment 'id';

update MONITORNOTIFYLOG
set ID =LOGID
where ID = ''
  and LOGID is null
  and LOGID <> '';
