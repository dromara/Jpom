package cn.keepbx.jpom.common;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author jiangzeyin
 * @date 2019/4/17
 */
public abstract class BaseAgentController extends BaseJpomController {

    protected String getUserName() {
        return getUserName(getRequest());
    }

    public static String getUserName(HttpServletRequest request) {
        String name = ServletUtil.getHeaderIgnoreCase(request, "Jpom-Server-UserName");
        name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }


    /**
     * 获取拦截器中缓存的项目信息
     *
     * @return this
     */
    protected ProjectInfoModel getProjectInfoModel() {
        ProjectInfoModel projectInfoModel = null;
        String id = getParameter("id");
        if (StrUtil.isNotEmpty(id)) {
            ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
            projectInfoModel = projectInfoService.getItem(id);
        }
        Objects.requireNonNull(projectInfoModel, "获取项目信息失败");
        return projectInfoModel;
    }
}
