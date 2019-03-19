package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.controller.system.WhitelistDirectoryController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ExtConfigBean;
import cn.keepbx.jpom.system.WebAopLog;
import cn.keepbx.jpom.util.JsonUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查运行命令
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
@PreLoadClass
public class CheckRunCommand {

    /**
     * 检查运行数据
     */
    @PreLoadMethod
    private static void checkData() {
        File file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.USER);
        if (!file.exists()) {
            DefaultSystemLog.LOG().info("创建默认文件：" + file.getPath());
            addDataFile(ConfigBean.USER, file.getPath());
        }

        file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.PROJECT);
        if (!file.exists()) {
            DefaultSystemLog.LOG().info("创建默认文件：" + file.getPath());
            addDataFile(ConfigBean.PROJECT, file.getPath());
        }

        file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.WHITELIST_DIRECTORY);
        if (!file.exists()) {
            DefaultSystemLog.LOG().info("创建默认文件：" + file.getPath());
            addDataFile(ConfigBean.WHITELIST_DIRECTORY, file.getPath());
        }
        repairUpdateWhiteList(file);

        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        DefaultSystemLog.LOG().info("日志存储路径：" + webAopLog.getPropertyValue());
        DefaultSystemLog.LOG().info("项目数据存储路径：" + ConfigBean.getInstance().getPath());
        Resource resource = ExtConfigBean.getResource();
        try {
            DefaultSystemLog.LOG().info("外部配置文件路径：" + resource.getURL());
        } catch (IOException ignored) {
        }
    }

    /**
     * 自动升级同步白名单数据
     *
     * @param file 文件
     */
    private static void repairUpdateWhiteList(File file) {
        // 自动同步项目路径
        try {
            Object object = JsonUtil.readJson(file.getPath());
            WhitelistDirectoryService whitelistDirectoryService = SpringUtil.getBean(WhitelistDirectoryService.class);
            WhitelistDirectoryController whitelistDirectoryController = SpringUtil.getBean(WhitelistDirectoryController.class);
            if (object instanceof JSONArray) {
                DefaultSystemLog.LOG().info("升级白名单目录数据");
                String all = whitelistDirectoryService.convertToLine((JSONArray) object);
                JsonMessage message = whitelistDirectoryController.save(all, null, null);
                DefaultSystemLog.LOG().info(message.toString());
            } else if (object instanceof JSONObject) {
                JSONArray jsonArray = whitelistDirectoryService.getProjectDirectory();
                if (jsonArray == null || jsonArray.isEmpty()) {
                    DefaultSystemLog.LOG().info("升级、自动转换白名单目录数据");
                    ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
                    List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
                    if (projectInfoModels != null && !projectInfoModels.isEmpty()) {
                        List<String> paths = new ArrayList<>();
                        for (ProjectInfoModel projectInfoModel : projectInfoModels) {
                            File file1 = new File(projectInfoModel.getLib());
                            file1 = file1.getParentFile().getParentFile();
                            paths.add(file1.getPath());
                        }
                        JSONArray certificateDirectory = whitelistDirectoryService.getCertificateDirectory();
                        List<String> certificateDirectoryStr = certificateDirectory.toJavaList(String.class);
                        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
                        List<String> ngxDirectoryStr = ngxDirectory.toJavaList(String.class);
                        JsonMessage message = whitelistDirectoryController.save(paths, certificateDirectoryStr, ngxDirectoryStr);
                        DefaultSystemLog.LOG().info(message.toString());
                    }

                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 修护检查
     */
    public static void repairData() {
        checkData();
    }

    private static void addDataFile(String name, String file) {
        URL url = ResourceUtil.getResource("bin/data/" + name);
        String content = FileUtil.readString(url, CharsetUtil.UTF_8);
        FileUtil.writeString(content, file, CharsetUtil.UTF_8);
    }
}
