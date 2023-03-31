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
package org.dromara.jpom.func.assets.server;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.Type;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.cron.IAsyncLoad;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.plugin.IWorkspaceEnvPlugin;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.JschUtils;
import org.dromara.jpom.util.StringUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@Service
@Slf4j
public class MachineSshServer extends BaseDbService<MachineSshModel> implements ILoadEvent, IAsyncLoad, Task {
    private static final String CRON_ID = "ssh-monitor";
    @Resource
    @Lazy
    private SshService sshService;

    private final JpomApplication jpomApplication;

    public MachineSshServer(JpomApplication jpomApplication) {
        this.jpomApplication = jpomApplication;
    }

    @Override
    protected void fillInsert(MachineSshModel machineSshModel) {
        super.fillInsert(machineSshModel);
        machineSshModel.setGroupName(StrUtil.emptyToDefault(machineSshModel.getGroupName(), Const.DEFAULT_GROUP_NAME));
        machineSshModel.setStatus(ObjectUtil.defaultIfNull(machineSshModel.getStatus(), 0));
    }

    @Override
    protected void fillSelectResult(MachineSshModel data) {
        if (data == null) {
            return;
        }
        if (!StrUtil.startWithIgnoreCase(data.getPassword(), ServerConst.REF_WORKSPACE_ENV)) {
            // 隐藏密码字段
            data.setPassword(null);
        }
        //data.setPassword(null);
        data.setPrivateKey(null);
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug("机器 SSH 表已经存在 {} 条数据，不需要修复机器 SSH 数据", count);
            return;
        }
        List<SshModel> list = sshService.list(false);
        if (CollUtil.isEmpty(list)) {
            log.debug("没有任何ssh信息,不需要修复机器 SSH 数据");
            return;
        }
        Map<String, List<SshModel>> sshMap = CollStreamUtil.groupByKey(list, sshModel -> StrUtil.format("{} {} {} {}", sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getConnectType()));
        List<MachineSshModel> machineSshModels = new ArrayList<>(sshMap.size());
        for (Map.Entry<String, List<SshModel>> entry : sshMap.entrySet()) {
            List<SshModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            SshModel first = CollUtil.getFirst(value);
            if (value.size() > 1) {
                log.warn("SSH 地址 {} 存在多个数据，将自动合并使用 {} SSH的配置信息", entry.getKey(), first.getName());
            }
            machineSshModels.add(this.sshInfoToMachineSsh(first));
        }
        this.insert(machineSshModels);
        log.info("成功修复 {} 条机器 SSH 数据", machineSshModels.size());
        // 更新 ssh 的机器id
        for (MachineSshModel value : machineSshModels) {
            Entity entity = Entity.create();
            entity.set("machineSshId", value.getId());
            Entity where = Entity.create();
            where.set("host", value.getHost());
            where.set("port", value.getPort());
            // 关键词，如果不加 ` 会查询不出结果
            where.set("`user`", value.getUser());
            where.set("connectType", value.getConnectType());
            int update = sshService.update(entity, where);
            Assert.state(update > 0, "更新 SSH 表机器id 失败：" + value.getName());
        }
    }

    private MachineSshModel sshInfoToMachineSsh(SshModel sshModel) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setName(sshModel.getName());
        machineSshModel.setGroupName(sshModel.getGroup());
        machineSshModel.setHost(sshModel.getHost());
        machineSshModel.setPort(sshModel.getPort());
        machineSshModel.setUser(sshModel.getUser());
        machineSshModel.setCharset(sshModel.getCharset());
        machineSshModel.setTimeout(sshModel.getTimeout());
        machineSshModel.setPrivateKey(sshModel.getPrivateKey());
        machineSshModel.setPassword(sshModel.getPassword());
        machineSshModel.setConnectType(sshModel.getConnectType());
        machineSshModel.setCreateTimeMillis(sshModel.getCreateTimeMillis());
        machineSshModel.setModifyTimeMillis(sshModel.getModifyTimeMillis());
        machineSshModel.setModifyUser(sshModel.getModifyUser());
        return machineSshModel;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void startLoad() {
        CronUtils.add(CRON_ID, "0 0/1 * * * ?", () -> MachineSshServer.this);
    }

    @Override
    public void execute() {
        List<MachineSshModel> list = this.list(false);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        this.checkList(list);
    }

    private void checkList(List<MachineSshModel> monitorModels) {
        monitorModels.forEach(monitorModel -> ThreadUtil.execute(() -> this.updateMonitor(monitorModel)));
    }

    private void updateMonitor(MachineSshModel machineSshModel) {

        Session session = null;
        try {
            InputStream sshExecTemplateInputStream = ExtConfigBean.getConfigResourceInputStream("/ssh/monitor-script.sh");
            String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
            Map<String, String> map = new HashMap<>(10);
            map.put("JPOM_AGENT_PID_TAG", Type.Agent.getTag());
            sshExecTemplate = StringUtil.formatStrByMap(sshExecTemplate, map);
            Charset charset = machineSshModel.charset();
            //
            session = this.getSessionByModelNoFill(machineSshModel);
            int timeout = machineSshModel.timeout();
            List<String> listStr = new ArrayList<>();
            List<String> error = new ArrayList<>();
            JschUtils.execCallbackLine(session, charset, timeout, sshExecTemplate, StrUtil.EMPTY, listStr::add, error::add);
            this.updateMonitorInfo(machineSshModel, listStr, error);
        } catch (Exception e) {
            String message = e.getMessage();
            if (StrUtil.containsIgnoreCase(message, "timeout")) {
                log.error("监控 ssh[{}] 超时 {}", machineSshModel.getName(), message);
            } else {
                log.error("监控 ssh[{}] 异常", machineSshModel.getName(), e);
            }
            this.updateStatus(machineSshModel.getId(), 0, message);
        } finally {
            JschUtil.close(session);
        }
    }

    private void updateMonitorInfo(MachineSshModel machineSshModel, List<String> listStr, List<String> errorList) {
        String error = CollUtil.join(errorList, StrUtil.LF);
        if (StrUtil.isNotEmpty(error)) {
            log.error("{} ssh 监控执行存在异常信息：{}", machineSshModel.getName(), error);
        }
        if (log.isDebugEnabled()) {
            log.debug("{} ssh 监控信息结果：{} {}", machineSshModel.getName(), CollUtil.join(listStr, StrUtil.LF), error);
        }
        if (CollUtil.isEmpty(listStr)) {
            this.updateStatus(machineSshModel.getId(), 1, "执行结果为空," + error);
            return;
        }
        Map<String, List<String>> map = new CaseInsensitiveMap<>(listStr.size());
        for (String strItem : listStr) {
            String key = StrUtil.subBefore(strItem, StrUtil.COLON, false);
            List<String> list = map.computeIfAbsent(key, s2 -> new ArrayList<>());
            list.add(StrUtil.subAfter(strItem, StrUtil.COLON, false));
        }
        MachineSshModel update = new MachineSshModel();
        update.setId(machineSshModel.getId());
        update.setStatus(1);
        update.setOsName(this.getFirstValue(map, "os name"));
        update.setOsVersion(this.getFirstValue(map, "os version"));
        update.setOsLoadAverage(CollUtil.join(map.get("load average"), StrUtil.COMMA));
        String uptime = this.getFirstValue(map, "uptime");
        if (StrUtil.isNotEmpty(uptime)) {
            try {
                // 可能有时区问题
                DateTime dateTime = DateUtil.parse(uptime);
                update.setOsSystemUptime((SystemClock.now() - dateTime.getTime()));
            } catch (Exception e) {
                error = error + " 解析系统启动时间错误：" + e.getMessage();
                update.setOsSystemUptime(0L);
            }
        }
        update.setOsCpuCores(Convert.toInt(this.getFirstValue(map, "cpu core"), 0));
        update.setHostName(this.getFirstValue(map, "hostname"));
        update.setOsCpuIdentifierName(this.getFirstValue(map, "model name"));
        // kb
        Long memoryTotal = Convert.toLong(this.getFirstValue(map, "memory total"), 0L);
        Long memoryUsed = Convert.toLong(this.getFirstValue(map, "memory used"), 0L);
        update.setOsMoneyTotal(memoryTotal * 1024);
        error = Opt.ofBlankAble(error).map(s -> ",错误信息：" + s).orElse(StrUtil.EMPTY);
        update.setStatusMsg("执行成功" + error);
        update.setOsOccupyCpu(Convert.toDouble(this.getFirstValue(map, "cpu usage"), -0D));
        if (memoryTotal > 0) {
            update.setOsOccupyMemory(NumberUtil.div(memoryUsed, memoryTotal, 2).doubleValue());
        } else {
            update.setOsOccupyMemory(-0D);
        }
        List<String> list = map.get("disk info");
        update.setOsMaxOccupyDisk(-0D);
        update.setOsMaxOccupyDiskName(StrUtil.EMPTY);
        if (CollUtil.isNotEmpty(list)) {
            long total = 0;
            for (String s : list) {
                List<String> trim = StrUtil.splitTrim(s, StrUtil.COLON);
                long total1 = Convert.toLong(CollUtil.get(trim, 1), 0L);
                total += total1;
                long used = Convert.toLong(CollUtil.get(trim, 2), 0L);
                // 计算最大的硬盘占用
                if (total1 > 0) {
                    Double osMaxOccupyDisk = update.getOsMaxOccupyDisk();
                    osMaxOccupyDisk = ObjectUtil.defaultIfNull(osMaxOccupyDisk, 0D);
                    double occupyDisk = NumberUtil.div(used, total1, 2);
                    if (occupyDisk > osMaxOccupyDisk) {
                        update.setOsMaxOccupyDisk(occupyDisk);
                        update.setOsMaxOccupyDiskName(CollUtil.getFirst(trim));
                    }
                }
            }
            update.setOsFileStoreTotal(total * 1024);
        }
        update.setJavaVersion(this.getFirstValue(map, "java version"));
        update.setJpomAgentPid(Convert.toInt(this.getFirstValue(map, "jpom agent pid")));
        this.updateById(update);
    }

    private String getFirstValue(Map<String, List<String>> map, String name) {
        List<String> list = map.get(name);
        String first = CollUtil.getFirst(list);
        // 内存获取可能最后存在 ：
        return StrUtil.removeSuffix(first, StrUtil.COLON);
    }

    /**
     * 更新 容器状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setId(id);
        machineSshModel.setStatus(status);
        machineSshModel.setStatusMsg(msg);
        super.updateById(machineSshModel);
    }

    /**
     * 获取 ssh 回话
     * GLOBAL
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModel(MachineSshModel sshModel) {
        MachineSshModel model = this.getByKey(sshModel.getId(), false);
        Optional.ofNullable(model).ifPresent(machineSshModel -> {
            sshModel.setPassword(StrUtil.emptyToDefault(sshModel.getPassword(), machineSshModel.getPassword()));
            sshModel.setPrivateKey(StrUtil.emptyToDefault(sshModel.getPrivateKey(), machineSshModel.getPrivateKey()));
        });
        return this.getSessionByModelNoFill(sshModel);
    }

    /**
     * 获取 ssh 回话
     * GLOBAL
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModelNoFill(MachineSshModel sshModel) {
        Assert.notNull(sshModel, "没有对应 SSH 信息");
        Session session = null;
        int timeout = sshModel.timeout();
        MachineSshModel.ConnectType connectType = sshModel.connectType();
        String user = sshModel.getUser();
        String password = sshModel.getPassword();
        // 转化密码字段
        IWorkspaceEnvPlugin plugin = (IWorkspaceEnvPlugin) PluginFactory.getPlugin(IWorkspaceEnvPlugin.PLUGIN_NAME);
        try {
            user = plugin.convertRefEnvValue(ServerConst.WORKSPACE_GLOBAL, user);
            password = plugin.convertRefEnvValue(ServerConst.WORKSPACE_GLOBAL, password);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        if (connectType == MachineSshModel.ConnectType.PASS) {
            session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), user, password, timeout);

        } else if (connectType == MachineSshModel.ConnectType.PUBKEY) {
            File rsaFile = null;
            String privateKey = sshModel.getPrivateKey();
            byte[] passwordByte = StrUtil.isEmpty(password) ? null : StrUtil.bytes(password);
            //sshModel.password();
            if (StrUtil.startWith(privateKey, URLUtil.FILE_URL_PREFIX)) {
                String rsaPath = StrUtil.removePrefix(privateKey, URLUtil.FILE_URL_PREFIX);
                rsaFile = FileUtil.file(rsaPath);
            } else if (StrUtil.startWith(privateKey, JschUtils.HEADER)) {
                // 直接采用 private key content 登录，无需写入文件
                session = JschUtils.createSession(sshModel.getHost(),
                        sshModel.getPort(),
                        user,
                        StrUtil.trim(privateKey),
                        passwordByte);
            } else if (StrUtil.isEmpty(privateKey)) {
                File home = FileUtil.getUserHomeDir();
                Assert.notNull(home, "用户目录没有找到");
                File identity = FileUtil.file(home, ".ssh", "identity");
                rsaFile = FileUtil.isFile(identity) ? identity : null;
                File idRsa = FileUtil.file(home, ".ssh", "id_rsa");
                rsaFile = FileUtil.isFile(idRsa) ? idRsa : rsaFile;
                File idDsa = FileUtil.file(home, ".ssh", "id_dsa");
                rsaFile = FileUtil.isFile(idDsa) ? idDsa : rsaFile;
                Assert.notNull(rsaFile, "用户目录没有找到私钥信息");
            } else {
                //这里的实现，用于把 private key 写入到一个临时文件中，此方式不太采取
                File tempPath = JpomApplication.getInstance().getTempPath();
                String sshFile = StrUtil.emptyToDefault(sshModel.getId(), IdUtil.fastSimpleUUID());
                rsaFile = FileUtil.file(tempPath, "ssh", sshFile);
                FileUtil.writeString(privateKey, rsaFile, CharsetUtil.UTF_8);
            }
            // 如果是私钥正文，则 session 已经初始化了
            if (session == null) {
                // 简要私钥文件是否存在
                Assert.state(FileUtil.isFile(rsaFile), "私钥文件不存在：" + FileUtil.getAbsolutePath(rsaFile));
                session = JschUtil.createSession(sshModel.getHost(),
                        sshModel.getPort(), user, FileUtil.getAbsolutePath(rsaFile), passwordByte);
            }
            try {
                session.setServerAliveInterval(timeout);
                session.setServerAliveCountMax(5);
            } catch (JSchException e) {
                log.warn("配置 ssh serverAliveInterval 错误", e);
            }
            try {
                session.connect(timeout);
            } catch (JSchException e) {
                throw new JschRuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("不支持的模式");
        }

        return session;
    }
}
