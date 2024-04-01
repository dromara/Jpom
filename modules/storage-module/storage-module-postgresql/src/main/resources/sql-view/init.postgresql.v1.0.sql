--
-- Copyright (c) 2019 Of Him Code Technology Studio
-- Jpom is licensed under Mulan PSL v2.
-- You can use this software according to the terms and conditions of the Mulan PSL v2.
-- You may obtain a copy of Mulan PSL v2 at:
-- 			http://license.coscl.org.cn/MulanPSL2
-- THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
-- See the Mulan PSL v2 for more details.
--


DROP FUNCTION IF EXISTS column_exists;
CREATE FUNCTION column_exists(tname varchar, cname varchar)
RETURNS boolean
AS
$$
BEGIN
RETURN EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name  = tname
        AND column_name = cname
    );
END;
$$
LANGUAGE plpgsql;

-- postgresql $delimiter$

DROP PROCEDURE IF EXISTS exec_if_column_exists;
CREATE PROCEDURE exec_if_column_exists(
    tname varchar,
    cname varchar,
    statemetStr varchar
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF (
        select column_exists(tname,cname)
    ) THEN
        EXECUTE statemetStr;
END IF;
END;
$$;

-- postgresql $delimiter$
