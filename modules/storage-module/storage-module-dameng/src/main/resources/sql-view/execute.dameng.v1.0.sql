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

-- dameng delimiter

CREATE OR REPLACE FUNCTION column_exists(
    tname VARCHAR2,
    cname VARCHAR2
)
RETURN BOOLEAN
IS
    v_count INT;
BEGIN
SELECT COUNT(*) INTO v_count
FROM DBA_TAB_COLUMNS  -- 或者使用 USER_TAB_COLUMNS，根据权限选择
WHERE OWNER = CURRENT_USER -- 当前用户（在达梦中可能需要使用 OWNER）
  AND TABLE_NAME = UPPER(tname)  -- 达梦通常将元数据以大写存储
  AND COLUMN_NAME = UPPER(cname);  -- 列名也使用大写处理

RETURN (v_count > 0);  -- 如果找到列，返回 TRUE，否则返回 FALSE
END


-- dameng delimiter

DROP PROCEDURE IF EXISTS drop_column_if_exists;

-- dameng delimiter

CREATE OR REPLACE PROCEDURE drop_column_if_exists(
    tname IN VARCHAR2,
    cname IN VARCHAR2
)
IS
    v_count INT;
drop_query VARCHAR2(4000);
BEGIN
-- 检查列是否存在
SELECT COUNT(*) INTO v_count
FROM DBA_TAB_COLUMNS  -- 使用适合的视图，如 USER_TAB_COLUMNS
WHERE OWNER = CURRENT_USER
  AND TABLE_NAME = UPPER(tname)
  AND COLUMN_NAME = UPPER(cname);

IF v_count > 0 THEN
        -- 构造动态 SQL 删除列
        drop_query := 'ALTER TABLE "' || UPPER(tname) || '" DROP COLUMN "' || UPPER(cname) || '"';

        -- 执行动态 SQL
EXECUTE IMMEDIATE drop_query;
END IF;
END;


-- dameng delimiter

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- dameng delimiter

CREATE OR REPLACE PROCEDURE add_column_if_not_exists(
    tname IN VARCHAR2,
    cname IN VARCHAR2,
    columninfo IN VARCHAR2
)
IS
    v_count INT;
add_column_sql VARCHAR2(4000);
comment_sql VARCHAR2(4000);
def_part VARCHAR2(3000);
comment_part VARCHAR2(1000);
comment_pos INT;
BEGIN
-- 判断列是否存在（保留原始大小写）
SELECT COUNT(*) INTO v_count
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = UPPER(tname)
  AND COLUMN_NAME = cname;  -- 注意：不要用 UPPER(cname)，否则误判

IF v_count = 0 THEN
        -- 解析 COMMENT 部分
        comment_pos := INSTR(UPPER(columninfo), ' COMMENT ');
IF comment_pos > 0 THEN
            def_part := TRIM(SUBSTR(columninfo, 1, comment_pos - 1));
comment_part := TRIM(REPLACE(SUBSTR(columninfo, comment_pos + 8), '''', ''));
ELSE
            def_part := columninfo;
comment_part := NULL;
END IF;

        -- 添加列
add_column_sql := 'ALTER TABLE "' || UPPER(tname) || '" ADD ' || def_part;
EXECUTE IMMEDIATE add_column_sql;

-- 添加注释（如果有）
IF comment_part IS NOT NULL THEN
            comment_sql := 'COMMENT ON COLUMN "' || UPPER(tname) || '"."' || cname || '" IS ''' || comment_part || '''';
EXECUTE IMMEDIATE comment_sql;
END IF;
END IF;
END;



-- dameng delimiter

DROP PROCEDURE IF EXISTS drop_index_if_exists;

-- dameng delimiter

CREATE OR REPLACE PROCEDURE drop_index_if_exists(
    p_tablename IN VARCHAR2,
    p_idxname IN VARCHAR2
)
IS
    v_count INT;
drop_index_sql VARCHAR2(4000);
v_owner VARCHAR2(100);
BEGIN
-- 查找当前用户拥有的该索引
SELECT COUNT(*)
INTO v_count
FROM USER_INDEXES
WHERE TABLE_NAME = UPPER(p_tablename)
  AND INDEX_NAME = UPPER(p_idxname);

IF v_count > 0 THEN
        -- 构造 DROP INDEX SQL（USER_INDEXES 无需带 schema）
        drop_index_sql := 'DROP INDEX "' || UPPER(p_idxname) || '"';
EXECUTE IMMEDIATE drop_index_sql;
ELSE
        -- 查找 DBA_INDEXES 中是否存在（非当前用户的索引）
SELECT OWNER INTO v_owner
FROM DBA_INDEXES
WHERE TABLE_NAME = UPPER(p_tablename)
  AND INDEX_NAME = UPPER(p_idxname)
  AND ROWNUM = 1;

-- 拼接带 schema 的 DROP INDEX
drop_index_sql := 'DROP INDEX "' || v_owner || '"."' || UPPER(p_idxname) || '"';
EXECUTE IMMEDIATE drop_index_sql;
END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        NULL; -- 索引不存在，忽略
END;

