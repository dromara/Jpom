package cn.keepbx.jpom.service.manage;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectInfoService extends BaseOperService<ProjectInfoModel> {

    /**
     * 查询所有项目信息
     *
     * @return list
     * @throws IOException 异常
     */
    @Override
    public List<ProjectInfoModel> list() throws IOException {
        JSONObject jsonObject = getJsonObject(ConfigBean.PROJECT);
        Set<String> setKey = jsonObject.keySet();
        JSONArray jsonArray = new JSONArray();
        for (String key : setKey) {
            jsonArray.add(jsonObject.getJSONObject(key));
        }
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
     * @param id 项目Id
     */
    public void deleteProject(String id) throws Exception {
        deleteJson(ConfigBean.PROJECT, id);
    }

    /**
     * 修改项目信息
     *
     * @param projectInfo 项目信息
     */
    public void updateProject(ProjectInfoModel projectInfo) throws Exception {
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
