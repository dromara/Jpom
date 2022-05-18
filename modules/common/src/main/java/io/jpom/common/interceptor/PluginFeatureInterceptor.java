/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
//package io.jpom.common.interceptor;
//
//import cn.jiangzeyin.common.interceptor.BaseInterceptor;
//import cn.jiangzeyin.common.interceptor.InterceptorPattens;
//import io.jpom.plugin.*;
//import org.springframework.web.method.HandlerMethod;
//import org.springframework.web.servlet.ModelAndView;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.util.List;
//
///**
// * 插件方法回调拦截器
// *
// * @author bwcx_jzy
// * @since 2019/8/13
// */
//@InterceptorPattens(sort = Integer.MAX_VALUE)
//public class PluginFeatureInterceptor extends BaseInterceptor {
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        super.postHandle(request, response, handler, modelAndView);
//        HandlerMethod handlerMethod = getHandlerMethod(handler);
//        if (handlerMethod == null) {
//            return;
//        }
//        Feature methodAnnotation = handlerMethod.getMethodAnnotation(Feature.class);
//        Feature annotation = handlerMethod.getBeanType().getAnnotation(Feature.class);
//        if (methodAnnotation != null && annotation != null) {
//            if (methodAnnotation.method() != MethodFeature.NULL && annotation.cls() != ClassFeature.NULL) {
//                List<FeatureCallback> featureCallbacks = PluginFactory.getFeatureCallbacks();
//                featureCallbacks.forEach(featureCallback -> featureCallback.postHandle(request, annotation.cls(), methodAnnotation.method()));
//            }
//        }
//    }
//}
