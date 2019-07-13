package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.util.SocketSessionUtil;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端socket 基类
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public abstract class BaseAgentWebSocketHandle {

    protected static final ConcurrentHashMap<String, String> USER = new ConcurrentHashMap<>();

    public void addUser(Session session, String name) {
        String optUser = URLUtil.decode(name);
        USER.put(session.getId(), optUser);
    }

    public void onError(Session session, Throwable thr) {
        // java.io.IOException: Broken pipe
        try {
            SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
        } catch (IOException ignored) {
        }
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
    }

    protected String getOptUserName(Session session) {
        String name = USER.get(session.getId());
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }
}
