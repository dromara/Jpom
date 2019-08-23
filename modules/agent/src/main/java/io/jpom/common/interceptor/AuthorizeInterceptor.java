package io.jpom.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 授权拦截
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@InterceptorPattens()
public class AuthorizeInterceptor extends BaseInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        super.preHandle(request, response, handler);
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            NotAuthorize notAuthorize = handlerMethod.getMethodAnnotation(NotAuthorize.class);
            if (notAuthorize == null) {
                String authorize = ServletUtil.getHeaderIgnoreCase(request, ConfigBean.JPOM_AGENT_AUTHORIZE);
                if (StrUtil.isEmpty(authorize)) {
                    this.error(response);
                    return false;
                }
                if (!AgentAuthorize.getInstance().checkAuthorize(authorize)) {
                    this.error(response);
                    return false;
                }
            }
        }
        return true;
    }

    private void error(HttpServletResponse response) {
        ServletUtil.write(response, JsonMessage.getString(ConfigBean.AUTHORIZE_ERROR, "授权信息错误"), MediaType.APPLICATION_JSON_UTF8_VALUE);
    }
}
