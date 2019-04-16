package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.ProjectRecoverModel;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectRecoverService extends BaseOperService<ProjectRecoverModel> {

    /**
     * 查询所有项目信息
     *
     * @return list
     */
    @Override
    public List<ProjectRecoverModel> list() {
        JSONObject jsonObject = getJSONObject(ConfigBean.PROJECT_RECOVER);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(ProjectRecoverModel.class);
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
        saveJson(ConfigBean.PROJECT_RECOVER, projectInfo.toJson());
    }

    /**
     * 根据id查询项目
     *
     * @param id 项目Id
     * @return model
     */
    @Override
    public ProjectRecoverModel getItem(String id) throws IOException {
        return getJsonObjectById(ConfigBean.PROJECT_RECOVER, id, ProjectRecoverModel.class);
    }

    @Override
    public boolean updateItem(ProjectRecoverModel projectRecoverModel) throws Exception {
        return false;
    }
}
