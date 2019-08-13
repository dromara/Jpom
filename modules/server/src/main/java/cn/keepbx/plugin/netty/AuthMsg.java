package cn.keepbx.plugin.netty;

/**
 * @package cn.keepbx.plugin.netty
 * @Date Created in 2019/8/13 10:45
 * @Author myzf
 */
public class AuthMsg {

    private int type;

    private String userId;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AuthMsg{" +
                "type=" + type +
                ", userId='" + userId + '\'' +
                '}';
    }
}
