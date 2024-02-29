/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.outgiving;

import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 分发管理
 *
 * @author bwcx_jzy
 * @since 2019/4/21
 */
@Service
public class OutGivingServer extends BaseWorkspaceService<OutGivingModel> implements IStatusRecover {


    public void checkNodeProject(String nodeId, String projectId, HttpServletRequest request, String msg) {
        // 检查节点分发
        List<OutGivingModel> outGivingModels = super.listByWorkspace(request);
        if (outGivingModels != null) {
            boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeId, projectId));
            Assert.state(!match, msg);
        }
    }

    public boolean checkNode(String nodeId, HttpServletRequest request) {
        List<OutGivingModel> list = super.listByWorkspace(request);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (OutGivingModel outGivingModel : list) {
            List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.outGivingNodeProjectList();
            if (outGivingNodeProjectList != null) {
                for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjectList) {
                    if (outGivingNodeProject.getNodeId().equals(nodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int statusRecover() {
        // 恢复异常数据
        String updateSql = "update " + super.getTableName() + " set status=? where status=?";
        return super.execute(updateSql, OutGivingModel.Status.DONE.getCode(), OutGivingModel.Status.ING.getCode());
    }
}
