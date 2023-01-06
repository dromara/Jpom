--
-- The MIT License (MIT)
--
-- Copyright (c) 2019 Code Technology Studio
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy of
-- this software and associated documentation files (the "Software"), to deal in
-- the Software without restriction, including without limitation the rights to
-- use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
-- the Software, and to permit persons to whom the Software is furnished to do so,
-- subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
-- FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
-- COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
-- IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
-- CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--

DROP FUNCTION IF EXISTS column_exists;

-- mysql delimiter

CREATE FUNCTION column_exists(
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

-- mysql delimiter


DROP PROCEDURE IF EXISTS drop_column_if_exists;

CREATE PROCEDURE drop_column_if_exists(
    tname VARCHAR(64),
    cname VARCHAR(64)
)
BEGIN
    IF column_exists(tname, cname)
    THEN
        SET @drop_column_if_exists = CONCAT('ALTER TABLE `', tname, '` DROP COLUMN `', cname, '`');
        PREPARE drop_query FROM @drop_column_if_exists;
        EXECUTE drop_query;
    END IF;
END

-- mysql delimiter

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

CREATE PROCEDURE add_column_if_not_exists(
    tname VARCHAR(64),
    cname VARCHAR(64),
    columninfo VARCHAR(64)
)
BEGIN
    IF column_exists(tname, cname)
    THEN
        SET @add_column_sql = '';
    else
        SET @add_column_sql = CONCAT('ALTER TABLE `', tname, '` ADD COLUMN ', columninfo);
        PREPARE execute_query FROM @add_column_sql;
        EXECUTE execute_query;
    END IF;
END

-- mysql delimiter

DROP PROCEDURE IF EXISTS drop_index_if_exists;
create procedure drop_index_if_exists(
    p_tablename varchar(200),
    p_idxname VARCHAR(200)
)
begin
    select count(*) into @cnt from information_schema.statistics where table_name = p_tablename and index_name = p_idxname;
    if @cnt > 0 then
        set @str = concat('drop index ', p_idxname, ' on ', p_tablename);
        PREPARE execute_query FROM @str;
        EXECUTE execute_query;
    end if;

end;
