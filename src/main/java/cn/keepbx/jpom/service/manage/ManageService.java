package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.service.BaseDataService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

/**
 * @author jiangzeyin
 */
@Service
public class ManageService extends BaseDataService {

    private static final String FILENAME = ConfigBean.PROJECT;

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
     * @param projectInfo 项目信息
     */
    public void updateProject(ProjectInfoModel projectInfo) throws Exception {
        // 修改
        JSONObject jsonObject = getJsonObject(FILENAME, projectInfo.getId());
        if (jsonObject == null) {
            return;
        }
        projectInfo.setModifyTime(DateUtil.now());
        JSONObject newJson = projectInfo.toJson();
        Set<String> keys = newJson.keySet();
        for (String key : keys) {
            String val = newJson.getString(key);
            if (StrUtil.isEmptyOrUndefined(val)) {
                continue;
            }
            jsonObject.put(key, newJson.get(key));
        }
        updateJson(FILENAME, jsonObject);
    }


    /**
     * 根据id查询项目
     *
     * @param id 项目Id
     * @return model
     */
    public ProjectInfoModel getProjectInfo(String id) throws IOException {
        JSONObject jsonObject = getJsonObject(FILENAME, id);
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(ProjectInfoModel.class);
    }

}
