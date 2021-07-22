package io.jpom.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author bwcx_jzy
 * @date 2019/9/4
 */
@InterceptorPattens(value = "/api/**")
public class OpenApiInterceptor extends BaseInterceptor {

    @Override
    protected boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

        NotLogin methodAnnotation = handlerMethod.getMethodAnnotation(NotLogin.class);
        if (methodAnnotation == null) {
            if (handlerMethod.getBeanType().isAnnotationPresent(NotLogin.class)) {
                return true;
            }
        } else {
            return true;
        }
        return checkOpenApi(request, response);
    }

    private boolean checkOpenApi(HttpServletRequest request, HttpServletResponse response) {
        String header = request.getHeader(ServerOpenApi.HEAD);
        if (StrUtil.isEmpty(header)) {
            ServletUtil.write(response, JsonMessage.getString(300, "token empty"), MediaType.APPLICATION_JSON_UTF8_VALUE);
            return false;
        }
        String authorizeToken = ServerExtConfigBean.getInstance().getAuthorizeToken();
        if (StrUtil.isEmpty(authorizeToken)) {
            ServletUtil.write(response, JsonMessage.getString(300, "not config token"), MediaType.APPLICATION_JSON_UTF8_VALUE);
            return false;
        }
        String md5 = SecureUtil.md5(authorizeToken);
        md5 = SecureUtil.sha1(md5 + ServerOpenApi.HEAD);
        if (!StrUtil.equals(header, md5)) {
            ServletUtil.write(response, JsonMessage.getString(300, "not config token"), MediaType.APPLICATION_JSON_UTF8_VALUE);
            return false;
        }
        return true;
    }
}
