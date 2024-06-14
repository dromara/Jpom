/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.outgiving;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.log.OutGivingLog;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@Slf4j
@Setter
public class OutGivingItemRun implements Callable<OutGivingNodeProject.Status> {

    private final String outGivingId;
    private final OutGivingNodeProject outGivingNodeProject;
    private final NodeModel nodeModel;
    private final File file;
    private final AfterOpt afterOpt;
    private final boolean unzip;
    private final boolean clearOld;
    private final Integer sleepTime;
    private final String secondaryDirectory;
    private final Boolean closeFirst;
    private int stripComponents;

    public OutGivingItemRun(OutGivingModel item,
                            OutGivingNodeProject outGivingNodeProject,
                            File file,
                            boolean unzip,
                            Integer sleepTime) {
        this.outGivingId = item.getId();
        this.secondaryDirectory = item.getSecondaryDirectory();
        this.clearOld = item.clearOld();
        this.closeFirst = item.getUploadCloseFirst();
        this.unzip = unzip;
        this.outGivingNodeProject = outGivingNodeProject;
        this.file = file;
        this.afterOpt = ObjectUtil.defaultIfNull(EnumUtil.likeValueOf(AfterOpt.class, item.getAfterOpt()), AfterOpt.No);
        //
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        this.nodeModel = nodeService.getByKey(outGivingNodeProject.getNodeId());
        //
        this.sleepTime = sleepTime;
    }

    @Override
    public OutGivingNodeProject.Status call() {
        OutGivingNodeProject.Status result;
        long time = SystemClock.now();
        String fileSize = FileUtil.readableFileSize(file);
        try {
            if (this.outGivingNodeProject.getDisabled() != null && this.outGivingNodeProject.getDisabled()) {
                // 禁用
                this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Cancel, I18nMessageUtil.get("i18n.project_disabled.f8b3"));
                return OutGivingNodeProject.Status.Cancel;
            }
            this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Ing, I18nMessageUtil.get("i18n.start_distribution.bce5"));
            //
            JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, this.secondaryDirectory,
                this.outGivingNodeProject.getProjectId(),
                unzip,
                afterOpt,
                this.nodeModel, this.clearOld,
                this.sleepTime, this.closeFirst, this.stripComponents, (total, progressSize) -> {

                    String logId = OutGivingRun.getLogId(outGivingId, outGivingNodeProject);
                    //
                    OutGivingLog outGivingLog = new OutGivingLog();
                    outGivingLog.setId(logId);
                    outGivingLog.setFileSize(total);
                    outGivingLog.setProgressSize(progressSize);
                    //
                    DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
                    dbOutGivingLogService.updateById(outGivingLog);
                });
            result = jsonMessage.success() ? OutGivingNodeProject.Status.Ok : OutGivingNodeProject.Status.Fail;

            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put("upload_duration", new BetweenFormatter(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND, 2).format());
            jsonObject.put("upload_file_size", fileSize);
            this.updateStatus(this.outGivingId, result, jsonObject.toString());
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.distribution_exception_saving.8285"), this.outGivingNodeProject.getNodeId(), this.outGivingNodeProject.getProjectId(), e);
            result = OutGivingNodeProject.Status.Fail;
            JSONObject jsonObject = JsonMessage.toJson(500, e.getMessage());
            jsonObject.put("upload_duration", new BetweenFormatter(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND, 2).format());
            jsonObject.put("upload_file_size", fileSize);
            this.updateStatus(this.outGivingId, result, jsonObject.toString());
        }
        return result;
    }

    /**
     * 更新状态
     *
     * @param outGivingId 分发id
     * @param status      状态
     * @param msg         消息描述
     */
    private void updateStatus(String outGivingId, OutGivingNodeProject.Status status, String msg) {
        String logId = OutGivingRun.getLogId(outGivingId, outGivingNodeProject);
        OutGivingLog outGivingLog = new OutGivingLog();
        outGivingLog.setId(logId);
        outGivingLog.setStatus(status.getCode());
        outGivingLog.setResult(msg);
        if (status == OutGivingNodeProject.Status.Ok || status == OutGivingNodeProject.Status.Fail) {
            outGivingLog.setEndTime(SystemClock.now());
        }
        DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
        dbOutGivingLogService.updateById(outGivingLog);
    }
}
