--
-- Copyright (c) 2019 Of Him Code Technology Studio
-- Jpom is licensed under Mulan PSL v2.
-- You can use this software according to the terms and conditions of the Mulan PSL v2.
-- You may obtain a copy of Mulan PSL v2 at:
-- 			http://license.coscl.org.cn/MulanPSL2
-- THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
-- See the Mulan PSL v2 for more details.
--

DROP FUNCTION IF EXISTS column_exists1;

-- mariadb delimiter

CREATE FUNCTION column_exists1(
    tname VARCHAR(64),
    cname VARCHAR(64)
)
    RETURNS BOOLEAN
    READS SQL DATA
BEGIN
    RETURN 0 < (SELECT COUNT(*)
                FROM `INFORMATION_SCHEMA`.`COLUMNS`
                WHERE `TABLE_SCHEMA` = SCHEMA()
                  AND `TABLE_NAME` = tname
                  AND `COLUMN_NAME` = cname);
END

-- mariadb delimiter

DROP PROCEDURE IF EXISTS drop_column_if_exists;

-- mariadb delimiter

CREATE PROCEDURE drop_column_if_exists(
    tname VARCHAR(64),
    cname VARCHAR(64)
)
BEGIN
    IF column_exists1(tname, cname)
    THEN
        SET @drop_column_if_exists = CONCAT('ALTER TABLE `', tname, '` DROP COLUMN `', cname, '`');
        PREPARE drop_query FROM @drop_column_if_exists;
        EXECUTE drop_query;
    END IF;
END

-- mariadb delimiter

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- mariadb delimiter

CREATE PROCEDURE add_column_if_not_exists(
    tname VARCHAR(64),
    cname VARCHAR(64),
    columninfo VARCHAR(200)
)
BEGIN
    IF column_exists1(tname, cname)
    THEN
        SET @add_column_sql = '';
    else
        SET @add_column_sql = CONCAT('ALTER TABLE `', tname, '` ADD COLUMN ', columninfo);
        PREPARE execute_query FROM @add_column_sql;
        EXECUTE execute_query;
    END IF;
END

-- mariadb delimiter

DROP PROCEDURE IF EXISTS drop_index_if_exists;

-- mariadb delimiter

create procedure drop_index_if_exists(
    p_tablename varchar(200),
    p_idxname VARCHAR(200)
)
begin
    select count(*) into @cnt from information_schema.statistics where `TABLE_SCHEMA` = SCHEMA() and table_name = p_tablename and index_name = p_idxname;
    if @cnt > 0 then
        set @str = concat('drop index ', p_idxname, ' on ', p_tablename);
        PREPARE execute_query FROM @str;
        EXECUTE execute_query;
    end if;

end;
