package io.jpom.controller.tomcat;

/**
 * tomcat操作
 *
 * @author bwcx_jzy
 * @date 2019/7/22
 **/
public enum TomcatOp {
    /**
     * 启动
     */
    start,
    stop,
    reload,
    /**
     * 删除发布
     */
    undeploy
}
