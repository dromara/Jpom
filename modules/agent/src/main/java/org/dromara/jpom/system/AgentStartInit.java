/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateException;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.RemoteVersion;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.script.BaseRunScript;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动备份控制台日志，防止日志文件过大
 *
 * @author bwcx_jzy
 * @since 2019/3/17
 */
@Slf4j
@Configuration
public class AgentStartInit implements ILoadEvent, ISystemTask {

    private static final String ID = "auto_back_log";
    private final ProjectInfoService projectInfoService;
    private final AgentConfig agentConfig;
    private final AgentAuthorize agentAuthorize;
    private final JpomApplication jpomApplication;
    private final ProjectCommander projectCommander;


    public AgentStartInit(ProjectInfoService projectInfoService,
                          AgentConfig agentConfig,
                          AgentAuthorize agentAuthorize,
                          JpomApplication jpomApplication,
                          ProjectCommander projectCommander) {
        this.projectInfoService = projectInfoService;
        this.agentConfig = agentConfig;
        this.agentAuthorize = agentAuthorize;
        this.jpomApplication = jpomApplication;
        this.projectCommander = projectCommander;
    }


    private void startAutoBackLog() {
        AgentConfig.ProjectConfig.LogConfig logConfig = agentConfig.getProject().getLog();
        // 获取cron 表达式
        String cron = Opt.ofBlankAble(logConfig.getAutoBackupConsoleCron()).orElse("0 0/10 * * * ?");
        //
        CronUtils.upsert(ID, cron, () -> {
            try {
                List<NodeProjectInfoModel> list = projectInfoService.list();
                if (list == null) {
                    return;
                }
                //
                list.forEach(this::checkProject);
            } catch (Exception e) {
                log.error("定时备份日志失败", e);
            }
        });
    }

    private void checkProject(NodeProjectInfoModel nodeProjectInfoModel) {
        File file = new File(nodeProjectInfoModel.getLog());
        if (!file.exists()) {
            return;
        }
        AgentConfig.ProjectConfig.LogConfig logConfig = agentConfig.getProject().getLog();
        DataSize autoBackSize = logConfig.getAutoBackupSize();
        autoBackSize = Optional.ofNullable(autoBackSize).orElseGet(() -> DataSize.ofMegabytes(50));
        long len = file.length();
        if (len > autoBackSize.toBytes()) {
            try {
                projectCommander.backLog(nodeProjectInfoModel);
            } catch (Exception e) {
                log.warn("auto back log", e);
            }
        }
        // 清理过期的文件
        File logFile = nodeProjectInfoModel.getLogBack();
        DateTime nowTime = DateTime.now();
        List<File> files = FileUtil.loopFiles(logFile, pathname -> {
            DateTime dateTime = DateUtil.date(pathname.lastModified());
            long days = DateUtil.betweenDay(dateTime, nowTime, false);
            long saveDays = logConfig.getSaveDays();
            return days > saveDays;
        });
        files.forEach(FileUtil::del);
    }

    @Override
    public void executeTask() {
        // 启动加载
        RemoteVersion.loadRemoteInfo();
        // 清空脚本缓存
        BaseRunScript.clearRunScript();
        // 清理临时文件
        File tempPath = agentConfig.getTempPath();
        if (FileUtil.exist(tempPath)) {
            File[] files = tempPath.listFiles((dir, name) -> {
                try {
                    DateTime dateTime = DateUtil.parse(name);
                    long between = DateUtil.between(dateTime, DateTime.now(), DateUnit.DAY);
                    // 保留一天以内的
                    return between > 1;
                } catch (DateException dateException) {
                    return false;
                }
            });
            Optional.ofNullable(files).ifPresent(files1 -> {
                for (File file : files1) {
                    CommandUtil.systemFastDel(file);
                }
            });
        }
    }

    private void autoStartProject() {
        List<NodeProjectInfoModel> list = projectInfoService.list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list = list.stream().filter(nodeProjectInfoModel -> nodeProjectInfoModel.getAutoStart() != null && nodeProjectInfoModel.getAutoStart()).collect(Collectors.toList());
        List<NodeProjectInfoModel> finalList = list;
        ThreadUtil.execute(() -> {
            for (NodeProjectInfoModel nodeProjectInfoModel : finalList) {
                try {
                    if (!projectCommander.isRun(nodeProjectInfoModel)) {
                        projectCommander.execCommand(ConsoleCommandOp.start, nodeProjectInfoModel);
                    }
                } catch (Exception e) {
                    log.warn("自动启动项目失败：{} {}", nodeProjectInfoModel.getId(), e.getMessage());
                }
            }
        });
    }


    /**
     * 自动推送插件端信息到服务端
     *
     * @param url 服务端url
     */
    public void autoPushToServer(String url) {
        url = StrUtil.removeSuffix(url, CharPool.SINGLE_QUOTE + "");
        url = StrUtil.removePrefix(url, CharPool.SINGLE_QUOTE + "");
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url);
        String networkName = (String) urlBuilder.getQuery().get("networkName");
        //
        LinkedHashSet<InetAddress> localAddressList = NetUtil.localAddressList(networkInterface -> StrUtil.isEmpty(networkName) || StrUtil.equals(networkName, networkInterface.getName()), address -> {
            // 非loopback地址，指127.*.*.*的地址
            return !address.isLoopbackAddress()
                // 需为IPV4地址
                && address instanceof Inet4Address;
        });
        if (StrUtil.isNotEmpty(networkName) && CollUtil.isEmpty(localAddressList)) {
            log.warn("No usable IP found by NIC name,{}", networkName);
        }
        Set<String> ips = localAddressList.stream().map(InetAddress::getHostAddress).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        urlBuilder.addQuery("ips", CollUtil.join(ips, StrUtil.COMMA));
        urlBuilder.addQuery("loginName", agentAuthorize.getAgentName());
        urlBuilder.addQuery("loginPwd", agentAuthorize.getAgentPwd());
        int port = jpomApplication.getPort();
        urlBuilder.addQuery("port", port + "");
        //
        String build = urlBuilder.build();
        try (HttpResponse execute = HttpUtil.createGet(build, true).execute()) {
            String body = execute.body();
            log.info("push result:" + body);
        }
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        this.startAutoBackLog();
        this.autoStartProject();
    }
}
