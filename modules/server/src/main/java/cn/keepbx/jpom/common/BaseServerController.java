package cn.keepbx.jpom.common;

import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.web.context.request.RequestAttributes;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        String nodeId = getParameter("nodeId");
        NodeModel nodeModel = nodeService.getItem(nodeId);
        Objects.requireNonNull(nodeModel);
        return nodeModel;
    }

    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

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
