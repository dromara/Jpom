-- @author Hotstrip
-- @Date 2021-07-31
-- add new tables for storage build info, repo info...

-- 仓库信息
CREATE TABLE IF NOT EXISTS PUBLIC.REPOSITORY
(
    ID           VARCHAR(50) not null comment 'id',
    `NAME`       VARCHAR(50) comment '仓库名称',
    CONSTRAINT REPOSITORY_PK PRIMARY KEY (ID)
);
comment on table REPOSITORY is '仓库信息';

-- 构建信息
CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
    ID           VARCHAR(50) not null comment 'id',
    `NAME`       VARCHAR(50) comment '构建名称',
    BUILD_ID     int comment '构建 ID',
    CONSTRAINT BUILD_INFO_PK PRIMARY KEY (ID)
);
comment on table BUILD_INFO is '构建信息';
