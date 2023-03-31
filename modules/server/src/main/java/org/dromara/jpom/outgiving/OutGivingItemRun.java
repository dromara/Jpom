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
package org.dromara.jpom.outgiving;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.log.OutGivingLog;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
                this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Cancel, "当前项目被禁用");
                return OutGivingNodeProject.Status.Cancel;
            }
            this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Ing, "开始分发");
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
            log.error("{} {} 分发异常保存", this.outGivingNodeProject.getNodeId(), this.outGivingNodeProject.getProjectId(), e);
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
