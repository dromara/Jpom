package org.dromara.jpom.plugin;

import org.dromara.jpom.util.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Git环境
 * <br>
 * Created By Hong on 2023/3/31
 **/
public class GitEnv {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitEnv.class);
    private static final String OS_NAME = "os.name";
    private static final String WIN_EXISTS_GIT = "where git";
    private static final String LINUX_EXISTS_GIT = "which git";

    /**
     * 操作系统是否有GIT环境
     */
    public static boolean existsSystemGit() {
        String result;
        if (isWin()) {
            result = CommandUtil.execSystemCommand(WIN_EXISTS_GIT);
        } else if (isLinux()) {
            result = CommandUtil.execSystemCommand(LINUX_EXISTS_GIT);
        } else {
            return false;
        }
        if (StringUtils.hasText(result) && (result.contains(".exe") || !result.contains("/usr/bin/which: no"))) {
            LOGGER.info("git安装位置：{}", result);
            return true;
        }
        return false;
    }

    /**
     * 是Windows
     */

    public static boolean isWin() {
        String value = getSystemEnv(OS_NAME);
        if (value.startsWith("Windows")) {
            return true;
        }
        return false;
    }

    /**
     * 是Linux
     */
    public static boolean isLinux() {
        String value = getSystemEnv(OS_NAME);
        if (value.startsWith("Linux") || value.startsWith("LINUX")) {
            return true;
        }
        return false;
    }


    /**
     * 获取系统参数
     *
     * @param key key
     * @return 结果
     */
    private static String getSystemEnv(String key) {
        String value = System.getProperty(key);
        if (!StringUtils.hasText(value)) {
            value = System.getenv(key);
        }
        return value;
    }
}
