package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.ProjectRecoverModel;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
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

    /**
     * 查询所有项目信息
     *
     * @return list
     * @throws IOException 异常
     */
    @Override
    public List<ProjectInfoModel> list() throws IOException {
        JSONObject jsonObject = getJSONObject(ConfigBean.PROJECT);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(ProjectInfoModel.class);
    }

    /**
     * 保存项目信息
     *
     * @param projectInfo 项目
     */
    public void saveProject(ProjectInfoModel projectInfo) throws Exception {
        // 保存
        saveJson(ConfigBean.PROJECT, projectInfo.toJson());
    }

    /**
     * 删除项目
     *
     * @param projectInfo 项目
     */
    public void deleteProject(ProjectInfoModel projectInfo, String userId) throws Exception {
        deleteJson(ConfigBean.PROJECT, projectInfo.getId());
        // 添加回收记录
        ProjectRecoverModel projectRecoverModel = new ProjectRecoverModel(projectInfo);
        projectRecoverModel.setDelUser(userId);
        projectRecoverService.addProject(projectRecoverModel);
    }

    /**
     * 修改项目信息
     *
     * @param projectInfo 项目信息
     */
    public void updateProject(ProjectInfoModel projectInfo) throws Exception {
        projectInfo.setModifyTime(DateUtil.now());
        updateJson(ConfigBean.PROJECT, projectInfo.toJson());
    }


    /**
     * 根据id查询项目
     *
     * @param id 项目Id
     * @return model
     */
    @Override
    public ProjectInfoModel getItem(String id) throws IOException {
        return getJsonObjectById(ConfigBean.PROJECT, id, ProjectInfoModel.class);
    }

    public String getLogSize(String id) {
        ProjectInfoModel pim;
        try {
            pim = getItem(id);
            if (pim == null) {
                return null;
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return null;
        }
        String logSize = null;
        File file = new File(pim.getLog());
        if (file.exists()) {
            long fileSize = file.length();
            logSize = FileUtil.readableFileSize(fileSize);
        }
        return logSize;
    }
}
