package org.dromara.jpom.util;

/**
 * 工作空间ID
 * <br>
 * Created By Hong on 2023/7/6
 **/
public class WorkspaceThreadLocal {

    private static final ThreadLocal<String> LOCAL = new ThreadLocal<>();

    public static void setWorkspaceId(String workspaceId) {
        LOCAL.set(workspaceId);
    }

    public static String getWorkspaceId() {
        return LOCAL.get();
    }

    public static void clear() {
        LOCAL.remove();
    }
}
