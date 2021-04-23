package io.jpom.socket;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public enum HandlerType {
    /**
     * 脚本模板
     */
    script,
    /**
     * tomcat
     */
    tomcat,
    /**
     * 项目控制台和首页监控
     */
    console,
    /**
     * ssh
     */
    ssh,
    /**
     * 节点升级
     */
    nodeUpdate
}
