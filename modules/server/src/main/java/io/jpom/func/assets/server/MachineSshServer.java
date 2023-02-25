package io.jpom.func.assets.server;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Entity;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.jpom.JpomApplication;
import io.jpom.common.Const;
import io.jpom.common.ILoadEvent;
import io.jpom.common.ServerConst;
import io.jpom.cron.IAsyncLoad;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.model.data.SshModel;
import io.jpom.plugin.IWorkspaceEnvPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.util.JschUtils;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@Service
@Slf4j
public class MachineSshServer extends BaseDbService<MachineSshModel> implements ILoadEvent, IAsyncLoad, Runnable {

    @Resource
    @Lazy
    private SshService sshService;

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
        // 更新节点的机器id

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

    }

    @Override
    public void run() {

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
