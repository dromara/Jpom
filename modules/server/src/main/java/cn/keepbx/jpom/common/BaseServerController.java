package cn.keepbx.jpom.common;

import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.JpomRuntimeException;
import org.springframework.web.context.request.RequestAttributes;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Jpom server 端
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        String nodeId = getParameter("nodeId");
        NodeModel nodeModel = nodeService.getItem(nodeId);
        if (nodeModel == null) {
            throw new JpomRuntimeException("节点信息不正确");
        }
        return nodeModel;
    }

    @Override
    public void resetInfo() {
        USER_MODEL_THREAD_LOCAL.set(getUserModel());
    }

    protected UserModel getUser() {
        UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
        Objects.requireNonNull(userModel);
        return userModel;
    }

    public static void remove() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    public static UserModel getUserModel() {
        return (UserModel) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }
}
