/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.socket.ServiceFileTailWatcher;
import org.dromara.jpom.system.LogbackConfig;
import org.dromara.jpom.util.DirTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_LOG)
@SystemPermission
public class LogManageController extends BaseServerController {


    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> logData(String machineId, HttpServletRequest request) {
        IJsonMessage<List<JSONObject>> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemLog);
        return Optional.ofNullable(message)
            .orElseGet(() -> {
                List<JSONObject> data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
                return JsonMessage.success("", data);
            });
    }

    /**
     * 删除 需要验证是否最后修改时间
     *
     * @param path 路径
     * @return json
     */
    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> logData(String machineId,
                                        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_path_error.f482") String path,
                                        HttpServletRequest request) {
        JsonMessage<String> jsonMessage = this.tryRequestMachine(machineId, request, NodeUrl.DelSystemLog);
        return Optional.ofNullable(jsonMessage).orElseGet(() -> {
            File file = FileUtil.file(LogbackConfig.getPath(), path);
            // 判断修改时间
            long modified = file.lastModified();
            Assert.state(System.currentTimeMillis() - modified > TimeUnit.DAYS.toMillis(1), I18nMessageUtil.get("i18n.cannot_delete_recent_logs.ee19"));
            // 离线上一个日志
            ServiceFileTailWatcher.offlineFile(file);
            if (FileUtil.del(file)) {
                FileUtil.cleanEmpty(file.getParentFile());
                return new JsonMessage<>(200, I18nMessageUtil.get("i18n.delete_success.0007"));
            }
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.delete_failure.acf0"));
        });
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void logDownload(String machineId,
                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_path_error.f482") String path,
                            HttpServletResponse response,
                            HttpServletRequest request) {
        if (StrUtil.isNotEmpty(machineId)) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, I18nMessageUtil.get("i18n.no_machine_found.c16c"));
            NodeForward.requestDownload(model, request, response, NodeUrl.DownloadSystemLog);
            return;
        }
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        if (file.isFile()) {
            FileUtil.cleanEmpty(file.getParentFile());
            ServletUtil.write(response, file);
        }
    }
}
