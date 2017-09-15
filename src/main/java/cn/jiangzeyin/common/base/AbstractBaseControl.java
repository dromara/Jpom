package cn.jiangzeyin.common.base;

import cn.jiangzeyin.util.FileUtil;
import cn.jiangzeyin.util.RequestUtil;
import cn.jiangzeyin.util.StringUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/1/12.
 */
public abstract class AbstractBaseControl {
    protected HttpServletRequest request;
    protected HttpSession session;
    protected HttpServletResponse response;

    protected String referer;
    protected String ip;
    protected String reqUrl;


    /**
     * 拦截器注入
     *
     * @param request
     * @param session
     * @param response
     */
    public void setReqAndRes(HttpServletRequest request, HttpSession session, HttpServletResponse response) {
        this.request = request;
        this.session = session;
        this.response = response;

        this.referer = StringUtil.convertNULL(this.request.getHeader("Referer"));
        this.ip = RequestUtil.getIpAddress(this.request);
        this.reqUrl = request.getRequestURI();
        this.response.setCharacterEncoding("UTF-8");

    }

    public void reLoad() {

    }

    protected String getUserAgent() {
        return request.getHeader("user-agent");
    }


    protected String convertFilePath(String path) {
        return FileUtil.ClearPath(StringUtil.convertNULL(path).replace("..", ""));
    }

    /**
     * 获取一次性session 字符串
     *
     * @param name
     * @return
     * @author jiangzeyin
     * @date 2016-10-17
     */
    public String getSessionAttributeAfterRemove(String name) {
        Object obj = session.getAttribute(name);
        session.removeAttribute(name);
        if (obj == null)
            return "";
        return obj.toString();
    }


    public String getCookieValue(String name) {
        Cookie cookie = RequestUtil.getCookieByName(request, name);
        if (cookie == null)
            return "";
        return cookie.getValue();

    }

    protected String getParameter(String name) {
        return getParameter(name, null);
    }

    protected String[] getParameters(String name) {
        return request.getParameterValues(name);
    }


    protected String getParameter(String name, String def) {
        String value = request.getParameter(name);
        return value == null ? def : value;
    }

    protected int getParameterInt(String name, int def) {
        return StringUtil.parseInt(request.getParameter(name), def);
    }

    protected int getParameterInt(String name) {
        return getParameterInt(name, 0);
    }
}
