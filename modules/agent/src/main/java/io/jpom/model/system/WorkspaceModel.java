package io.jpom.model.system;

import io.jpom.model.BaseJsonModel;
import io.jpom.model.BaseModel;

import java.util.List;

public class WorkspaceModel extends BaseModel {

	private String workspaceId;

	private List<WorkspaceEnvVarModel> list;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public List<WorkspaceEnvVarModel> getList() {
        return list;
    }

    public void setList(List<WorkspaceEnvVarModel> list) {
        this.list = list;
    }
}
