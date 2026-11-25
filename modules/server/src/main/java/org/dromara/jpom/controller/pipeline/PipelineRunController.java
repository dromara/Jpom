/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.pipeline;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONObject;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.pipeline.PipelineRunner;
import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.pipeline.model.PipelineRunModel;
import org.dromara.jpom.pipeline.service.PipelineInfoService;
import org.dromara.jpom.pipeline.service.PipelineRunService;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流水线运行记录与运行日志
 * <p>
 * 与配置读写分开：运行记录是高频轮询入口（运行中每 2 秒拉一次日志），
 * 单独一个控制器便于后续限流/审计，也让两个类的职责各自收敛。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@RestController
@RequestMapping(value = "/pipeline/run")
@Feature(cls = ClassFeature.PIPELINE)
public class PipelineRunController extends BaseServerController {

    /**
     * 单次读取行数（与前端翻页粒度一致）
     */
    private static final int LOG_PAGE_SIZE = 1000;

    private final PipelineInfoService pipelineInfoService;
    private final PipelineRunService pipelineRunService;
    private final PipelineRunner pipelineRunner;

    public PipelineRunController(PipelineInfoService pipelineInfoService,
                                 PipelineRunService pipelineRunService,
                                 PipelineRunner pipelineRunner) {
        this.pipelineInfoService = pipelineInfoService;
        this.pipelineRunService = pipelineRunService;
        this.pipelineRunner = pipelineRunner;
    }

    /**
     * 运行记录列表（分页，按运行编号倒序）
     *
     * @param pipelineId 流水线 ID
     * @param request    请求
     * @return 记录
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<PipelineRunModel>> runList(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String pipelineId,
                                          HttpServletRequest request) {
        PipelineInfoModel item = pipelineInfoService.getByKey(pipelineId, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        // 强制条件：流水线与工作空间都只认服务端校验后的值，客户端传的同名字段一律剔除
        BaseDbService.putForcedFilter(paramMap, "pipelineId", pipelineId);
        BaseDbService.putForcedFilter(paramMap, "workspaceId", item.getWorkspaceId());
        if (!paramMap.containsKey("order_field")) {
            paramMap.put("order_field", "runNumber");
            paramMap.put("order", "descend");
        }
        PageResultDto<PipelineRunModel> page = pipelineRunService.listPage(paramMap);
        page.each(run -> {
            File logFile = pipelineRunner.logFile(run.getPipelineId(), run.getRunNumber());
            long size = FileUtil.isFile(logFile) ? logFile.length() : 0L;
            run.setLogFileSize(size);
            run.setHasLog(size > 0);
        });
        return JsonMessage.success("", page);
    }

    /**
     * 读取运行日志（增量：从 line 行开始取，与在线构建 {@code get-now-log} 同一套读取器）
     *
     * @param runId 运行记录 ID
     * @param line  起始行号（1 开始）
     * @param request 请求
     * @return 日志片段与运行状态
     */
    @RequestMapping(value = "/log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> runLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String runId,
                                           Integer line,
                                           HttpServletRequest request) {
        PipelineRunModel run = pipelineRunService.getByKey(runId, request);
        Assert.notNull(run, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File logFile = pipelineRunner.logFile(run.getPipelineId(), run.getRunNumber());
        if (!FileUtil.isFile(logFile)) {
            return new JsonMessage<>(300, I18nMessageUtil.get("i18n.log_file_does_not_exist.f6c6"));
        }
        // 分页读取（每页 1000 行）：在线构建的读取器只保留最后一次范围内的 1000 行，
        // 长日志会丢头部；这里按行号推进，前端翻页直到 hasMore=false，保证「完整日志」。
        int start = line == null || line < 1 ? 1 : line;
        List<String> allLines = FileUtil.readLines(logFile, CharsetUtil.CHARSET_UTF_8);
        int from = Math.min(Math.max(0, start - 1), allLines.size());
        int to = Math.min(allLines.size(), from + LOG_PAGE_SIZE);
        JSONObject data = new JSONObject();
        data.put("dataLines", new ArrayList<>(allLines.subList(from, to)));
        data.put("line", to + 1);
        data.put("getLine", start);
        data.put("totalLine", allLines.size());
        data.put("hasMore", to < allLines.size());
        data.put("run", this.isRunning(run.getStatus()));
        data.put("status", run.getStatus());
        data.put("statusMsg", run.getStatusMsg());
        data.put("runNumber", run.getRunNumber());
        data.put("logFileSize", logFile.length());
        return JsonMessage.success("", data);
    }

    /**
     * 下载运行日志
     *
     * @param runId    运行记录 ID
     * @param request  请求
     * @param response 响应
     */
    @RequestMapping(value = "/download-log", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String runId,
                            HttpServletRequest request, HttpServletResponse response) {
        PipelineRunModel run = pipelineRunService.getByKey(runId, request);
        Assert.notNull(run, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File logFile = pipelineRunner.logFile(run.getPipelineId(), run.getRunNumber());
        if (FileUtil.isFile(logFile)) {
            ServletUtil.write(response, logFile);
        }
    }

    /**
     * 运行中判定：只有这些状态才继续轮询日志
     *
     * @param status 状态码
     * @return true 表示仍在执行
     */
    private boolean isRunning(Integer status) {
        if (status == null) {
            return false;
        }
        return status == BuildStatus.Ing.getCode()
            || status == BuildStatus.PubIng.getCode()
            || status == BuildStatus.WaitExec.getCode()
            || status == BuildStatus.CancelRequested.getCode();
    }
}
