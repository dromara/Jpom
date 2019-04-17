//package cn.keepbx.jpom.common.interceptor;
//
//import cn.jiangzeyin.common.interceptor.BaseInterceptor;
//import cn.jiangzeyin.common.interceptor.InterceptorPattens;
//import cn.jiangzeyin.common.spring.SpringUtil;
//import cn.keepbx.jpom.service.manage.ProjectInfoService;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
///**
// * @author jiangzeyin
// * @date 2019/4/17
// */
//@InterceptorPattens()
//public class ProjectInterceptor extends BaseInterceptor {
//
//    private ProjectInfoService projectInfoService;
//
//    public static final String CACHE_PROJECT_INFO = "cache_project_info";
//
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        if (projectInfoService == null) {
//            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
//        }
//        super.preHandle(request, response, handler);
////        if (handler instanceof HandlerMethod) {
////            HandlerMethod handlerMethod = (HandlerMethod) handler;
////        }
//        String val = request.getParameter("id");
//    }
//}
