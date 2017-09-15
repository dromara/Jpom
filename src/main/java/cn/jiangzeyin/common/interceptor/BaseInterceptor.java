package cn.jiangzeyin.common.interceptor;

import cn.jiangzeyin.common.base.AbstractBaseControl;
import cn.jiangzeyin.system.log.LogType;
import cn.jiangzeyin.system.log.SystemLog;
import cn.jiangzeyin.util.RequestUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;

/**
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/17.
 */
public abstract class BaseInterceptor extends HandlerInterceptorAdapter {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HttpSession session;
    protected ServletContext application;
    protected String url;
    private AbstractBaseControl abstractBaseControl;
    private volatile static GetUserName getUserName;

    protected static void put(GetUserName getUserName) {
        BaseInterceptor.getUserName = getUserName;
    }

    public static String getNowUserName() {
        if (getUserName == null)
            return null;
        return getUserName.getUserName();
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        this.session = request.getSession();
        this.application = session.getServletContext();
        this.request = request;
        this.response = response;
        this.url = request.getRequestURI();
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Class controlClass = ((HandlerMethod) handler).getBean().getClass();
            Object object = handlerMethod.getBean();
            //
            if (AbstractBaseControl.class.isAssignableFrom(controlClass)) {
                abstractBaseControl = (AbstractBaseControl) object;
                abstractBaseControl.setReqAndRes(this.request, this.session, this.response);
            }
        }
        Map<String, String> header = RequestUtil.getHeaderMapValues(request);
        Map<String, String[]> parameters = request.getParameterMap();
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(request.getRequestURI());
        //.append(",ip:").append(RequestUtil.getIpAddress(request))
        stringBuffer.append(" parameters:");
        if (parameters != null) {
            Set<Map.Entry<String, String[]>> entries = parameters.entrySet();
            stringBuffer.append("{");
            for (Map.Entry<String, String[]> entry : entries) {
                String key = entry.getKey();
                stringBuffer.append(key).append(":");
                String[] value = entry.getValue();
                if (value != null) {
                    for (int i = 0; i < value.length; i++) {
                        if (i != 0)
                            stringBuffer.append(",");
                        stringBuffer.append(value[i]);
                    }
                }
                stringBuffer.append(";");
            }
            stringBuffer.append("}");
        } else {
            stringBuffer.append("null");
        }
        stringBuffer.append(",header:").append(header);
        SystemLog.LOG(LogType.REQUEST).info(stringBuffer.toString());
        return true;
    }

    protected void reload() {
        if (abstractBaseControl != null)
            abstractBaseControl.reLoad();
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (response.getStatus() != HttpStatus.OK.value()) {
            SystemLog.LOG(LogType.CONTROL_ERROR).info("请求错误:" + request.getRequestURI() + "  " + response.getStatus());
        }
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null)
            SystemLog.LOG(LogType.CONTROL_ERROR).error("controller 异常", ex);
    }
}
