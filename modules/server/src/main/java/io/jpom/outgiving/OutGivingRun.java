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
package io.jpom.outgiving;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.SyncFinisher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseIdModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.OutGivingLog;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.model.user.UserModel;
import io.jpom.service.outgiving.DbOutGivingLogService;
import io.jpom.service.outgiving.OutGivingServer;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 分发线程
 *
 * @author bwcx_jzy
 * @since 2019/7/18
 **/
@Slf4j
public class OutGivingRun {

    private static final Map<String, SyncFinisher> SYNC_FINISHER_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> LOG_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 取消分发
     *
     * @param id 分发id
     */
    public static void cancel(String id) {
        SyncFinisher syncFinisher = SYNC_FINISHER_MAP.remove(id);
        Optional.ofNullable(syncFinisher).ifPresent(SyncFinisher::stopNow);
        //
        Map<String, String> map = LOG_CACHE_MAP.remove(id);
        Optional.ofNullable(map).ifPresent(map1 -> {
            DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
            for (String logId : map1.values()) {
                OutGivingLog outGivingLog = new OutGivingLog();
                outGivingLog.setId(logId);
                outGivingLog.setStatus(OutGivingNodeProject.Status.ArtificialCancel.getCode());
                outGivingLog.setResult("手动取消分发");
                dbOutGivingLogService.update(outGivingLog);
            }
            if (!map1.isEmpty()) {
                //
                OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
                // 更新分发数据
                OutGivingModel outGivingModel1 = new OutGivingModel();
                outGivingModel1.setId(id);
                outGivingModel1.setStatus(OutGivingModel.Status.CANCEL.getCode());
                outGivingServer.update(outGivingModel1);
            }
        });

    }


    public static String getLogId(String outId, OutGivingNodeProject nodeProject) {
        Map<String, String> map = LOG_CACHE_MAP.get(outId);
        Assert.notNull(map, "当前分发数据丢失");
        String dataId = StrUtil.format("{}_{}", nodeProject.getNodeId(), nodeProject.getProjectId());
        String logId = map.get(dataId);
        Assert.hasText(logId, "当前分发数据丢失，记录id 不存在");
        return logId;
    }

    public static void removeLogId(String outId, OutGivingNodeProject nodeProject) {
        Map<String, String> map = LOG_CACHE_MAP.get(outId);
        Assert.notNull(map, "当前分发数据丢失");
        String dataId = StrUtil.format("{}_{}", nodeProject.getNodeId(), nodeProject.getProjectId());
        map.remove(dataId);
    }

    /**
     * 标记系统取消
     *
     * @param cancelList  需要取消的 list
     * @param outGivingId 分发id
     */
    private static void systemCancel(String outGivingId, List<OutGivingNodeProject> cancelList) {
        if (CollUtil.isEmpty(cancelList)) {
            return;
        }
        DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
        for (OutGivingNodeProject outGivingNodeProject : cancelList) {
            String logId = OutGivingRun.getLogId(outGivingId, outGivingNodeProject);
            OutGivingLog outGivingLog = new OutGivingLog();
            outGivingLog.setId(logId);
            outGivingLog.setStatus(OutGivingNodeProject.Status.Cancel.getCode());
            outGivingLog.setResult("前一个节点分发失败，取消分发");
            dbOutGivingLogService.update(outGivingLog);
        }
    }

    /**
     * 开始异步执行分发任务
     *
     * @param id              分发id
     * @param file            文件
     * @param userModel       操作的用户
     * @param stripComponents 剔除文件夹
     * @param unzip           解压
     */
    public synchronized static void startRun(final String id,
                                             File file,
                                             UserModel userModel,
                                             boolean unzip, int stripComponents) {
        OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
        OutGivingModel item = outGivingServer.getByKey(id);
        Objects.requireNonNull(item, "不存在分发");
        AfterOpt afterOpt = ObjectUtil.defaultIfNull(EnumUtil.likeValueOf(AfterOpt.class, item.getAfterOpt()), AfterOpt.No);
        SyncFinisher syncFinisher;
        //
        List<OutGivingNodeProject> outGivingNodeProjects = item.outGivingNodeProjectList();
        int projectSize = outGivingNodeProjects.size();
        final List<OutGivingNodeProject.Status> statusList = new ArrayList<>(projectSize);
        // 开启线程
        if (afterOpt == AfterOpt.Order_Restart || afterOpt == AfterOpt.Order_Must_Restart) {
            syncFinisher = new SyncFinisher(1);
            syncFinisher.addWorker(() -> {
                try {
                    // 截取睡眠时间
                    int sleepTime = ObjectUtil.defaultIfNull(item.getIntervalTime(), 10);
                    //
                    int nowIndex;
                    for (nowIndex = 0; nowIndex < outGivingNodeProjects.size(); nowIndex++) {
                        final OutGivingNodeProject outGivingNodeProject = outGivingNodeProjects.get(nowIndex);
                        final OutGivingItemRun outGivingRun = new OutGivingItemRun(item, outGivingNodeProject, file, unzip, sleepTime);
                        outGivingRun.setStripComponents(stripComponents);
                        OutGivingNodeProject.Status status = outGivingRun.call();
                        if (status != OutGivingNodeProject.Status.Ok) {
                            if (afterOpt == AfterOpt.Order_Must_Restart) {
                                // 完整重启，不再继续剩余的节点项目
                                break;
                            }
                        }
                        statusList.add(status);
                        // 删除标记 log
                        removeLogId(id, outGivingNodeProject);
                        // 休眠x秒 等待之前项目正常启动
                        ThreadUtil.sleep(sleepTime, TimeUnit.SECONDS);
                    }
                    // 取消后面的分发
                    List<OutGivingNodeProject> cancelList = CollUtil.sub(outGivingNodeProjects, nowIndex + 1, outGivingNodeProjects.size());
                    systemCancel(id, cancelList);
                } catch (Exception e) {
                    log.error("分发异常 {}", id, e);
                }
            });
        } else if (afterOpt == AfterOpt.Restart || afterOpt == AfterOpt.No) {
            // 判断最大值
            int threadSize = Math.min(projectSize, RuntimeUtil.getProcessorCount());
            syncFinisher = new SyncFinisher(threadSize);

            for (final OutGivingNodeProject outGivingNodeProject : outGivingNodeProjects) {
                final OutGivingItemRun outGivingItemRun = new OutGivingItemRun(item, outGivingNodeProject, file, unzip, null);
                outGivingItemRun.setStripComponents(stripComponents);
                syncFinisher.addWorker(() -> {
                    try {
                        statusList.add(outGivingItemRun.call());
                        // 删除标记 log
                        removeLogId(id, outGivingNodeProject);
                    } catch (Exception e) {
                        log.error("分发异常", e);
                    }
                });
            }
        } else {
            //
            throw new IllegalArgumentException("Not implemented " + afterOpt.getDesc());
        }
        String userId = Optional.ofNullable(userModel).map(BaseIdModel::getId).orElse(Const.SYSTEM_ID);
        // 更新维准备中
        allPrepare(id, userId);
        // 开启线程
        SYNC_FINISHER_MAP.put(id, syncFinisher);
        // 异步执行
        ThreadUtil.execute(() -> {
            try {
                syncFinisher.start();
                syncFinisher.close();
                // 更新分发状态
                OutGivingModel.Status status;
                String msg;
                if (statusList.size() != projectSize) {
                    //
                    status = OutGivingModel.Status.FAIL;
                    msg = StrUtil.format("完成的个数不足 {}/{}", statusList.size(), projectSize);
                } else {
                    int successCount = statusList.stream().mapToInt(value -> value == OutGivingNodeProject.Status.Ok ? 1 : 0).sum();
                    if (successCount == projectSize) {
                        status = OutGivingModel.Status.DONE;
                        msg = "分发成功 " + successCount;
                    } else {
                        status = OutGivingModel.Status.FAIL;
                        msg = StrUtil.format("完成并成功的个数不足 {}/{}", successCount, projectSize);
                    }
                }
                updateStatus(id, status, msg);
            } catch (Exception e) {
                log.error("分发线程异常", e);
                updateStatus(id, OutGivingModel.Status.FAIL, e.getMessage());
            } finally {
                // 删除分发的文件
                FileUtil.del(file);
                //
                IoUtil.close(SYNC_FINISHER_MAP.remove(id));
                LOG_CACHE_MAP.remove(id);
            }
        });
    }

    /**
     * 将所有数据更新维准备中
     *
     * @param outGivingId 分发id
     * @param userId      用户id
     */
    private static void allPrepare(String outGivingId, String userId) {
        OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
        OutGivingModel outGivingModel = outGivingServer.getByKey(outGivingId);
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, "没有分发项目");
        //
        List<OutGivingLog> outGivingLogs = outGivingNodeProjects.stream()
            .map(outGivingNodeProject -> {
                OutGivingLog outGivingLog = new OutGivingLog();
                outGivingLog.setOutGivingId(outGivingId);
                outGivingLog.setWorkspaceId(outGivingModel.getWorkspaceId());
                outGivingLog.setNodeId(outGivingNodeProject.getNodeId());
                outGivingLog.setProjectId(outGivingNodeProject.getProjectId());
                outGivingLog.setModifyUser(userId);
                outGivingLog.setStartTime(SystemClock.now());
                outGivingLog.setStatus(OutGivingNodeProject.Status.Prepare.getCode());
                return outGivingLog;
            })
            .collect(Collectors.toList());
        DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
        dbOutGivingLogService.insert(outGivingLogs);
        //
        Map<String, String> logIdMap = CollStreamUtil.toMap(outGivingLogs, outGivingLog -> StrUtil.format("{}_{}", outGivingLog.getNodeId(), outGivingLog.getProjectId()), BaseIdModel::getId);

        OutGivingRun.LOG_CACHE_MAP.put(outGivingId, logIdMap);

        // 更新分发数据
        OutGivingModel outGivingModel1 = new OutGivingModel();
        outGivingModel1.setId(outGivingId);
        outGivingModel1.setStatus(OutGivingModel.Status.ING.getCode());
        outGivingServer.update(outGivingModel1);

    }

    private static void updateStatus(String outGivingId, OutGivingModel.Status status, String msg) {
        OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
        OutGivingModel outGivingModel1 = new OutGivingModel();
        outGivingModel1.setId(outGivingId);
        outGivingModel1.setStatus(status.getCode());
        outGivingModel1.setStatusMsg(msg);
        outGivingServer.update(outGivingModel1);
    }

    /**
     * 上传项目文件
     *
     * @param file       需要上传的文件
     * @param projectId  项目id
     * @param unzip      是否需要解压
     * @param afterOpt   是否需要重启
     * @param nodeModel  节点
     * @param clearOld   清空发布
     * @param levelName  文件夹层级
     * @param closeFirst 保存项目文件前先关闭项目
     * @return json
     */
    public static JsonMessage<String> fileUpload(File file, String levelName, String projectId,
                                                 boolean unzip,
                                                 AfterOpt afterOpt,
                                                 NodeModel nodeModel,
                                                 boolean clearOld,
                                                 Boolean closeFirst,
                                                 BiConsumer<Long, Long> streamProgress) {
        return fileUpload(file, levelName, projectId, unzip, afterOpt, nodeModel, clearOld, null, closeFirst, streamProgress);
    }

    /**
     * 上传项目文件
     *
     * @param file       需要上传的文件
     * @param projectId  项目id
     * @param unzip      是否需要解压
     * @param afterOpt   是否需要重启
     * @param nodeModel  节点
     * @param clearOld   清空发布
     * @param levelName  文件夹层级
     * @param sleepTime  休眠时间
     * @param closeFirst 保存项目文件前先关闭项目
     * @return json
     */
    public static JsonMessage<String> fileUpload(File file, String levelName, String projectId,
                                                 boolean unzip,
                                                 AfterOpt afterOpt,
                                                 NodeModel nodeModel,
                                                 boolean clearOld,
                                                 Integer sleepTime,
                                                 Boolean closeFirst,
                                                 BiConsumer<Long, Long> streamProgress) {
        return fileUpload(file, levelName, projectId, unzip, afterOpt, nodeModel, clearOld, sleepTime, closeFirst, 0, streamProgress);
    }

    /**
     * 上传项目文件
     *
     * @param file       需要上传的文件
     * @param projectId  项目id
     * @param unzip      是否需要解压
     * @param afterOpt   是否需要重启
     * @param nodeModel  节点
     * @param clearOld   清空发布
     * @param levelName  文件夹层级
     * @param sleepTime  休眠时间
     * @param closeFirst 保存项目文件前先关闭项目
     * @return json
     */
    public static JsonMessage<String> fileUpload(File file, String levelName, String projectId,
                                                 boolean unzip,
                                                 AfterOpt afterOpt,
                                                 NodeModel nodeModel,
                                                 boolean clearOld,
                                                 Integer sleepTime,
                                                 Boolean closeFirst, int stripComponents,
                                                 BiConsumer<Long, Long> streamProgress) {
        JSONObject data = new JSONObject();
        //  data.put("file", file);
        data.put("id", projectId);
        Opt.ofBlankAble(levelName).ifPresent(s -> data.put("levelName", s));
        Opt.ofNullable(sleepTime).ifPresent(integer -> data.put("sleepTime", integer));

        if (unzip) {
            // 解压
            data.put("type", "unzip");
            data.put("stripComponents", stripComponents);
        }
        if (clearOld) {
            // 清空
            data.put("clearType", "clear");
        }
        // 操作
        if (afterOpt != AfterOpt.No) {
            data.put("after", afterOpt.getCode());
        }
        data.put("closeFirst", closeFirst);
        try {
            return NodeForward.requestSharding(nodeModel, NodeUrl.Manage_File_Upload_Sharding, data, file,
                sliceData -> {
                    sliceData.putAll(data);
                    return NodeForward.request(nodeModel, NodeUrl.Manage_File_Sharding_Merge, sliceData);
                },
                streamProgress);
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }

        //return NodeForward.request(nodeModel, NodeUrl.Manage_File_Upload, data);
    }
}
