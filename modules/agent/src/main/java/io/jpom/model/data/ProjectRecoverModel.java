package io.jpom.model.data;

import cn.hutool.core.lang.ObjectId;
import io.jpom.model.BaseModel;

/**
 * 项目回收记录实体
 *
 * @author jiangzeyin
 */
public class ProjectRecoverModel extends BaseModel {
    /**
     * 删除人
     */
    private String delUser;
    /**
     * 删除时间
     */
    private String delTime;
    /**
     * 删除的对应项目信息
     */
    private ProjectInfoModel projectInfoModel;

    public ProjectRecoverModel(ProjectInfoModel projectInfoModel) {
        this.projectInfoModel = projectInfoModel;
        // 生成操作id
        setId(ObjectId.next());
    }

    public ProjectRecoverModel() {
    }

    public ProjectInfoModel getProjectInfoModel() {
        return projectInfoModel;
    }

    public void setProjectInfoModel(ProjectInfoModel projectInfoModel) {
        this.projectInfoModel = projectInfoModel;
    }

    public String getDelUser() {
        return delUser;
    }

    public void setDelUser(String delUser) {
        this.delUser = delUser;
    }

    public String getDelTime() {
        return delTime;
    }

    public void setDelTime(String delTime) {
        this.delTime = delTime;
    }
}
