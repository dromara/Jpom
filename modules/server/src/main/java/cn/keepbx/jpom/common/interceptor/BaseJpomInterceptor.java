package cn.keepbx.jpom.common.interceptor;

import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
abstract class BaseJpomInterceptor extends BaseInterceptor {

    boolean isPage(HandlerMethod handlerMethod) {
        ResponseBody responseBody = handlerMethod.getMethodAnnotation(ResponseBody.class);
        if (responseBody == null) {
            RestController restController = handlerMethod.getBeanType().getAnnotation(RestController.class);
            return restController == null;
        }
        return false;
    }
}
