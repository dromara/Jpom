package cn.keepbx.plugin;

/**
 * 功能模块
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public enum ClassFeature {
    /**
     * 没有
     */
    NULL(""),
    /**
     * ssh
     */
    SSH("SSH管理"),
    NODE("节点管理"),
    OUTGIVING("分发管理"),
    MONITOR("监控管理"),
    BUILD("在线构建"),
    ;

    private String name;

    public String getName() {
        return name;
    }

    ClassFeature(String name) {
        this.name = name;
    }
}
