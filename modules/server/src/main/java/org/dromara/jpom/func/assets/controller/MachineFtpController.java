package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/8/31
 */
@RestController
@RequestMapping(value = "/system/assets/ftp")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_SSH)
@SystemPermission
@Slf4j
public class MachineFtpController extends BaseGroupNameController {

    private final MachineFtpServer machineFtpServer;

    public MachineFtpController(MachineFtpServer machineFtpServer) {
        super(machineFtpServer);
        this.machineFtpServer = machineFtpServer;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<MachineFtpModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineFtpModel> pageResultDto = machineFtpServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    /**
     * 编辑
     *
     * @param name               名称
     * @param host               端口
     * @param user               用户名
     * @param password           密码
     * @param serverLanguageCode 服务器语言
     * @param systemKey          服务器系统关键词
     * @param port               端口
     * @param charset            编码格式
     * @param id                 ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_ssh_name_cannot_be_empty.ff4f") String name,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.host_cannot_be_empty.644a") String host,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_user_cannot_be_empty.9239") String user,
                                     String password,
                                     @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.parameter_error_port_error.810d") int port,
                                     String charset,
                                     String id,
                                     Integer timeout,
                                     String allowEditSuffix,
                                     String serverLanguageCode,
                                     String systemKey,
                                     String mode,
                                     String groupName) {

        MachineFtpModel model = new MachineFtpModel();
        model.setId(id);
        model.setGroupName(groupName);
        model.setHost(host);
        model.setServerLanguageCode(serverLanguageCode);
        model.setMode(EnumUtil.fromString(FtpMode.class, mode, FtpMode.Active));
        model.setSystemKey(systemKey);
        // 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
        Opt.ofBlankAble(password).ifPresent(model::setPassword);
        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, I18nMessageUtil.get("i18n.suffix_cannot_be_empty.ec72"));
        model.allowEditSuffix(allowEditSuffixList);
        model.setPort(port);
        model.setUser(user);
        model.setName(name);
        model.setTimeout(timeout);
        try {
            Charset.forName(charset);
            model.setCharset(charset);
        } catch (Exception e) {
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.correct_encoding_format_required.1f7f") + e.getMessage());
        }
        // 判断重复
        Entity entity = Entity.create();
        entity.set("host", model.getHost());
        entity.set("port", model.getPort());
        entity.set(DialectUtil.wrapField("user"), model.getUser());
        Opt.ofBlankAble(id).ifPresent(s -> entity.set("id", StrUtil.format(" <> {}", s)));
        boolean exists = machineFtpServer.exists(entity);
        Assert.state(!exists, "对应的FTP已经存在啦");
        // 测试连接
        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(model), model.getMode())) {
            ftp.pwd();
        } catch (Exception e) {
            log.error("连接FTP失败", e);
            return new JsonMessage<>(500, "连接FTP失败：" + e.getMessage());
        }

        model.setStatus(1);
        boolean add = StrUtil.isEmpty(id);
        int i = add ? machineFtpServer.insert(model) : machineFtpServer.updateById(model);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }
}
