package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分发实体
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
public class OutGivingModel extends BaseModel {
    /**
     * 节点下的项目列表
     */
    private List<OutGivingNodeProject> outGivingNodeProjectList;
    /**
     * 分发后的操作
     */
    private int afterOpt;
    /**
     * 是否清空旧包发布
     */
    private boolean clearOld;
    /**
     * 临时缓存
     */
    private Map<NodeModel, JSONObject> tempCacheMap;
    /**
     * 是否为单独创建的分发项目
     */
    private boolean outGivingProject;

    public boolean isClearOld() {
        return clearOld;
    }

    public void setClearOld(boolean clearOld) {
        this.clearOld = clearOld;
    }

    public boolean isOutGivingProject() {
        return outGivingProject;
    }

    public void setOutGivingProject(boolean outGivingProject) {
        this.outGivingProject = outGivingProject;
    }

    public Map<NodeModel, JSONObject> getTempCacheMap() {
        return tempCacheMap;
    }

    public void setTempCacheMap(Map<NodeModel, JSONObject> tempCacheMap) {
        this.tempCacheMap = tempCacheMap;
    }

    public int getAfterOpt() {
        return afterOpt;
    }

    public void setAfterOpt(int afterOpt) {
        this.afterOpt = afterOpt;
    }

    public List<OutGivingNodeProject> getOutGivingNodeProjectList() {
        return outGivingNodeProjectList;
    }

    public void setOutGivingNodeProjectList(List<OutGivingNodeProject> outGivingNodeProjectList) {
        this.outGivingNodeProjectList = outGivingNodeProjectList;
    }

    /**
     * 判断是否包含某个项目id
     *
     * @param projectId 项目id
     * @return true 包含
     */
    public boolean checkContains(String nodeId, String projectId) {
        return getNodeProject(nodeId, projectId) != null;
    }

    /**
     * 获取节点的项目信息
     *
     * @param nodeId    节点
     * @param projectId 项目
     * @return outGivingNodeProject
     */
    public OutGivingNodeProject getNodeProject(String nodeId, String projectId) {
        List<OutGivingNodeProject> thisPs = getOutGivingNodeProjectList();
        return getNodeProject(thisPs, nodeId, projectId);
    }

    /**
     * 从指定数组中获取对应信息
     *
     * @param outGivingNodeProjects 节点项目列表
     * @param nodeId                节点id
     * @param projectId             项目id
     * @return 实体
     */
    private OutGivingNodeProject getNodeProject(List<OutGivingNodeProject> outGivingNodeProjects, String nodeId, String projectId) {
        if (outGivingNodeProjects == null) {
            return null;
        }
        for (OutGivingNodeProject outGivingNodeProject1 : outGivingNodeProjects) {
            if (StrUtil.equalsIgnoreCase(outGivingNodeProject1.getProjectId(), projectId) && StrUtil.equalsIgnoreCase(outGivingNodeProject1.getNodeId(), nodeId)) {
                return outGivingNodeProject1;
            }
        }
        return null;
    }

    /**
     * 获取已经删除的节点项目
     *
     * @param newsProject 要比较的分发项目
     * @return 已经删除过的
     */
    public List<OutGivingNodeProject> getDelete(List<OutGivingNodeProject> newsProject) {
        List<OutGivingNodeProject> old = getOutGivingNodeProjectList();
        if (old == null || old.isEmpty()) {
            return null;
        }
        List<OutGivingNodeProject> delete = new ArrayList<>();
        old.forEach(outGivingNodeProject -> {
            if (getNodeProject(newsProject, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId()) != null) {
                return;
            }
            delete.add(outGivingNodeProject);
        });
        return delete;
    }

    /**
     * 分发后的操作
     */
    public enum AfterOpt implements BaseEnum {
        /**
         * 操作
         */
        No(0, "不做任何操作"),
        /**
         * 并发执行项目分发
         */
        Restart(1, "并发重启"),
        /**
         * 顺序执行项目分发
         */
        Order_Must_Restart(2, "完整顺序重启(有节点分发并重启失败将不再进行分发剩余节点)"),
        /**
         * 顺序执行项目分发
         */
        Order_Restart(3, "顺序重启(有节点分发并重启失败将继续分发剩余节点)"),
        ;
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
}
