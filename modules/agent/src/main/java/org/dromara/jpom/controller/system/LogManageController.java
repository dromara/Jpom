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
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.socket.AgentFileTailWatcher;
import org.dromara.jpom.system.LogbackConfig;
import org.dromara.jpom.util.DirTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class LogManageController extends BaseAgentController {


    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> logData() {
        List<JSONObject> data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
        return JsonMessage.success("", data);
    }


    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public IJsonMessage<String> logData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_path_error.f482") String path) {
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        // 判断修改时间
        long modified = file.lastModified();
        Assert.state(System.currentTimeMillis() - modified > TimeUnit.DAYS.toMillis(1), I18nMessageUtil.get("i18n.cannot_delete_recent_logs.ee19"));
        AgentFileTailWatcher.offlineFile(file);
        if (FileUtil.del(file)) {
            FileUtil.cleanEmpty(file.getParentFile());
            return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
        }
        return new JsonMessage<>(500, I18nMessageUtil.get("i18n.delete_failure.acf0"));
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void logDownload(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_path_error.f482") String path, HttpServletResponse response) {
        File file = FileUtil.file(LogbackConfig.getPath(), path);
        if (file.isFile()) {
            ServletUtil.write(response, file);
        }
    }
}
