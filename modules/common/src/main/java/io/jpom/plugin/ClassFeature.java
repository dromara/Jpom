package io.jpom.plugin;

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
    USER("用户管理"),
    SYSTEM("系统管理"),
    USER_ROLE("用户角色"),
    NGINX("Nginx"),
    SSL("ssl证书"),
    //******************************************     节点管理功能
    PROJECT("项目管理", ClassFeature.NODE),
    PROJECT_RECOVER("项目回收", ClassFeature.NODE),
    SCRIPT("脚本模板", ClassFeature.NODE),
    TOMCAT("Tomcat", ClassFeature.NODE),
    ;

    private final String name;

    private ClassFeature parent;

    public String getName() {
        return name;
    }

    public ClassFeature getParent() {
        return parent;
    }

    ClassFeature(String name) {
        this.name = name;
    }

    ClassFeature(String name, ClassFeature parent) {
        this.name = name;
        this.parent = parent;
    }
}
