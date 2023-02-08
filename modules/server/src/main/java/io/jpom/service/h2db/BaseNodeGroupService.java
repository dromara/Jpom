package io.jpom.service.h2db;

import cn.hutool.db.Entity;
import io.jpom.common.ServerConst;
import io.jpom.model.BaseNodeGroupModel;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2023/2/8
 */
public abstract class BaseNodeGroupService<T extends BaseNodeGroupModel> extends BaseNodeService<T> {

    protected BaseNodeGroupService(NodeService nodeService,
                                   WorkspaceService workspaceService,
                                   String dataName) {
        super(nodeService, workspaceService, dataName);
    }

    /**
     * load date group by group name
     *
     * @return list
     */
    public List<String> listGroup(HttpServletRequest request) {
        String workspaceId = getCheckUserWorkspace(request);
        String sql = "select `GROUP` from " + getTableName() + " where workspaceId=? group by `GROUP`";
        List<Entity> list = super.query(sql, workspaceId);
        // 筛选字段
        return list.stream().flatMap(entity -> {
                Object obj = entity.get(ServerConst.GROUP_STR);
                if (obj == null) {
                    return null;
                }
                return Stream.of(String.valueOf(obj));
            }).filter(Objects::nonNull)
            .distinct().collect(Collectors.toList());
    }

    /**
     * 恢复字段
     */
    public void repairGroupFiled() {
        String sql = "update " + getTableName() + " set `GROUP`=? where `GROUP` is null or `GROUP`=''";
        super.execute(sql, "默认");
    }
}
