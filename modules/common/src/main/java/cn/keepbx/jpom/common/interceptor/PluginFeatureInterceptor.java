package cn.keepbx.jpom.common.interceptor;

import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import cn.keepbx.plugin.PluginFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 插件方法回调拦截器
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public class PluginFeatureInterceptor extends BaseInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
        HandlerMethod handlerMethod = getHandlerMethod();
        Feature methodAnnotation = handlerMethod.getMethodAnnotation(Feature.class);
        Feature annotation = handlerMethod.getBeanType().getAnnotation(Feature.class);
        if (methodAnnotation != null && annotation != null) {
            if (methodAnnotation.method() != MethodFeature.NULL && annotation.cls() != ClassFeature.NULL) {
                PluginFactory.getFeatureCallback().postHandle(request, annotation.cls(), methodAnnotation.method());
            }
        }
    }
}
