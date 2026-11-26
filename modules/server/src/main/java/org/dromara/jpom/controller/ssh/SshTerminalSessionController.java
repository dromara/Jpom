/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.log.SshTerminalSessionModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.SshTerminalSessionService;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * SSH 终端会话（录制 / 回看）
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@RestController
@RequestMapping(value = "/node/ssh-terminal-session")
@Feature(cls = ClassFeature.SSH_TERMINAL_SESSION)
public class SshTerminalSessionController extends BaseServerController {

    private final SshTerminalSessionService sshTerminalSessionService;

    public SshTerminalSessionController(SshTerminalSessionService sshTerminalSessionService) {
        this.sshTerminalSessionService = sshTerminalSessionService;
    }

    /**
     * 分页查询会话
     *
     * @param request 请求
     * @return 分页
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<SshTerminalSessionModel>> list(HttpServletRequest request) {
        PageResultDto<SshTerminalSessionModel> page = sshTerminalSessionService.listPage(request);
        return JsonMessage.success("", page);
    }

    /**
     * 会话内的命令索引
     *
     * @param id      会话 id
     * @param request 请求
     * @return 命令列表
     */
    @RequestMapping(value = "commands", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> commands(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id, HttpServletRequest request) {
        SshTerminalSessionModel item = sshTerminalSessionService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        return JsonMessage.success("", sshTerminalSessionService.listCommands(item));
    }

    /**
     * 下载/流式读取录制文件（asciicast v2）
     *
     * @param id       会话 id
     * @param request  请求
     * @param response 响应
     */
    @RequestMapping(value = "cast", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void cast(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                     HttpServletRequest request, HttpServletResponse response) {
        SshTerminalSessionModel item = sshTerminalSessionService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File file = sshTerminalSessionService.castFile(item);
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.file_not_found.d952"));
        response.setContentType("application/x-asciicast;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + item.getId() + ".cast\"");
        ServletUtil.write(response, file);
    }

    /**
     * 删除会话（文件 + 记录）
     *
     * @param id      会话 id，多个用逗号分隔
     * @param request 请求
     * @return 结果
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(String id, HttpServletRequest request) {
        List<String> ids = StrUtil.splitTrim(id, StrUtil.COMMA);
        Assert.notEmpty(ids, I18nMessageUtil.get("i18n.no_data.1ac0"));
        for (String item : ids) {
            SshTerminalSessionModel model = sshTerminalSessionService.getByKey(item, request);
            if (model == null) {
                continue;
            }
            try {
                CommandUtil.systemFastDel(sshTerminalSessionService.castFile(model));
            } catch (Exception ignored) {
                // 文件已不存在时不阻断记录删除
            }
            sshTerminalSessionService.delByKey(item, request);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }
}
