package cn.keepbx.jpom.common.forward;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public enum NodeUrl {
    /**
     *
     */
    GetTop("/getTop"),

    ProcessList("/processList"),

    TopSocket("/console/system"),

    WhitelistDirectory_Submit("/system/whitelistDirectory_submit"),

    WhitelistDirectory_data("/system/whitelistDirectory_data"),

    Manage_SaveProject("/manage/saveProject"),

    Manage_DeleteProject("/manage/deleteProject"),

    Manage_GetProjectInfo("/manage/getProjectInfo"),

    Manage_Jude_Lib("/manage/judge_lib.json"),

    Manage_GetProjectGroup("/manage/getProjectGroup"),

    Manage_GetProjectItem("/manage/getProjectItem"),


    Manage_GetProjectPort("/manage/getProjectPort"),

    Manage_Recover_List_Data("/manage/recover/list_data"),

    Manage_Recover_Item_Data("/manage/recover/item_data"),

    ;

    private String url;

    public String getUrl() {
        return url;
    }

    NodeUrl(String url) {
        this.url = url;
    }
}
