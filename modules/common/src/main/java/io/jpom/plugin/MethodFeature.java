package io.jpom.plugin;

/**
 * 功能方法
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public enum MethodFeature {
    /**
     * 没有
     */
    NULL(""),
    /**
     * 文件管理
     */
    FILE("文件管理"),
    EDIT("修改"),
    DEL("删除"),
    INSTALL("安装"),
    LIST("列表"),
    TERMINAL("终端"),
    DOWNLOAD("下载"),
    LOG("日志"),
    UPLOAD("上传"),
    //    WHITELIST("白名单"),
    EXECUTE("执行"),
    DEL_FILE("删除文件"),
    CACHE("缓存"),
    DEL_LOG("删除日志"),
    CONFIG("配置"),
    READ_FILE("读取文件"),
    GET_FILE_FOMAT("获取在线编辑文件格式"),
    UPDATE_CONFIG_FILE("更新文件"),
    ;

    private String name;

    public String getName() {
        return name;
    }

    MethodFeature(String name) {
        this.name = name;
    }
}
