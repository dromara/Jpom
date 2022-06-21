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
package io.jpom.controller.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.common.BaseServerController;
import io.jpom.common.Type;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.log.SshTerminalExecuteLog;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.SshTerminalExecuteLogService;
import io.jpom.service.node.ssh.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@RestController
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
@Slf4j
public class SshController extends BaseServerController {

    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final BuildInfoService buildInfoService;

    public SshController(SshService sshService,
                         SshTerminalExecuteLogService sshTerminalExecuteLogService,
                         BuildInfoService buildInfoService) {
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.buildInfoService = buildInfoService;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<SshModel>> listData() {
        PageResultDto<SshModel> pageResultDto = sshService.listPage(getRequest());
        return new JsonMessage<>(200, "", pageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<SshModel>> listDataAll() {
        List<SshModel> list = sshService.listByWorkspace(getRequest());
        return new JsonMessage<>(200, "", list);
    }

    @GetMapping(value = "get-item.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<SshModel> getItem(@ValidatorItem String id) {
        SshModel byKey = sshService.getByKey(id, getRequest());
        Assert.notNull(byKey, "对应的 ssh 不存在");
        return new JsonMessage<>(200, "", byKey);
    }


    /**
     * 编辑
     *
     * @param name              名称
     * @param host              端口
     * @param user              用户名
     * @param password          密码
     * @param connectType       连接方式
     * @param privateKey        私钥
     * @param port              端口
     * @param charset           编码格式
     * @param fileDirs          文件夹
     * @param id                ID
     * @param notAllowedCommand 禁止输入的命令
     * @return json
     */
    @PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
                       String password,
                       SshModel.ConnectType connectType,
                       String privateKey,
                       @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
                       String charset, String fileDirs,
                       String id, String notAllowedCommand) {
        SshModel sshModel;
        boolean add = StrUtil.isEmpty(getParameter("id"));
        if (add) {
            // 优先判断参数 如果是 password 在修改时可以不填写
            if (connectType == SshModel.ConnectType.PASS) {
                Assert.hasText(password, "请填写登录密码");
            } else if (connectType == SshModel.ConnectType.PUBKEY) {
                //Assert.hasText(privateKey, "请填写证书内容");
            }
            sshModel = new SshModel();
        } else {
            sshModel = sshService.getByKey(id);
            Assert.notNull(sshModel, "不存在对应ssh");
        }
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
        sshModel.setNotAllowedCommand(notAllowedCommand);
        sshModel.setConnectType(connectType.name());
        // 获取允许编辑的后缀
        String allowEditSuffix = getParameter("allowEditSuffix");
        int timeout = getParameterInt("timeout", 5);
        sshModel.setTimeout(timeout);
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        try {
            Charset.forName(charset);
            sshModel.setCharset(charset);
        } catch (Exception e) {
            return JsonMessage.getString(405, "请填写正确的编码格式");
        }
        // 判断重复
        HttpServletRequest request = getRequest();
        String workspaceId = sshService.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("host", sshModel.getHost());
        entity.set("port", sshModel.getPort());
        entity.set("user", sshModel.getUser());
        entity.set("connectType", sshModel.getConnectType());
        entity.set("workspaceId", workspaceId);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = sshService.exists(entity);
        Assert.state(!exists, "对应的SSH已经存在啦");
        try {
            SshModel model = sshService.getByKey(id, false);
            if (model != null) {
                sshModel.setPassword(StrUtil.emptyToDefault(sshModel.getPassword(), model.getPassword()));
                sshModel.setPrivateKey(StrUtil.emptyToDefault(sshModel.getPrivateKey(), model.getPrivateKey()));
            }
            Session session = SshService.getSessionByModel(sshModel);
            JschUtil.close(session);
        } catch (Exception e) {
            log.warn("ssh连接失败", e);
            return JsonMessage.getString(505, "ssh连接失败：" + e.getMessage());
        }
        if (add) {
            sshService.insert(sshModel);
        } else {
            sshService.update(sshModel);
        }
        return JsonMessage.getString(200, "操作成功");
    }


    /**
     * 检查 ssh 是否安装插件端
     *
     * @param ids ids
     * @return json
     */
    @GetMapping(value = "check_agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String checkAgent(String ids) {
        List<SshModel> sshModels = sshService.listById(StrUtil.split(ids, StrUtil.COMMA), getRequest());
        Assert.notEmpty(sshModels, "没有任何节点信息");

        JSONObject result = new JSONObject();
        for (SshModel sshModel : sshModels) {
            List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(sshModel.getId());
            JSONObject data = new JSONObject();
            NodeModel nodeModel = CollUtil.getFirst(nodeBySshId);
            SshModel model = sshService.getByKey(sshModel.getId(), false);
            try {
                if (nodeModel == null) {
                    Integer pid = sshService.checkSshRunPid(model, Type.Agent.getTag());
                    data.put("pid", ObjectUtil.defaultIfNull(pid, 0));
                    data.put("ok", true);
                } else {
                    data.put("nodeId", nodeModel.getId());
                    data.put("nodeName", nodeModel.getName());
                }
                //
                String javaVersion = sshService.checkCommand(model, "java");
                data.put("javaVersion", javaVersion);
            } catch (Exception e) {
                log.error("检查运行状态异常:{}", e.getMessage());
                data.put("error", e.getMessage());
            }
            result.put(sshModel.getId(), data);
        }
        return JsonMessage.getString(200, "", result);
    }

    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
        HttpServletRequest request = getRequest();
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, request, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, "当前ssh存在构建项，不能删除");
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state(CollUtil.isEmpty(nodeBySshId), "当前ssh被节点绑定，不能删除");

        sshService.delByKey(id, request);
        //
        int logCount = sshTerminalExecuteLogService.delByWorkspace(request, entity -> entity.set("sshId", id));
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log_list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public String logListData() {
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(getRequest());
        return JsonMessage.getString(200, "获取成功", pageResult);
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids         节点ID
     * @param workspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public String syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(getRequest());
        //
        sshService.checkUserWorkspace(workspaceId);
        sshService.syncToWorkspace(ids, nowWorkspaceId, workspaceId);
        return JsonMessage.getString(200, "操作成功");
    }
}
