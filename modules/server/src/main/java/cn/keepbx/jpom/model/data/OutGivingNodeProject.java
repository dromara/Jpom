package cn.keepbx.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.node.NodeService;
import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 节点项目
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class OutGivingNodeProject extends BaseJsonModel {
    private static ProjectInfoService projectInfoService;
    private static NodeService nodeService;

    private String nodeId;
    private String projectId;
    private String lastOutGivingTime;
    private int status = Status.No.getCode();
    private String result;
    private JSONObject projectInfo;

    public String getStatusMsg() {
        return BaseEnum.getDescByCode(Status.class, getStatus());
    }

    public String getLastOutGivingTime() {
        return StrUtil.emptyToDefault(lastOutGivingTime, StrUtil.DASHED);
    }

    /**
     * 获取对应的项目数据
     *
     * @param get 防止自动读取
     * @return json
     */
    public JSONObject getProjectData(boolean get) {
        if (projectInfo != null) {
            return projectInfo;
        }
        if (projectInfoService == null) {
            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        }
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        NodeModel nodeModel = nodeService.getItem(this.nodeId);
        try {
            projectInfo = projectInfoService.getItem(nodeModel, this.projectId);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取信息异常", e);
        }
        return projectInfo;
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
