package io.jpom.plugin;

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
     * @param request       请求对象
     * @param classFeature  类方法
     * @param methodFeature 方法
     * @param pars          辅助参数
     */
    void postHandle(HttpServletRequest request,
                    ClassFeature classFeature,
                    MethodFeature methodFeature,
                    Object... pars);
}
