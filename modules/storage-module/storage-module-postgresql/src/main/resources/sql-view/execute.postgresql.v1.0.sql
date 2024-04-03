--
-- Copyright (c) 2019 Of Him Code Technology Studio
-- Jpom is licensed under Mulan PSL v2.
-- You can use this software according to the terms and conditions of the Mulan PSL v2.
-- You may obtain a copy of Mulan PSL v2 at:
-- 			http://license.coscl.org.cn/MulanPSL2
-- THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
-- See the Mulan PSL v2 for more details.
--

DROP PROCEDURE IF EXISTS drop_column_if_exists;
CREATE PROCEDURE drop_column_if_exists(
    tname varchar,
    cname varchar
)
LANGUAGE plpgsql
AS $$
DECLARE
    drop_query varchar;
BEGIN
    -- 检查列是否存在
    IF (select column_exists(tname,cname))  THEN
        -- 构造ALTER TABLE语句
        drop_query := format('ALTER TABLE %s DROP COLUMN %s', tname, cname);
        -- 执行ALTER TABLE语句
        EXECUTE drop_query;
    END IF;
END;
$$;

-- postgresql $delimiter$

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
CREATE PROCEDURE add_column_if_not_exists(
    tname varchar,
    cname varchar,
    columninfo varchar
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT (
        select column_exists(tname,cname)
    ) THEN
        -- 构造并执行ALTER TABLE语句来添加新列
        EXECUTE format('ALTER TABLE %s ADD COLUMN %s ', tname, columninfo);
    END IF;
END;
$$;

-- postgresql $delimiter$

DROP PROCEDURE IF EXISTS drop_index_if_exists;
CREATE PROCEDURE drop_index_if_exists(
    p_tablename varchar,
    p_idxname varchar
)
LANGUAGE plpgsql
AS $$
DECLARE
    idx_exists boolean;
    drop_idx_sql text;
BEGIN
    -- 检查索引是否存在
    SELECT EXISTS (
               SELECT 1
               FROM pg_indexes
               WHERE tablename = p_tablename
                 AND indexname = p_idxname
               ) INTO idx_exists;

    -- 如果索引存在，则构建DROP INDEX语句并执行
    IF idx_exists THEN
        drop_idx_sql := format('DROP INDEX IF EXISTS %s', p_idxname);
        EXECUTE drop_idx_sql;
    END IF;
END;
$$;

-- postgresql $delimiter$

-- 实现 instr函数，这个是postgresql上没有的
DROP FUNCTION IF EXISTS instr;
CREATE FUNCTION instr(str1 text, str2 text)
RETURNS boolean AS
$$
    SELECT POSITION(str2 IN str1)  > 0;
$$
LANGUAGE sql;
