package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.ProjectRecoverModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectRecoverService extends BaseOperService<ProjectRecoverModel> {

    public ProjectRecoverService() {
        super(AgentConfigBean.PROJECT_RECOVER);
    }


    /**
     * 保存项目信息
     *
     * @param projectInfo 项目
     */
    @Override
    public void addItem(ProjectRecoverModel projectInfo) {
        projectInfo.setDelTime(DateUtil.now());
        // 保存
        super.addItem(projectInfo);
    }
}
