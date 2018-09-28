package cn.jiangzeyin.service.manage;

import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.BaseService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ManageService extends BaseService {

    private static final String FILENAME = "project.json";

    /**
     * 查询所有项目信息
     *
     * @return
     * @throws IOException
     */
    public JSONObject getAllProjectInfo() throws IOException {
        return getJsonObject(FILENAME);
    }

    /**
     * 保存项目信息
     *
     * @param projectInfo
     * @return
     */
    public void saveProject(ProjectInfoModel projectInfo) throws Exception {
        // 保存
        saveJson(FILENAME, (JSONObject) JSONObject.toJSON(projectInfo));
    }

    /**
     * 删除项目
     *
     * @param id
     */
    public void deleteProject(String id) throws Exception {
        deleteJson(FILENAME, id);
    }

    /**
     * 修改项目信息
     *
     * @param projectInfo
     */
    public void updateProject(ProjectInfoModel projectInfo) throws Exception {
        // 修改
        updateJson(FILENAME, (JSONObject) JSONObject.toJSON(projectInfo));
    }


    /**
     * 根据id查询项目
     *
     * @param id
     * @return
     */
    public ProjectInfoModel getProjectInfo(String id) throws IOException {
        JSONObject jsonObject = getJsonObject(FILENAME, id);
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(ProjectInfoModel.class);
    }
}
