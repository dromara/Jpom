package org.dromara.jpom.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;

/**
 * GIt执行基类
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
@Slf4j
public abstract class AbstractGitProcess implements GitProcess {

    private final IWorkspaceEnvPlugin workspaceEnvPlugin;
    protected final Map<String, Object> parameter;

    protected AbstractGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        this.workspaceEnvPlugin = workspaceEnvPlugin;
        this.parameter = decryptParameter(parameter);
    }

    /**
     * 解密参数
     *
     * @param parameter 参数
     */
    protected Map<String, Object> decryptParameter(Map<String, Object> parameter) {
        try {
            parameter.put("password", workspaceEnvPlugin.convertRefEnvValue(parameter, "password"));
            parameter.put("username", workspaceEnvPlugin.convertRefEnvValue(parameter, "username"));
        } catch (Exception e) {
            log.error("解密参数失败", e);
        }
        return parameter;
    }


    /**
     * 获取保存路径
     */
    protected File getSaveFile() {
        return (File) parameter.get("savePath");
    }

    /**
     * 获取分支Name
     */
    protected String getBranchName() {
        return (String) parameter.get("branchName");
    }

    /**
     * 获取TagName
     */
    protected String getTagName() {
        return (String) parameter.get("tagName");
    }
}
