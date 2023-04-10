package org.dromara.jpom.plugin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.util.CommandUtil;

/**
 * Git环境
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
@Slf4j
public class GitEnv {

    private static final String WIN_EXISTS_GIT = "where git";
    private static final String LINUX_EXISTS_GIT = "which git";

    /**
     * 操作系统是否有GIT环境
     */
    public static boolean existsSystemGit() {
        String result;
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            result = CommandUtil.execSystemCommand(WIN_EXISTS_GIT);
            if (StrUtil.contains(result, ".exe")) {
                log.info("git安装位置：{}", result);
                return true;
            }
        } else if (osInfo.isLinux() || osInfo.isMac()) {
            result = CommandUtil.execSystemCommand(LINUX_EXISTS_GIT);
            if (StrUtil.containsAny(result, "no git", "not found")) {
                return false;
            }
            log.info("git安装位置：{}", result);
        } else {
            log.warn("不支持的系统类型：{}", osInfo.getName());
            return false;
        }
        return false;
    }
}
