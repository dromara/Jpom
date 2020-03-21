package io.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.BaseAgentController;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.model.data.ProjectRecoverModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectInfoService extends BaseOperService<ProjectInfoModel> {
    @Resource
    private ProjectRecoverService projectRecoverService;

    public ProjectInfoService() {
        super(AgentConfigBean.PROJECT);
    }

    public HashSet<String> getAllGroup() {
        //获取所有分组
        List<ProjectInfoModel> projectInfoModels = list();
        HashSet<String> hashSet = new HashSet<>();
        if (projectInfoModels == null) {
            return hashSet;
        }
        for (ProjectInfoModel projectInfoModel : projectInfoModels) {
            hashSet.add(projectInfoModel.getGroup());
        }
        return hashSet;
    }


    /**
     * 删除项目
     *
     * @param id 项目
     */
    @Override
    public void deleteItem(String id) {
        ProjectInfoModel projectInfo = getItem(id);
        String userId = BaseAgentController.getNowUserName();
        super.deleteItem(id);
        // 添加回收记录
        ProjectRecoverModel projectRecoverModel = new ProjectRecoverModel(projectInfo);
        projectRecoverModel.setDelUser(userId);
        projectRecoverService.addItem(projectRecoverModel);
    }

    /**
     * 修改项目信息
     *
     * @param projectInfo 项目信息
     */
    @Override
    public void updateItem(ProjectInfoModel projectInfo) {
        projectInfo.setModifyTime(DateUtil.now());
        String userName = BaseAgentController.getNowUserName();
        if (!StrUtil.DASHED.equals(userName)) {
            projectInfo.setModifyUser(userName);
        }
        super.updateItem(projectInfo);
    }

    @Override
    public void addItem(ProjectInfoModel projectInfoModel) {
        projectInfoModel.setCreateTime(DateUtil.now());
        super.addItem(projectInfoModel);
    }

    /**
     * 查看项目控制台日志文件大小
     *
     * @param projectInfoModel 项目
     * @param copyItem  副本
     * @return 文件大小
     */
    public String getLogSize(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem copyItem) {
        if (projectInfoModel == null) {
            return null;
        }
        File file = copyItem == null ? new File(projectInfoModel.getLog()) : projectInfoModel.getLog(copyItem);
        if (file.exists()) {
            long fileSize = file.length();
            if (fileSize <= 0) {
                return null;
            }
            return FileUtil.readableFileSize(fileSize);
        }
        return null;
    }
}
