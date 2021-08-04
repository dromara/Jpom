-- @author Hotstrip
-- @Date 2021-07-31
-- add new tables for storage build info, repo info...

-- 仓库信息
CREATE TABLE IF NOT EXISTS PUBLIC.REPOSITORY
(
    ID           VARCHAR(50) not null comment 'id',
    `NAME`       VARCHAR(50) comment '仓库名称',
    GIT_URL      varchar(255) comment '仓库地址',
    REPO_TYPE    int comment '仓库类型{0: GIT, 1: SVN}',
    PROTOCOL     int comment '拉取代码的协议{0: http, 1: ssh}',
    USER_NAME    VARCHAR(50) comment '登录用户',
    PASSWORD     VARCHAR(50) comment '登录密码',
    RSA_PUB      VARCHAR(2048) comment 'SSH RSA 公钥',
    MODIFY_TIME  datetime comment '修改时间',
    CONSTRAINT REPOSITORY_PK PRIMARY KEY (ID)
);
comment on table REPOSITORY is '仓库信息';

-- 构建信息
CREATE TABLE IF NOT EXISTS PUBLIC.BUILD_INFO
(
    ID           VARCHAR(50) not null comment 'id',
    `NAME`       VARCHAR(50) comment '构建名称',
    BUILD_ID     int comment '构建 ID',
    `GROUP`      VARCHAR(50) comment '分组名称',
    BRANCH_NANME VARCHAR(50) comment '分支',
    SCRIPT       VARCHAR(200) comment '构建命令',
    RESULT_DIR_FILE VARCHAR(50) comment '构建产物目录',
    RELEASE_METHOD int comment '发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}',
    EXTRA_DATA   VARCHAR(200) comment '额外信息，JSON 字符串格式',
    CONSTRAINT BUILD_INFO_PK PRIMARY KEY (ID)
);
comment on table BUILD_INFO is '构建信息';
