package cn.keepbx.jpom.service.build;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 构建service
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 **/
@Service
public class BuildService extends BaseOperService<BuildModel> implements BaseDynamicService {

    public BuildService() {
        super(ServerConfigBean.BUILD);
    }

    @Override
    public void updateItem(BuildModel buildModel) {
        buildModel.setModifyTime(DateUtil.now());
        super.updateItem(buildModel);
    }

    public boolean checkOutGiving(String outGivingId) {
        List<BuildModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (BuildModel buildModel : list) {
            if (buildModel.getReleaseMethod() == BuildModel.ReleaseMethod.Outgiving.getCode() &&
                    outGivingId.equals(buildModel.getReleaseMethodDataId())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkNode(String nodeId) {
        List<BuildModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (BuildModel buildModel : list) {
            if (buildModel.getReleaseMethod() == BuildModel.ReleaseMethod.Project.getCode()) {
                String releaseMethodDataId = buildModel.getReleaseMethodDataId();
                if (StrUtil.startWith(releaseMethodDataId, nodeId + ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkNodeProjectId(String nodeId, String projectId) {
        List<BuildModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (BuildModel buildModel : list) {
            if (buildModel.getReleaseMethod() == BuildModel.ReleaseMethod.Project.getCode()) {
                String releaseMethodDataId = buildModel.getReleaseMethodDataId();
                if (StrUtil.equals(releaseMethodDataId, nodeId + ":" + projectId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<BuildModel> list() {
        return (List<BuildModel>) filter(super.list(), ClassFeature.BUILD);
    }

    @Override
    public JSONArray listToArray(String dataId) {
        return (JSONArray) JSONArray.toJSON(this.list());
    }
}
