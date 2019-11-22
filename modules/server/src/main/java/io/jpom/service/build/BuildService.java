package io.jpom.service.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.build.BuildManage;
import io.jpom.common.BaseOperService;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    public <E> List<E> list(Class<E> cls) {
        List<E> list = super.list(cls);
        JSONArray jsonArray = ((JSONArray) JSONArray.toJSON(list));
        jsonArray = filter(jsonArray, ClassFeature.BUILD);
        if (jsonArray == null) {
            return null;
        }
        return jsonArray.toJavaList(cls);
    }

    @Override
    public JSONArray listToArray(String dataId) {
        return (JSONArray) JSONArray.toJSON(this.list());
    }

    /**
     * 开始构建
     *
     * @param userModel 用户
     * @param id        id
     * @return json
     */
    public String start(UserModel userModel, String id) {
        BuildModel item = getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        String e = checkStatus(item.getStatus());
        if (e != null) {
            return e;
        }
        //
        item.setBuildId(item.getBuildId() + 1);
        String optUserName = userModel == null ? "openApi" : UserModel.getOptUserName(userModel);
        item.setModifyUser(optUserName);
        this.updateItem(item);
        BuildManage.create(item, userModel);
        return JsonMessage.getString(200, "开始构建中", item.getBuildId());
    }

    public String checkStatus(int status) {
        BuildModel.Status nowStatus = BaseEnum.getEnum(BuildModel.Status.class, status);
        Objects.requireNonNull(nowStatus);
        if (BuildModel.Status.Ing == nowStatus ||
                BuildModel.Status.PubIng == nowStatus) {
            return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
        }
        return null;
    }

    /**
     * 构建所有分组
     */
    public Set<String> listGroup() {
        List<BuildModel> list = list();
        Set<String> set = new HashSet<>();
        if (CollUtil.isEmpty(list)) {
            return set;
        }
        for (BuildModel buildModel : list) {
            String group = buildModel.getGroup();
            if (StrUtil.isNotEmpty(group)) {
                set.add(group);
            }
        }
        return set;
    }
}
