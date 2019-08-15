package cn.keepbx.jpom.service.build;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 构建service
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 **/
@Service
public class BuildService extends BaseOperService<BuildModel> {

    public BuildService() {
        super(ServerConfigBean.BUILD);
    }

    @Override
    public void updateItem(BuildModel buildModel) {
        buildModel.setModifyTime(DateUtil.now());
        super.updateItem(buildModel);
    }

    public boolean checkOutGiving(String outGivingId) throws IOException {
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

    public boolean checkNode(String nodeId) throws IOException {
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

    public boolean checkNodeProjectId(String nodeId, String projectId) throws IOException {
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
}
