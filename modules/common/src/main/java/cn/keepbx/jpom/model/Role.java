package cn.keepbx.jpom.model;

/**
 * 用户角色
 *
 * @author jiangzeyin
 * @date 2019/4/13
 */
public enum Role {
    /**
     * 系统管理员
     */
    System("系统管理员"),
    /**
     * 服务端管理员
     */
    ServerManager("服务端管理员"),
    /**
     * 节点管理员
     */
    NodeManage("节点管理员"),
    /**
     * 用户
     */
    User("普通成员"),
    ;

    private String desc;

    public String getDesc() {
        return desc;
    }

    Role(String desc) {
        this.desc = desc;
    }
}
