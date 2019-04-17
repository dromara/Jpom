package cn.keepbx.jpom.common.interceptor;

import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 节点拦截器
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@InterceptorPattens(sort = 1)
public class NodeInterceptor extends BaseJpomInterceptor {
    private NodeService nodeService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        super.preHandle(request, response, handler);
        UserModel userModel = BaseController.getUserModel();
        if (handler instanceof HandlerMethod && userModel != null) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            if (isPage(handlerMethod)) {
                String id = request.getParameter("nodeId");
                NodeModel nodeModel = nodeService.getItem(id);
                request.setAttribute("nowNodeItem", nodeModel);
            }
        }
        return true;
    }
}
