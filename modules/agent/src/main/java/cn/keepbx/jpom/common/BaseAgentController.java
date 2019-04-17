package cn.keepbx.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Objects;

/**
 * @author jiangzeyin
 * @date 2019/4/17
 */
public abstract class BaseAgentController extends BaseJpomController {

    protected String getUserName() {
        return getUserName(getRequest());
    }

    private static String getUserName(HttpServletRequest request) {
        String name = ServletUtil.getHeaderIgnoreCase(request, "Jpom-Server-UserName");
        name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }

    /**
     * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
     *
     * @return 文件夹
     */
    public static String getTempPathName() {
        File file = getTempPath();
        return FileUtil.normalize(file.getPath());
    }

    /**
     * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
     *
     * @return file
     */
    public static File getTempPath() {
        File file = new File(ConfigBean.getInstance().getDataPath());
        HttpServletRequest request = getRequestAttributes().getRequest();
        String userName = getUserName(request);
        if (StrUtil.isEmpty(userName)) {
            throw new JpomRuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userName);
        FileUtil.mkdir(file);
        return file;
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
