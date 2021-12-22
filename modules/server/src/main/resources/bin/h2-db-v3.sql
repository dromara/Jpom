-- @author bwcx_jzy
-- @Date 2021-12-02
-- add new tables for storage other info...

-- 系统参数名称
CREATE TABLE IF NOT EXISTS PUBLIC.SYSTEM_PARAMETERS
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    `value`          CLOB comment '参数值，JSON 字符串格式',
    description      varchar(255) comment '参数描述',
    CONSTRAINT SYSTEM_PARAMETERS_PK PRIMARY KEY (id)
);
comment on table SYSTEM_PARAMETERS is '系统参数表';

-- 用户信息表
CREATE TABLE IF NOT EXISTS PUBLIC.USER_INFO
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int    DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    parent           VARCHAR(50) not null comment '创建人',
    `name`           VARCHAR(50) comment '昵称',
    systemUser       int    default 0 comment '是否为系统管理员 {1，是，0 否(默认)}',
    password         varchar(100) comment '密码',
    salt             varchar(50) comment '盐值',
    pwdErrorCount    int    default 0 comment '密码错误次数',
    lastPwdErrorTime BIGINT default 0 COMMENT '最后登录失败时间',
    lockTime         BIGINT default 0 comment '锁定时长',
    email            varchar(255) comment '邮箱地址',
    dingDing         varchar(255) comment '钉钉地址',
    workWx           varchar(255) comment '企业微信地址',
    CONSTRAINT USER_INFO_PK PRIMARY KEY (id)
);
comment on table USER_INFO is '用户信息表';

-- 用户的盐值保证唯一
CREATE UNIQUE INDEX IF NOT EXISTS USER_INF_SALT_INDEX1 ON PUBLIC.USER_INFO (salt);

-- 工作空间表
CREATE TABLE IF NOT EXISTS PUBLIC.WORKSPACE
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    `name`           VARCHAR(50) comment '名称',
    description      varchar(255) comment '描述',
    CONSTRAINT WORKSPACE_PK PRIMARY KEY (id)
);
comment on table WORKSPACE is '用户信息表';

-- 用户绑定工作空间表
CREATE TABLE IF NOT EXISTS PUBLIC.USER_BIND_WORKSPACE
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    userId           VARCHAR(50) not null comment '用户ID',
    workspaceId      VARCHAR(50) not null comment '工作空间ID',
    CONSTRAINT USER_BIND_WORKSPACE_PK PRIMARY KEY (id)
);
comment on table USER_BIND_WORKSPACE is '用户工作空间绑定表';

-- 节点列表
CREATE TABLE IF NOT EXISTS PUBLIC.NODE_INFO
(
    id               VARCHAR(50)  not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50)  not null comment '所属工作空间',
    `name`           VARCHAR(50) comment '名称',
    url              varchar(100) not null comment '节点 url IP:PORT',
    loginName        varchar(100) not null comment '节点登录名',
    loginPwd         varchar(100) not null comment '节点密码',
    protocol         varchar(10)  not null comment '协议 http https',
    openStatus       int DEFAULT 0 comment '启用状态{1，启用，0 未启用)}',
    timeOut          int default 0 comment '节点超时时间',
    cycle            int comment '监控周期',
    CONSTRAINT NODE_INFO_PK PRIMARY KEY (id)
);
comment on table NODE_INFO is '节点信息表';

ALTER TABLE NODE_INFO
    ADD IF NOT EXISTS sshId VARCHAR(50) comment '关联的sshid';

-- ssh 信息
CREATE TABLE IF NOT EXISTS PUBLIC.SSH_INFO
(
    id                VARCHAR(50)  not null comment 'id',
    createTimeMillis  BIGINT COMMENT '数据创建时间',
    modifyTimeMillis  BIGINT COMMENT '数据修改时间',
    modifyUser        VARCHAR(50) comment '修改人',
    strike            int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId       varchar(50)  not null comment '所属工作空间',
    `name`            VARCHAR(50) comment '名称',
    host              varchar(100) not null comment 'ssh host IP',
    port              int          not null comment '端口',
    user              varchar(100) not null comment '用户',
    password          varchar(100) comment '密码',
    charset           varchar(100) comment '编码格式',
    fileDirs          CLOB comment '文件目录',
    privateKey        CLOB comment '私钥',
    connectType       varchar(10) comment '连接方式',
    notAllowedCommand CLOB comment '不允许执行的命令',
    allowEditSuffix   CLOB comment '允许编辑的后缀文件',
    CONSTRAINT SSH_INFO_PK PRIMARY KEY (id)
);
comment on table SSH_INFO is 'ssh信息表';

-- 项目信息表
CREATE TABLE IF NOT EXISTS PUBLIC.PROJECT_INFO
(
    id                 VARCHAR(50) not null comment 'id',
    createTimeMillis   BIGINT COMMENT '数据创建时间',
    modifyTimeMillis   BIGINT COMMENT '数据修改时间',
    modifyUser         VARCHAR(50) comment '修改人',
    strike             int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId        varchar(50) not null comment '所属工作空间',
    projectId          varchar(50) not null comment '项目ID',
    nodeId             varchar(50) not null comment '节点ID',
    `name`             VARCHAR(50) not null comment '名称',
    mainClass          varchar(100) comment 'mainClas',
    lib                varchar(100) comment 'lib',
    whitelistDirectory varchar(100) comment 'whitelistDirectory',
    logPath            varchar(100) comment 'logPath',
    jvm                CLOB comment 'jvm',
    args               CLOB comment 'args',
    javaCopyItemList   CLOB comment 'javaCopyItemList',
    token              varchar(255) comment 'token',
    jdkId              VARCHAR(50) comment '名称',
    runMode            varchar(20) comment '连接方式',
    outGivingProject   int DEFAULT 0 comment '分发项目{1，分发，0 独立项目}',
    javaExtDirsCp      CLOB comment 'javaExtDirsCp',
    CONSTRAINT PROJECT_INFO_PK PRIMARY KEY (id)
);
comment on table PROJECT_INFO is '项目信息表';

-- 监控信息
CREATE TABLE IF NOT EXISTS PUBLIC.MONITOR_INFO
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int     DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50) not null comment '所属工作空间',
    `name`           VARCHAR(50) not null comment '名称',
    autoRestart      TINYINT DEFAULT 0 comment '是否自动重启{1，是，0 否}',
    status           TINYINT DEFAULT 0 comment '启用状态{1，启用，0 未启用}',
    alarm            TINYINT DEFAULT 0 comment '报警状态{1，报警中，0 未报警}',
    cycle            int     DEFAULT 0 comment '监控周期',
    notifyUser       CLOB comment '报警联系人',
    projects         CLOB comment '监控的项目',
    CONSTRAINT MONITOR_INFO_PK PRIMARY KEY (id)
);
comment on table MONITOR_INFO is '监控信息';

-- 节点分发信息
CREATE TABLE IF NOT EXISTS PUBLIC.OUT_GIVING
(
    id                       VARCHAR(50) not null comment 'id',
    createTimeMillis         BIGINT COMMENT '数据创建时间',
    modifyTimeMillis         BIGINT COMMENT '数据修改时间',
    modifyUser               VARCHAR(50) comment '修改人',
    strike                   int     DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId              varchar(50) not null comment '所属工作空间',
    `name`                   VARCHAR(50) not null comment '名称',
    afterOpt                 int     DEFAULT 0 comment '分发后的操作',
    clearOld                 TINYINT DEFAULT 0 comment '是否清空旧包发布',
    outGivingProject         TINYINT DEFAULT 0 comment '是否为单独创建的分发项目',
    outGivingNodeProjectList CLOB comment '分发项目信息',
    CONSTRAINT OUT_GIVING_PK PRIMARY KEY (id)
);
comment on table OUT_GIVING is '节点分发信息';

-- 操作监控
CREATE TABLE IF NOT EXISTS PUBLIC.MONITOR_USER_OPT
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int     DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50) not null comment '所属工作空间',
    `name`           VARCHAR(50) not null comment '名称',
    monitorUser      CLOB comment '监控的人',
    status           TINYINT DEFAULT 0 comment '启用状态{1，启用，0 未启用}',
    notifyUser       CLOB comment '报警联系人',
    monitorFeature   CLOB comment '监控的项目',
    monitorOpt       CLOB comment '监控的项目',
    CONSTRAINT MONITOR_USER_OPT_PK PRIMARY KEY (id)
);
comment on table MONITOR_USER_OPT is '监控信息';


-- 工作空间环境变量表
CREATE TABLE IF NOT EXISTS PUBLIC.WORKSPACE_ENV_VAR
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50) not null comment '所属工作空间',
    `name`           VARCHAR(50) comment '名称',
    description      varchar(255) comment '参数描述',
    `value`          CLOB comment '值',
    CONSTRAINT WORKSPACE_ENV_VAR_PK PRIMARY KEY (id)
);
comment on table WORKSPACE_ENV_VAR is '用户信息表';


-- 脚本模版
CREATE TABLE IF NOT EXISTS PUBLIC.SCRIPT_INFO
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50) not null comment '所属工作空间',
    nodeId           varchar(50) not null comment '节点ID',
    scriptId         varchar(50) not null comment '脚本ID',
    `name`           VARCHAR(50) not null comment '名称',
    lastRunUser      varchar(50) comment '最后执行人',
    CONSTRAINT SCRIPT_INFO_PK PRIMARY KEY (id)
);
comment on table SCRIPT_INFO is '脚本模版';


-- 脚本模版-执行记录
CREATE TABLE IF NOT EXISTS PUBLIC.SCRIPT_EXECUTE_LOG
(
    id               VARCHAR(50) not null comment 'id',
    createTimeMillis BIGINT COMMENT '数据创建时间',
    modifyTimeMillis BIGINT COMMENT '数据修改时间',
    modifyUser       VARCHAR(50) comment '修改人',
    strike           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    workspaceId      varchar(50) not null comment '所属工作空间',
    nodeId           varchar(50) not null comment '节点ID',
    scriptId         varchar(50) not null comment '脚本ID',
    CONSTRAINT SCRIPT_EXECUTE_LOG_PK PRIMARY KEY (id)
);
comment on table SCRIPT_EXECUTE_LOG is '脚本模版执行记录';

-- 命令行信息
CREATE TABLE IF NOT EXISTS PUBLIC.COMMAND_MODEL
(
    ID               VARCHAR(50) not null comment 'id',
    CREATETIMEMILLIS BIGINT COMMENT '数据创建时间',
    MODIFYTIMEMILLIS BIGINT COMMENT '数据修改时间',
    `NAME`           VARCHAR(100) comment '命令名称',
    DESC             VARCHAR(500) comment '命令描述',
    COMMAND          CLOB comment '指令内容',
    EXECUTIONROLE    VARCHAR(100) comment '执行用户，默认为root',
    executionPath    VARCHAR(200) comment '执行路径，默认为/root',
    TIMEOUT          int DEFAULT 60 comment '超时时间,单位：秒，默认60',
    TYPE             int DEFAULT 0 comment '命令类型，0-shell，1-powershell',
    PARAMS           CLOB comment '命令参数',
    MODIFYUSER       VARCHAR(50) comment '修改人',
    STRIKE           int DEFAULT 0 comment '逻辑删除{1，删除，0 未删除(默认)}',
    CONSTRAINT COMMAND_MODEL_PK PRIMARY KEY (ID)
);
comment on table COMMAND_MODEL is '命令行信息';


