/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.configuration.ProjectConfig;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.DslScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JvmUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * unix
 *
 * @author bwcx_jzy
 * @since 2021/12/17
 */
@Slf4j
public abstract class BaseUnixProjectCommander extends AbstractProjectCommander {

    public BaseUnixProjectCommander(Charset fileCharset,
                                    SystemCommander systemCommander,
                                    ProjectConfig projectConfig,
                                    DslScriptServer dslScriptServer,
                                    ProjectInfoService projectInfoService) {
        super(fileCharset, systemCommander, projectConfig, dslScriptServer, projectInfoService);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(nodeProjectInfoModel);
        return this.buildRunCommand(nodeProjectInfoModel, infoModel);
    }


    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String lib = projectInfoService.resolveLibPath(originalModel);
        String path = this.getClassPathLib(originalModel, lib);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String tag = nodeProjectInfoModel.getId();
        String absoluteLog = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        return StrUtil.format("nohup {} {} {} {} {} {} >> {} 2>&1 &",
            getRunJavaPath(nodeProjectInfoModel, false),
            Optional.ofNullable(nodeProjectInfoModel.getJvm()).orElse(StrUtil.EMPTY),
            JvmUtil.getJpomPidTag(tag, lib),
            path,
            Optional.ofNullable(originalModel.mainClass()).orElse(StrUtil.EMPTY),
            Optional.ofNullable(nodeProjectInfoModel.getArgs()).orElse(StrUtil.EMPTY),
            absoluteLog);
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, int pid) {
        File file = projectInfoService.resolveLibFile(originalModel);
        List<String> result = new ArrayList<>();
        boolean success = false;
        String kill = systemCommander.kill(file, pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, originalModel, false)) {
            success = true;
        } else {
            // 强制杀进程
            result.add("Kill not completed, test kill -9");
            String cmd = String.format("kill -9 %s", pid);
            try {
                CommandUtil.asyncExeLocalCommand(cmd, file, null, true);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
            //
            if (this.loopCheckRun(nodeProjectInfoModel, originalModel, 5, false)) {
                success = true;
            } else {
                result.add("Kill -9 not completed, kill -9 failed ");
            }
        }
        String tag = nodeProjectInfoModel.getId();
        return CommandOpResult.of(success, status(tag)).appendMsg(result);
//        return status(tag) + StrUtil.SPACE + kill;
    }

    /**
     * 尝试ps -ef | grep  中查看进程id
     *
     * @param tag 进程标识
     * @return 运行标识
     */
    @Override
    protected String bySystemPs(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("ps -ef | grep " + tag);
        log.debug("getPsStatus {} {}", tag, execSystemCommand);
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        for (String item : list) {
            if (JvmUtil.checkCommandLineIsJpom(item, tag)) {
                String[] split = StrUtil.splitToArray(item, StrUtil.SPACE);
                return StrUtil.format("{}:{}", AbstractProjectCommander.RUNNING_TAG, split[1]);
            }
        }
        return AbstractProjectCommander.STOP_TAG;
    }
}
