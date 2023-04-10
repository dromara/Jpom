package org.dromara.jpom.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * GIt执行基类
 * <br>
 * Created By Hong on 2023/3/31
 **/
public abstract class AbstractGitProcess implements GitProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGitProcess.class);

    private final IWorkspaceEnvPlugin workspaceEnvPlugin;
    private final Map<String, Object> parameter;

    protected AbstractGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        this.workspaceEnvPlugin = workspaceEnvPlugin;
        this.parameter = decryptParameter(parameter);
    }

    /**
     * 解密参数
     * @param parameter 参数
     */
    protected Map<String, Object> decryptParameter(Map<String, Object> parameter) {
        try {
            parameter.put("password", workspaceEnvPlugin.convertRefEnvValue(parameter, "password"));
            parameter.put("username", workspaceEnvPlugin.convertRefEnvValue(parameter, "username"));
        } catch (Exception e) {
            LOGGER.error("解密参数失败", e);
        }
        return parameter;
    }

    protected Map<String, Object> getParameter() {
        return Collections.unmodifiableMap(this.parameter);
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
        return (String) parameter.getOrDefault("branchName", "master");
    }

    /**
     * 获取TagName
     */
    protected String getTagName() {
        return (String) parameter.get("tagName");
    }

    protected void debug(String msg, Object... val) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg, val);
        }
    }
}
