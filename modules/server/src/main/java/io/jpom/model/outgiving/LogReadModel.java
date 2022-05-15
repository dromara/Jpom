package io.jpom.model.outgiving;

import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 日志阅读
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@TableName(value = "LOG_READ", name = "日志阅读")
@Data
@EqualsAndHashCode(callSuper = true)
public class LogReadModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 节点下的项目列表
     *
     * @see Item
     */
    private String nodeProject;
    /**
     * 缓存操作数据
     */
    private String cacheData;

    @Data
    public static class CacheDta {
        /**
         * 日志文件名称
         */
        private String logFile;
        /**
         * 显示关键词，后多少行
         */
        private Integer afterCount;
        /**
         * 显示关键词，前多少行
         */
        private Integer beforeCount;
        /**
         * 搜索关键词
         */
        private String keyword;
        /**
         * 使用等节点ID
         */
        private String useProjectId;
    }


    public List<Item> nodeProjectList() {
        return StringUtil.jsonConvertArray(nodeProject, Item.class);
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
    public Item getNodeProject(String nodeId, String projectId) {
        List<Item> thisPs = nodeProjectList();
        if (thisPs == null) {
            return null;
        }
        for (Item outGivingNodeProject1 : thisPs) {
            if (StrUtil.equalsIgnoreCase(outGivingNodeProject1.getProjectId(), projectId) && StrUtil.equalsIgnoreCase(outGivingNodeProject1.getNodeId(), nodeId)) {
                return outGivingNodeProject1;
            }
        }
        return null;
    }


    public static class Item extends BaseNodeProject {

    }
}
