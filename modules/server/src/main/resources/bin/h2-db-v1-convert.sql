-- @author bwcx_jzy
-- @Date 2021-12-005
-- add new tables for convert other info...

update MONITORNOTIFYLOG
set MONITORNOTIFYLOG.ID =MONITORNOTIFYLOG.LOGID
where MONITORNOTIFYLOG.ID = '';
