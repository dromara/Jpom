package cn.keepbx.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.controller.base.AbstractController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.PermissionInterceptor;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.data.UserModel;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Objects;

/**
 * base
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
public abstract class BaseController extends AbstractController {
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void resetInfo() {
        USER_MODEL_THREAD_LOCAL.set(getUserModel());
    }

    protected UserModel getUser() {
        UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
        Objects.requireNonNull(userModel);
        return userModel;
    }

    public static void remove() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    public static UserModel getUserModel() {
        return (UserModel) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 获取拦截器中缓存的项目信息
     *
     * @return this
     */
    protected ProjectInfoModel getProjectInfoModel() {
        ProjectInfoModel projectInfoModel = (ProjectInfoModel) this.getRequest().getAttribute(PermissionInterceptor.CACHE_PROJECT_INFO);
        Objects.requireNonNull(projectInfoModel, "获取项目信息失败");
        return projectInfoModel;
    }

    /**
     * 获取当前登录人
     *
     * @return 用户名
     */
    public static String getUserName() {
        UserModel userModel = getUserModel();
        if (userModel == null) {
            return null;
        }
        return userModel.getId();
    }

    /**
     * 路径安全格式化
     *
     * @param path 路径
     * @return 去掉 提权字符串
     */
    protected String pathSafe(String path) {
        if (path == null) {
            return null;
        }
        String newPath = path.replace("../", StrUtil.EMPTY);
        newPath = newPath.replace("..\\", StrUtil.EMPTY);
        newPath = newPath.replace("+", StrUtil.EMPTY);
        return FileUtil.normalize(newPath);
    }

    protected boolean checkPathSafe(String path) {
        if (path == null) {
            return false;
        }
        String newPath = path.replace("../", StrUtil.EMPTY);
        newPath = newPath.replace("..\\", StrUtil.EMPTY);
        newPath = newPath.replace("+", StrUtil.EMPTY);
        return newPath.equals(path);
    }
}
