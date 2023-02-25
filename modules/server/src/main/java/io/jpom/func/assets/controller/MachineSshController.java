package io.jpom.func.assets.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.JschUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.common.JsonMessage;
import io.jpom.common.Type;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.func.BaseGroupNameController;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.func.assets.server.MachineSshServer;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.SshModel;
import io.jpom.model.log.SshTerminalExecuteLog;
import io.jpom.model.user.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.SshTerminalExecuteLogService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@RestController
@RequestMapping(value = "/system/assets/ssh")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_SSH)
@SystemPermission
@Slf4j
public class MachineSshController extends BaseGroupNameController {

    private final MachineSshServer machineSshServer;
    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final WorkspaceService workspaceService;

    public MachineSshController(MachineSshServer machineSshServer,
                                SshService sshService,
                                SshTerminalExecuteLogService sshTerminalExecuteLogService,
                                WorkspaceService workspaceService) {
        super(machineSshServer);
        this.machineSshServer = machineSshServer;
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.workspaceService = workspaceService;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<MachineSshModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineSshModel> pageResultDto = machineSshServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }


    /**
     * 编辑
     *
     * @param name        名称
     * @param host        端口
     * @param user        用户名
     * @param password    密码
     * @param connectType 连接方式
     * @param privateKey  私钥
     * @param port        端口
     * @param charset     编码格式
     * @param id          ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
                                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
                                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
                                    String password,
                                    MachineSshModel.ConnectType connectType,
                                    String privateKey,
                                    @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
                                    String charset,
                                    String id,
                                    Integer timeout) {
        boolean add = StrUtil.isEmpty(id);
        if (add) {
            // 优先判断参数 如果是 password 在修改时可以不填写
            if (connectType == MachineSshModel.ConnectType.PASS) {
                Assert.hasText(password, "请填写登录密码");
            } else if (connectType == MachineSshModel.ConnectType.PUBKEY) {
                //Assert.hasText(privateKey, "请填写证书内容");
            }
        } else {
            boolean exists = machineSshServer.exists(new MachineSshModel(id));
            Assert.state(exists, "不存在对应ssh");
        }
        MachineSshModel sshModel = new MachineSshModel();
        sshModel.setId(id);
        sshModel.setHost(host);
        // 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
        if (StrUtil.isNotEmpty(password)) {
            sshModel.setPassword(password);
        }
        if (StrUtil.startWith(privateKey, URLUtil.FILE_URL_PREFIX)) {
            String rsaPath = StrUtil.removePrefix(privateKey, URLUtil.FILE_URL_PREFIX);
            Assert.state(FileUtil.isFile(rsaPath), "配置的私钥文件不存在");
        }
        if (StrUtil.isNotEmpty(privateKey)) {
            sshModel.setPrivateKey(privateKey);
        }

        sshModel.setPort(port);
        sshModel.setUser(user);
        sshModel.setName(name);
        sshModel.setConnectType(connectType.name());
        sshModel.setTimeout(timeout);
        try {
            Charset.forName(charset);
            sshModel.setCharset(charset);
        } catch (Exception e) {
            return new JsonMessage<>(405, "请填写正确的编码格式");
        }
        // 判断重复
        Entity entity = Entity.create();
        entity.set("host", sshModel.getHost());
        entity.set("port", sshModel.getPort());
        entity.set("`user`", sshModel.getUser());
        entity.set("connectType", sshModel.getConnectType());
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = machineSshServer.exists(entity);
        Assert.state(!exists, "对应的SSH已经存在啦");
        try {
            Session session = machineSshServer.getSessionByModel(sshModel);
            JschUtil.close(session);
        } catch (Exception e) {
            log.warn("ssh连接失败", e);
            return new JsonMessage<>(505, "ssh连接失败：" + e.getMessage());
        }
        if (add) {
            machineSshServer.insert(sshModel);
        } else {

            machineSshServer.update(sshModel);
        }
        return JsonMessage.success("操作成功");
    }

    /**
     * 检查 ssh 是否安装插件端
     *
     * @param ids ids
     * @return json
     */
    @GetMapping(value = "check-agent", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> checkAgent(String ids) {
        List<MachineSshModel> sshModels = machineSshServer.listById(StrUtil.split(ids, StrUtil.COMMA), false);
        Assert.notEmpty(sshModels, "没有任何节点信息");

        JSONObject result = new JSONObject();
        for (MachineSshModel sshModel : sshModels) {
            JSONObject data = new JSONObject();
            try {
                Integer pid = sshService.checkSshRunPid(sshModel, Type.Agent.getTag());
                data.put("pid", ObjectUtil.defaultIfNull(pid, 0));
                //
                String javaVersion = sshService.checkCommand(sshModel, "java");
                data.put("javaVersion", javaVersion);
            } catch (Exception e) {
                log.error("检查运行状态异常:{}", e.getMessage());
                data.put("error", e.getMessage());
            }
            result.put(sshModel.getId(), data);
        }
        return JsonMessage.success("", result);
    }

    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> delete(@ValidatorItem String id) {
        long count = sshService.countByMachine(id);
        Assert.state(count <= 0, "当前机器SSH还关联" + count + "个ssh不能删除");
        machineSshServer.delByKey(id);
        return JsonMessage.success("操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log-list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<SshTerminalExecuteLog>> logListData(HttpServletRequest request) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(paramMap);
        return JsonMessage.success("获取成功", pageResult);
    }

    @GetMapping(value = "list-workspace-ssh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<SshModel>> listWorkspaceSsh(@ValidatorItem String id) {
        MachineSshModel machineSshModel = machineSshServer.getByKey(id);
        Assert.notNull(machineSshModel, "没有对应的机器");
        SshModel sshModel = new SshModel();
        sshModel.setMachineSshId(id);
        List<SshModel> modelList = sshService.listByBean(sshModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (SshModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return JsonMessage.success("", modelList);
    }

    /**
     * 保存工作空间配置
     *
     * @param fileDirs          文件夹
     * @param id                ID
     * @param notAllowedCommand 禁止输入的命令
     * @return json
     */
    @PostMapping(value = "save-workspace-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> saveWorkspaceConfig(
        String fileDirs,
        @ValidatorItem String id,
        String notAllowedCommand,
        String allowEditSuffix) {
        SshModel sshModel = new SshModel(id);
        // 目录
        if (StrUtil.isEmpty(fileDirs)) {
            sshModel.fileDirs(null);
        } else {
            List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
            for (String s : list) {
                String normalize = FileUtil.normalize(s + StrUtil.SLASH);
                int count = StrUtil.count(normalize, StrUtil.SLASH);
                Assert.state(count >= 2, "ssh 授权目录不能是根目录");
            }
            //
            UserModel userModel = getUser();
            Assert.state(!userModel.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            sshModel.fileDirs(list);
        }
        sshModel.setNotAllowedCommand(notAllowedCommand);
        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        sshService.update(sshModel);
        return JsonMessage.success("操作成功");
    }
}
