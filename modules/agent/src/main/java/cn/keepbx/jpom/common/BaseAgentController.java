package cn.keepbx.jpom.common;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.ConfigBean;

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
    public static String getUserName(HttpServletRequest request) {
        String name = ServletUtil.getHeaderIgnoreCase(request, ConfigBean.JPOM_SERVER_USER_NAME);
        name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }

    /**
     * 操作的人员是否为系统管理员
     *
     * @return true
     */
    protected Role getUserRole() {
        String val = ServletUtil.getHeaderIgnoreCase(getRequest(), ConfigBean.JPOM_SERVER_SYSTEM_USER_ROLE);
        try {
            return Role.valueOf(val);
        } catch (Exception e) {
            return Role.User;
        }
    }

    protected boolean isSystemUser() {
        return getUserRole() == Role.System;
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
