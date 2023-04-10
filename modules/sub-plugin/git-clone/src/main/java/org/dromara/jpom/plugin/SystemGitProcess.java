package org.dromara.jpom.plugin;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.util.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Hong
* @since 2023/4/10
*/
public class SystemGitProcess extends AbstractGitProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemGitProcess.class);

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
    public Object pull() throws Exception {
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

    private Object pull(Map<String, Object> map, String branchOrTag) throws Exception {
        String url = (String) map.get("url");
        File savePath = handleSaveFile();
        String username = (String) map.getOrDefault("username", "");
        String password = (String) map.getOrDefault("password", "");
        String userPwd = username + ":" + password + "@";
        if (userPwd.equals(":@")) {
            userPwd = "";
        }
        if (map.getOrDefault("protocol", 0).equals(0)) {
            // HTTP or HTTPS
            if (url.startsWith("http://")) {
                url = new StringBuilder(url).insert(7, userPwd).toString();
            } else if (url.startsWith("https://")) {
                url = new StringBuilder(url).insert(8, userPwd).toString();
            }
        }
        if (!existsGit()) {
            // 目录下面无git，重新clone
            String result = processCmd(new String[]{"git", "clone", "-b", branchOrTag, url, savePath.getAbsolutePath()});
            if (result.contains("fatal")) {
                throw new RuntimeException(result);
            }
            LOGGER.info("Clone 代码成功：{}", result);
        } else {
            String result = processCmd(new String[]{"git", "pull", "origin ", branchOrTag + ":" + branchOrTag}, savePath);
            if (result.contains("fatal")) {
                throw new RuntimeException(result);
            }
            LOGGER.info("Pull 代码成功：{}", result);
        }

        // 获取提交日志
        String result = processCmd(new String[]{"git", "log", "-1", branchOrTag}, savePath);
        List<String> list = Arrays.stream(result.split("\n")).filter(it -> StringUtils.hasText(it)).map(it -> it.trim()).collect(Collectors.toList());
        if (!list.isEmpty() && list.get(0).contains("warning")) {
            list = list.subList(1, list.size());
        }
        String commitId = list.get(0).substring(7);
        String desc = branchOrTag;
        String msg = list.get(3);
        String person = list.get(1).substring(7).replace(" ", "")
            .replace("<", "[").replace(">", "]");
        String time = DateUtil.format(new Date(list.get(2).substring(5).trim()), "yyyy-MM-dd HH:mm:ss");
        String parentCount = "1";
        return new String[]{commitId, StrUtil.format("{} {} {} {} {}", desc, msg, person, time, parentCount)};
    }

    /**
     * 处理Git保存目录
     */
    private File handleSaveFile() {
        Map<String, Object> map = getParameter();
        File rsaFile = (File) map.get("rsaFile");
        if (rsaFile.isFile() && rsaFile.exists()) {
            String url = (String) map.get("url");
            url = url.substring(4, url.indexOf(":"));
            setPrivateGitRsa(url, rsaFile);
            debug("使用私有Rsa进行操作Git");
        }
        strictHostKeyChecking();
        File saveFile = getSaveFile();
        if (!saveFile.exists()) {
            saveFile.mkdirs();
        }
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
            debug("{}下无git仓库", savePath.getAbsolutePath());
            return false;
        }
        return true;
    }


    /**
     * 指定私钥
     */
    private boolean setPrivateGitRsa(String host, File rsaFile) {
        String path = System.getProperty("user.home") + "/.ssh";
        File config = new File(path + "/config");
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
        String path = System.getProperty("user.home") + "/.ssh";
        File config = new File(path + "/config");
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
        List<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /**
     * 写入配置文件
     */
    private boolean writeGitConfig(File config, List<String> configs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(config))) {
            for (String s : configs) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
