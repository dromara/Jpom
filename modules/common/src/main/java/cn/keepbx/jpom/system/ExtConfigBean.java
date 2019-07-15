package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.model.system.JpomManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;

/**
 * 外部资源配置
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class ExtConfigBean {
    static final String FILE_NAME = "extConfig.yml";

    private static Resource resource;
    /**
     * 请求日志
     */
    @Value("${consoleLog.reqXss:true}")
    private boolean consoleLogReqXss;
    /**
     * 请求响应
     */
    @Value("${consoleLog.reqResponse:true}")
    private boolean consoleLogReqResponse;

    public boolean isConsoleLogReqResponse() {
        return consoleLogReqResponse;
    }

    public boolean isConsoleLogReqXss() {
        return consoleLogReqXss;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static Resource getResource() {
        if (resource != null) {
            return resource;
        }
        File file = JpomManifest.getRunPath();
        if (file.isFile()) {
            file = file.getParentFile().getParentFile();
            file = new File(file, FILE_NAME);
            if (file.exists() && file.isFile()) {
                resource = new FileSystemResource(file);
                return ExtConfigBean.resource;
            }
        }
        resource = new ClassPathResource("/bin/" + FILE_NAME);
        return ExtConfigBean.resource;
    }

    /**
     * 单例
     *
     * @return this
     */
    public static ExtConfigBean getInstance() {
        return SpringUtil.getBean(ExtConfigBean.class);
    }

    /**
     * 项目运行存储路径
     */
    @Value("${jpom.path}")
    private String path;

    public String getPath() {
        if (StrUtil.isEmpty(path)) {
            if (JpomManifest.getInstance().isDebug()) {
                // 调试模式 为根路径的 jpom文件
                path = ("/jpom/" + BaseJpomApplication.getAppType().name() + "/").toLowerCase();
            } else {
                // 获取当前项目运行路径的父级
                File file = JpomManifest.getRunPath();
                if (!file.exists() && !file.isFile()) {
                    throw new JpomRuntimeException("请配置运行路径属性【jpom.path】");
                }
                path = file.getParentFile().getParentFile().getAbsolutePath();
            }
            DefaultSystemLog.LOG().info("当前数据路径：{}", path);
        }
        return path;
    }

    public String getAbsolutePath() {
        return FileUtil.getAbsolutePath(FileUtil.file(getPath()));
    }
}
