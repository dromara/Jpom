package io.jpom.common;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.controller.base.AbstractController;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.system.ConfigBean;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * agent 端
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
public abstract class BaseAgentController extends BaseJpomController {
    @Resource
    protected ProjectInfoService projectInfoService;

    protected String getUserName() {
        return getUserName(getRequest());
    }

    /**
     * 获取server 端操作人
     *
     * @param request req
     * @return name
     */
    private static String getUserName(HttpServletRequest request) {
        String name = ServletUtil.getHeaderIgnoreCase(request, ConfigBean.JPOM_SERVER_USER_NAME);
        name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
        name = StrUtil.emptyToDefault(name, StrUtil.DASHED);
        return URLUtil.decode(name, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 获取server 端操作人
     *
     * @return name
     */
    public static String getNowUserName() {
        ServletRequestAttributes servletRequestAttributes = AbstractController.tryGetRequestAttributes();
        if (servletRequestAttributes == null) {
            return StrUtil.DASHED;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        return getUserName(request);
    }

    /**
     * 获取拦截器中缓存的项目信息
     *
     * @return this
     */
    protected ProjectInfoModel getProjectInfoModel() {
        ProjectInfoModel projectInfoModel = tryGetProjectInfoModel();
        Objects.requireNonNull(projectInfoModel, "获取项目信息失败");
        return projectInfoModel;
    }

    protected ProjectInfoModel tryGetProjectInfoModel() {
        ProjectInfoModel projectInfoModel = null;
        String id = getParameter("id");
        if (StrUtil.isNotEmpty(id)) {
            projectInfoModel = projectInfoService.getItem(id);
        }
        return projectInfoModel;
    }
}
