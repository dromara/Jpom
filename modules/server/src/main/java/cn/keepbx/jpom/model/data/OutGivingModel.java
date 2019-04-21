package cn.keepbx.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.model.BaseModel;

import java.util.List;

/**
 * 分发实体
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
public class OutGivingModel extends BaseModel {
    private List<NodeProject> nodeProjectList;

    public List<NodeProject> getNodeProjectList() {
        return nodeProjectList;
    }

    public void setNodeProjectList(List<NodeProject> nodeProjectList) {
        this.nodeProjectList = nodeProjectList;
    }

    public boolean checkEquals(String trueProjectId) {
        if (trueProjectId == null) {
            return false;
        }
        List<NodeProject> thisPs = getNodeProjectList();
        if (thisPs == null) {
            return false;
        }
        for (NodeProject nodeProject1 : thisPs) {
            if (StrUtil.equalsIgnoreCase(nodeProject1.getProjectId(), trueProjectId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 节点项目
     */
    public static class NodeProject extends BaseJsonModel {
        private String nodeId;
        private String projectId;
        private String lastOutGivingTime;
        private int status;

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

        public boolean checkEquals(NodeProject nodeProject) {
            if (nodeProject == null) {
                return false;
            }
            if (nodeProject == this) {
                return true;
            }
            return StrUtil.equalsIgnoreCase(this.getNodeId(), nodeProject.getNodeId()) &&
                    StrUtil.equalsIgnoreCase(this.getProjectId(), nodeProject.getProjectId());
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
}
