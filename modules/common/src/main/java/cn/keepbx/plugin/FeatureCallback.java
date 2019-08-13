package cn.keepbx.plugin;

import javax.servlet.http.HttpServletRequest;

/**
 * 功能回调
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public interface FeatureCallback {

    /**
     * 方法执行
     *
     * @param request
     * @param classFeature
     * @param methodFeature
     * @param pars
     */
    void postHandle(HttpServletRequest request,
                    ClassFeature classFeature,
                    MethodFeature methodFeature,
                    Object... pars);
}
