package cn.keepbx.outgiving;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.OutGivingLog;
import cn.keepbx.jpom.service.dblog.DbOutGivingLogService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 分发线程
 *
 * @author bwcx_jzy
 * @date 2019/7/18
 **/
public class OutGivingRun implements Callable<OutGivingNodeProject.Status> {
    private String outGivingId;
    private OutGivingNodeProject outGivingNodeProject;
    private NodeModel nodeModel;
    private File file;
    private OutGivingModel.AfterOpt afterOpt;
    private UserModel userModel;
    private boolean unzip;
    private boolean clearOld;
    /**
     * 数据库记录id
     */
    private String logId;


    /**
     * 开始异步执行分发任务
     *
     * @param id        分发id
     * @param file      文件
     * @param userModel 操作的用户
     */
    public static void startRun(String id,
                                File file,
                                UserModel userModel,
                                boolean unzip) throws IOException {
        OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
        OutGivingModel item = outGivingServer.getItem(id);
        Objects.requireNonNull(item, "不存在分发");
        OutGivingModel.AfterOpt afterOpt = BaseEnum.getEnum(OutGivingModel.AfterOpt.class, item.getAfterOpt());
        if (afterOpt == null) {
            afterOpt = OutGivingModel.AfterOpt.No;
        }
        OutGivingModel.AfterOpt finalAfterOpt = afterOpt;
        //
        List<OutGivingNodeProject> outGivingNodeProjects = item.getOutGivingNodeProjectList();
        // 开启线程
        if (afterOpt == OutGivingModel.AfterOpt.Order_Restart || afterOpt == OutGivingModel.AfterOpt.Order_Must_Restart) {
            ThreadUtil.execute(() -> {
                boolean cancel = false;
                for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjects) {
                    if (cancel) {
                        updateStatus(null, id, outGivingNodeProject,
                                OutGivingNodeProject.Status.Cancel, "前一个节点分发失败，取消分发");
                    } else {
                        OutGivingRun outGivingRun = new OutGivingRun(item, outGivingNodeProject, file, userModel, unzip);
                        OutGivingNodeProject.Status status = outGivingRun.call();
                        if (status != OutGivingNodeProject.Status.Ok) {
                            if (finalAfterOpt == OutGivingModel.AfterOpt.Order_Must_Restart) {
                                // 完整重启，不再继续剩余的节点项目
                                cancel = true;
                            }
                        }
                    }
                }
            });
        } else if (afterOpt == OutGivingModel.AfterOpt.Restart || afterOpt == OutGivingModel.AfterOpt.No) {

            outGivingNodeProjects.forEach(outGivingNodeProject -> ThreadUtil.execAsync(
                    new OutGivingRun(item, outGivingNodeProject, file, userModel, unzip)));
        } else {
            //
            throw new RuntimeException("Not implemented");
        }
    }

    private OutGivingRun(OutGivingModel item,
                         OutGivingNodeProject outGivingNodeProject,
                         File file,
                         UserModel userModel,
                         boolean unzip) {
        this.outGivingId = item.getId();
        this.unzip = unzip;
        this.clearOld = item.isClearOld();
        this.outGivingNodeProject = outGivingNodeProject;
        this.file = file;
        OutGivingModel.AfterOpt afterOpt = BaseEnum.getEnum(OutGivingModel.AfterOpt.class, item.getAfterOpt());
        if (afterOpt == null) {
            afterOpt = OutGivingModel.AfterOpt.No;
        }
        this.afterOpt = afterOpt;
        //
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        this.nodeModel = nodeService.getItem(outGivingNodeProject.getNodeId());
        //
        this.userModel = userModel;
        this.logId = IdUtil.fastSimpleUUID();
    }

    @Override
    public OutGivingNodeProject.Status call() {
        OutGivingNodeProject.Status result;
        try {
            updateStatus(this.logId, this.outGivingId, this.outGivingNodeProject,
                    OutGivingNodeProject.Status.Ing, "开始分发");
            //
            JsonMessage jsonMessage = fileUpload(file,
                    this.outGivingNodeProject.getProjectId(),
                    unzip,
                    afterOpt != OutGivingModel.AfterOpt.No,
                    this.nodeModel, this.userModel, this.clearOld);
            if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                result = OutGivingNodeProject.Status.Ok;
                updateStatus(this.logId, this.outGivingId, this.outGivingNodeProject,
                        result, jsonMessage.toString());
            } else {
                result = OutGivingNodeProject.Status.Fail;
                updateStatus(this.logId, this.outGivingId, this.outGivingNodeProject,
                        result, jsonMessage.toString());
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(this.outGivingNodeProject.getNodeId() + " " + this.outGivingNodeProject.getProjectId() + " " + "分发异常保存", e);
            result = OutGivingNodeProject.Status.Fail;
            updateStatus(this.logId, this.outGivingId, this.outGivingNodeProject,
                    result, "error:" + e.getMessage());
        }
        return result;
    }

    /**
     * 上传项目文件
     *
     * @param file      需要上传的文件
     * @param projectId 项目id
     * @param unzip     是否需要解压
     * @param restart   是否需要重启
     * @param nodeModel 节点
     * @param userModel 操作用户
     * @return json
     */
    public static JsonMessage fileUpload(File file, String projectId,
                                         boolean unzip, boolean restart,
                                         NodeModel nodeModel, UserModel userModel,
                                         boolean clearOld) {
        JSONObject data = new JSONObject();
        data.put("file", file);
        data.put("id", projectId);
        if (unzip) {
            // 解压
            data.put("type", "unzip");
            if (clearOld) {
                // 清空
                data.put("clearType", "clear");
            }
        }
        // 操作
        if (restart) {
            data.put("after", "restart");
        }
        return NodeForward.request(nodeModel, NodeUrl.Manage_File_Upload, userModel, data);
    }

    /**
     * 更新状态
     *
     * @param outGivingId              分发id
     * @param outGivingNodeProjectItem 分发项
     * @param status                   状态
     * @param msg                      消息描述
     */
    private static void updateStatus(String logId,
                                     String outGivingId,
                                     OutGivingNodeProject outGivingNodeProjectItem,
                                     OutGivingNodeProject.Status status,
                                     String msg) {
        synchronized (OutGivingRun.class) {
            OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
            OutGivingModel outGivingModel = outGivingServer.getItem(outGivingId);
            OutGivingNodeProject finOutGivingNodeProject = null;
            List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.getOutGivingNodeProjectList();
            for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjects) {
                if (!outGivingNodeProject.getProjectId().equalsIgnoreCase(outGivingNodeProjectItem.getProjectId()) ||
                        !outGivingNodeProject.getNodeId().equalsIgnoreCase(outGivingNodeProjectItem.getNodeId())) {
                    continue;
                }
                outGivingNodeProject.setStatus(status.getCode());
                outGivingNodeProject.setResult(msg);
                outGivingNodeProject.setLastOutGivingTime(DateUtil.now());
                //
                finOutGivingNodeProject = outGivingNodeProject;
            }
            outGivingServer.updateItem(outGivingModel);
            //
            OutGivingLog outGivingLog = new OutGivingLog();
            if (logId != null) {
                outGivingLog.setId(logId);
            } else {
                outGivingLog.setId(IdUtil.fastSimpleUUID());
            }
            if (finOutGivingNodeProject != null) {
                outGivingLog.setNodeId(finOutGivingNodeProject.getNodeId());
                outGivingLog.setProjectId(finOutGivingNodeProject.getProjectId());
            }
            outGivingLog.setOutGivingId(outGivingId);
            outGivingLog.setResult(msg);
            outGivingLog.setStatus(status.getCode());
            DbOutGivingLogService dbOutGivingLogService = SpringUtil.getBean(DbOutGivingLogService.class);
            if (status == OutGivingNodeProject.Status.Ing || status == OutGivingNodeProject.Status.Cancel) {
                // 开始或者 取消都还没有记录
                dbOutGivingLogService.insert(outGivingLog);
            } else {
                dbOutGivingLogService.update(outGivingLog);
            }
        }
    }
}
