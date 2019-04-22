package cn.keepbx.jpom.model.data;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 分发实体
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
public class OutGivingModel extends BaseModel {
    private List<NodeProject> nodeProjectList;
    /**
     *
     */
    private int afterOpt;

    public int getAfterOpt() {
        return afterOpt;
    }

    public void setAfterOpt(int afterOpt) {
        this.afterOpt = afterOpt;
    }

    public List<NodeProject> getNodeProjectList() {
        return nodeProjectList;
    }

    public void setNodeProjectList(List<NodeProject> nodeProjectList) {
        this.nodeProjectList = nodeProjectList;
    }

    /**
     * 判断是否包含某个项目id
     *
     * @param projectId 项目id
     * @return true 包含
     */
    public boolean checkContains(String projectId) {
        if (projectId == null) {
            return false;
        }
        List<NodeProject> thisPs = getNodeProjectList();
        if (thisPs == null) {
            return false;
        }
        for (NodeProject nodeProject1 : thisPs) {
            if (StrUtil.equalsIgnoreCase(nodeProject1.getProjectId(), projectId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取节点的项目信息
     *
     * @param nodeId    节点
     * @param projectId 项目
     * @return nodeProject
     */
    public NodeProject getNodeProject(String nodeId, String projectId) {
        List<NodeProject> thisPs = getNodeProjectList();
        if (thisPs == null) {
            return null;
        }
        for (NodeProject nodeProject1 : thisPs) {
            if (StrUtil.equalsIgnoreCase(nodeProject1.getProjectId(), projectId) && StrUtil.equalsIgnoreCase(nodeProject1.getNodeId(), nodeId)) {
                return nodeProject1;
            }
        }
        return null;
    }

    /**
     * 分发后的操作
     */
    public enum AfterOpt implements BaseEnum {
        /**
         * 操作
         */
        No(0, "不做任何操作"),
        Restart(1, "自动重启");
        private int code;
        private String desc;

        AfterOpt(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    public void start() {
        List<NodeProject> thisPs = getNodeProjectList();
        if (thisPs == null) {
            return;
        }
        thisPs.forEach(nodeProject -> nodeProject.setStatus(NodeProject.Status.Ing.getCode()));
    }

    /**
     * 节点项目
     */
    public static class NodeProject extends BaseJsonModel {
        private String nodeId;
        private String projectId;
        private String lastOutGivingTime;
        private int status;
        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public int getStatus() {
            return status;
        }

        public String getStatusMsg() {
            return BaseEnum.getDescByCode(Status.class, getStatus());
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getLastOutGivingTime() {
            return StrUtil.emptyToDefault(lastOutGivingTime, StrUtil.DASHED);
        }

        public void setLastOutGivingTime(String lastOutGivingTime) {
            this.lastOutGivingTime = lastOutGivingTime;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public enum Status implements BaseEnum {
            /**
             *
             */
            No(0, "未分发"),
            Ing(1, "分发中"),
            Ok(2, "分发成功"),
            Fail(3, "分发失败"),
            ;
            private int code;
            private String desc;

            Status(int code, String desc) {
                this.code = code;
                this.desc = desc;
            }

            @Override
            public int getCode() {
                return code;
            }

            @Override
            public String getDesc() {
                return desc;
            }
        }
    }

    /**
     * 分发线程
     */
    public static class OutGivingRun implements Runnable {
        private String outGivingId;
        private NodeProject nodeProject;
        private NodeModel nodeModel;
        private File file;
        private AfterOpt afterOpt;
        private UserModel userModel;

        public OutGivingRun(String outGivingId, NodeProject nodeProject, File file, AfterOpt afterOpt, UserModel userModel) {
            this.outGivingId = outGivingId;
            this.nodeProject = nodeProject;
            this.file = file;
            this.afterOpt = afterOpt;
            //
            NodeService nodeService = SpringUtil.getBean(NodeService.class);
            this.nodeModel = nodeService.getItem(nodeProject.getNodeId());
            //
            this.userModel = userModel;
        }

        @Override
        public void run() {
            try {
                String url = nodeModel.getRealUrl(NodeUrl.Manage_File_Upload);
                HttpRequest request = HttpUtil.createPost(url);
                // 授权信息
                NodeForward.addUser(request, this.nodeModel, this.userModel);
                request.form("file", file)
                        .form("id", this.nodeProject.projectId)
                        .form("type", "unzip");
                // 操作
                if (afterOpt == AfterOpt.Restart) {
                    request.form("after", "restart");
                }
                //
                String body = request.execute()
                        .body();
                JsonMessage jsonMessage = NodeForward.toJsonMessage(body);
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    updateStatus(NodeProject.Status.Ok, body);
                } else {
                    updateStatus(NodeProject.Status.Fail, body);
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(this.nodeProject.nodeId + " " + this.nodeProject.projectId + " " + "分发异常保存", e);
                try {
                    updateStatus(NodeProject.Status.Fail, e.getMessage());
                } catch (IOException ex) {
                    DefaultSystemLog.ERROR().error(this.nodeProject.nodeId + " " + this.nodeProject.projectId + " " + "分发异常保存", ex);
                }
            }
        }

        /**
         * 更新状态
         */
        private void updateStatus(NodeProject.Status status, String msg) throws IOException {
            synchronized (OutGivingRun.class) {
                OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
                OutGivingModel outGivingModel = outGivingServer.getItem(this.outGivingId);
                List<NodeProject> nodeProjects = outGivingModel.getNodeProjectList();
                for (NodeProject nodeProject : nodeProjects) {
                    if (!nodeProject.getProjectId().equalsIgnoreCase(OutGivingRun.this.nodeProject.getProjectId())) {
                        continue;
                    }
                    nodeProject.setStatus(status.getCode());
                    nodeProject.setResult(msg);
                    nodeProject.setLastOutGivingTime(DateUtil.now());
                }
                outGivingServer.updateItem(outGivingModel);
            }
        }
    }
}
