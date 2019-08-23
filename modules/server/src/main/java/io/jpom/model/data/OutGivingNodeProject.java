package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseJsonModel;
import io.jpom.service.node.NodeService;

/**
 * 节点项目
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
public class OutGivingNodeProject extends BaseJsonModel {
    private static NodeService nodeService;

    private String nodeId;
    private String projectId;
    private String lastOutGivingTime;
    private int status = Status.No.getCode();
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

    /**
     * 获取节点的数据
     *
     * @param get 防止自动读取
     * @return 实体
     */
    public NodeModel getNodeData(boolean get) {
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        return nodeService.getItem(this.nodeId);
    }

    /**
     * 状态
     */
    public enum Status implements BaseEnum {
        /**
         *
         */
        No(0, "未分发"),
        Ing(1, "分发中"),
        Ok(2, "分发成功"),
        Fail(3, "分发失败"),
        Cancel(4, "取消分发"),
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
