package io.jpom.common;

/**
 * @author bwcx_jzy
 * @since 2022/8/30
 */
public class ServerConst extends Const {

    /**
     * String const
     */
    public static final String ID_STR = "id";

    public static final String GROUP_STR = "group";

    /**
     * 工作空间全局
     */
    public static final String WORKSPACE_GLOBAL = "GLOBAL";

    /**
     * SQL backup default directory name
     * 数据库备份默认目录名称
     */
    public static final String BACKUP_DIRECTORY_NAME = "backup";
    /**
     * h2 数据库表名字段
     */
    public static final String TABLE_NAME = "TABLE_NAME";
    /**
     * 备份 SQL 文件 后缀
     */
    public static final String SQL_FILE_SUFFIX = ".sql";

    /**
     * String get
     */
    public static final String GET_STR = "get";

    /**
     * id_rsa
     */
    public static final String ID_RSA = "_id_rsa";
    /**
     * sshkey
     */
    public static final String SSH_KEY = "sshkey";
    /**
     * 引用工作空间环境变量的前缀
     */
    public static final String REF_WORKSPACE_ENV = "$ref.wEnv.";
}
