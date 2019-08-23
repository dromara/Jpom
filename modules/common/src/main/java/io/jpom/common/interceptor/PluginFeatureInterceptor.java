package io.jpom.common.interceptor;

import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import io.jpom.plugin.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 插件方法回调拦截器
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
@InterceptorPattens(sort = Integer.MAX_VALUE)
public class PluginFeatureInterceptor extends BaseInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
        HandlerMethod handlerMethod = getHandlerMethod(handler);
        if (handlerMethod == null) {
            return;
        }
        Feature methodAnnotation = handlerMethod.getMethodAnnotation(Feature.class);
        Feature annotation = handlerMethod.getBeanType().getAnnotation(Feature.class);
        if (methodAnnotation != null && annotation != null) {
            if (methodAnnotation.method() != MethodFeature.NULL && annotation.cls() != ClassFeature.NULL) {
                List<FeatureCallback> featureCallbacks = PluginFactory.getFeatureCallbacks();
                featureCallbacks.forEach(featureCallback -> featureCallback.postHandle(request, annotation.cls(), methodAnnotation.method()));
            }
        }
    }
}
