package org.dromara.jpom.plugin;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Hong
* @since 2023/4/10
*/
@Slf4j
public class SystemGitProcess extends AbstractGitProcess {

    protected SystemGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        super(workspaceEnvPlugin, parameter);
    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        File savePath = handleSaveFile();
        Map<String, Object> map = getParameter();
        String url = (String) map.get("url");
        if (!existsGit()) {
            // 初始化本地仓库
            processCmd(new String[]{"git", "init"}, savePath);
            // 添加远程仓库
            processCmd(new String[]{"git", "remote", "add", "origin", url}, savePath);
        }
        String result = processCmd(new String[]{"git", "ls-remote"}, savePath);
        List<String> branchRemote = new ArrayList<>();
        List<String> tagRemote = new ArrayList<>();
        for (String branch : result.split("\n")) {
            if (branch.contains("heads")) {
                branchRemote.add(branch.substring(branch.lastIndexOf("/") + 1));
            } else if (branch.contains("tags")) {
                tagRemote.add(branch.substring(branch.lastIndexOf("/") + 1));
            }
        }
        return new Tuple(branchRemote, tagRemote);
    }

    @Override
    public String[] pull() throws Exception {
        Map<String, Object> map = getParameter();
        String branchName = (String) map.getOrDefault("branchName", "master");
        return pull(map, branchName);
    }

    @Override
    public Object pullByTag() throws Exception {
        Map<String, Object> map = getParameter();
        String branchName = (String) map.getOrDefault("tagName", "master");
        return pull(map, branchName);
    }

    private String[] pull(Map<String, Object> map, String branchOrTag) {
        String url = (String) map.get("url");
        File savePath = handleSaveFile();
        String username = (String) map.getOrDefault("username", "");
        String password = (String) map.getOrDefault("password", "");
        String userPwd = username + ":" + password + "@";
        if (":@".equals(userPwd)) {
            userPwd = "";
        }
        int protocol = (int) map.getOrDefault("protocol", 0);
        if (protocol == 0) {
            // HTTP or HTTPS
            if (url.startsWith("http://")) {
                url = new StringBuilder(url).insert(7, userPwd).toString();
            } else if (url.startsWith("https://")) {
                url = new StringBuilder(url).insert(8, userPwd).toString();
            }
        }
        if (!existsGit()) {
            // 目录下面无git，重新clone
            String result = processCmd("git", "clone", "-b", branchOrTag, url, savePath.getAbsolutePath());
            if (result.contains("fatal")) {
                throw new RuntimeException(result);
            }
            log.info("Clone 代码成功：{}", result);
        } else {
            String result = processCmd(new String[]{"git", "pull", "origin ", branchOrTag + ":" + branchOrTag}, savePath);
            if (result.contains("fatal")) {
                throw new RuntimeException(result);
            }
            log.info("Pull 代码成功：{}", result);
        }

        // 获取提交日志
        String result = processCmd(new String[]{"git", "log", "-1", branchOrTag}, savePath);
        List<String> list = Arrays.stream(result.split("\n")).filter(StringUtils::hasText).map(String::trim).collect(Collectors.toList());
        if (!list.isEmpty() && list.get(0).contains("warning")) {
            list = list.subList(1, list.size());
        }
        String commitId = list.get(0).substring(7);
        String msg = list.get(3);
        String person = list.get(1).substring(7).replace(" ", "")
            .replace("<", "[").replace(">", "]");
        String time = DateUtil.format(DateUtil.parse(list.get(2).substring(5).trim()), DatePattern.NORM_DATETIME_FORMAT);
        String parentCount = "1";
        return new String[]{commitId, StrUtil.format("{} {} {} {} {}", branchOrTag, msg, person, time, parentCount)};
    }

    /**
     * 处理Git保存目录
     */
    private File handleSaveFile() {
        Map<String, Object> map = getParameter();
        File rsaFile = (File) map.get("rsaFile");
        if (FileUtil.isFile(rsaFile)) {
            String url = (String) map.get("url");
            url = url.substring(4, url.indexOf(":"));
            setPrivateGitRsa(url, rsaFile);
            log.debug("使用私有Rsa进行操作Git");
        }
        strictHostKeyChecking();
        File saveFile = getSaveFile();
        FileUtil.mkdir(saveFile);
        return saveFile;
    }

    /**
     * 执行命令
     */
    private String processCmd(String... cmd) {
        return processCmd(cmd, null);
    }

    /**
     * 执行命令
     */
    private String processCmd(String[] cmd, File file) {
        String result = file == null ? CommandUtil.execSystemCommand(String.join(" ", cmd)) : CommandUtil.execSystemCommand(String.join(" ", cmd), file);
        if (!StringUtils.hasText(result)) {
            return "";
        }
        return result;
    }

    /**
     * 是否存在GIT仓库
     */
    private boolean existsGit() {
        File savePath = getSaveFile();
        String result = processCmd(new String[]{"git", "status"}, savePath);
        if (result.startsWith("fatal")) {
            // 目录下面无git，需要拉取代码
            log.debug("{}下无git仓库", savePath.getAbsolutePath());
            return false;
        }
        return true;
    }


    /**
     * 指定私钥
     */
    private boolean setPrivateGitRsa(String host, File rsaFile) {
        File config = FileUtil.file(FileUtil.getUserHomePath(), ".ssh", "config");
        List<String> configs = readGitConfig(config);
        boolean isHost = false;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).contains("Host " + host)) {
                if (configs.get(i + 1).contains("HostName")) {
                    configs.set(i + 1, "\tHostName " + host);
                }
                if (configs.get(i + 2).contains("User")) {
                    configs.set(i + 2, "\tUser git");
                }
                if (configs.get(i + 3).contains("IdentityFile")) {
                    configs.set(i + 3, "\tIdentityFile " + rsaFile.getAbsolutePath());
                }
                if (configs.get(i + 4).contains("IdentitiesOnly")) {
                    configs.set(i + 4, "\tIdentitiesOnly yes");
                }
                isHost = true;
            }
        }
        if (!isHost) {
            configs.add("Host " + host);
            configs.add("\tHostName " + host);
            configs.add("\tUser git");
            configs.add("\tIdentityFile " + rsaFile.getAbsolutePath());
            configs.add("\tIdentitiesOnly yes");
        }
        return writeGitConfig(config, configs);
    }

    /**
     * 设置git不验证host
     */
    private boolean strictHostKeyChecking() {
        File config = FileUtil.file(FileUtil.getUserHomePath(), ".ssh", "config");
        List<String> configs = readGitConfig(config);
        boolean isStrictHostKeyChecking = false, isUserKnownHostsFile = false;
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).contains("StrictHostKeyChecking")) {
                configs.set(i, "StrictHostKeyChecking no");
                isStrictHostKeyChecking = true;
            } else if (configs.get(i).contains("UserKnownHostsFile")) {
                configs.set(i, "UserKnownHostsFile /dev/null");
                isUserKnownHostsFile = true;
            }
        }
        if (!isStrictHostKeyChecking) {
            configs.add("StrictHostKeyChecking no");
        }
        if (!isUserKnownHostsFile) {
            configs.add("UserKnownHostsFile /dev/null");
        }
        return writeGitConfig(config, configs);
    }

    /**
     * 读取Git配置文件
     */
    private List<String> readGitConfig(File config) {
        if (!config.exists()) {
            return new ArrayList<>();
        }
        return FileUtil.readLines(config, Charset.defaultCharset());
    }

    /**
     * 写入配置文件
     */
    private boolean writeGitConfig(File config, List<String> configs) {
        FileUtil.writeLines(configs, config, Charset.defaultCharset());
        return true;
    }
}
