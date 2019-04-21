package cn.keepbx.jpom.common.forward;

/**
 * agent 端的请求地址枚举
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public enum NodeUrl {
    /**
     * Jpom agent 信息
     */
    Info("/info"),
    /**
     *
     */
    GetTop("/getTop"),
    Status("/status"),

    ProcessList("/processList"),
    /**
     * socket 连接  ，第一节项目id 第二节用户信息
     */
    TopSocket("/console/{}/{}"),

    WhitelistDirectory_Submit("/system/whitelistDirectory_submit"),

    WhitelistDirectory_data("/system/whitelistDirectory_data"),

    Manage_SaveProject("/manage/saveProject"),

    Manage_DeleteProject("/manage/deleteProject"),

    Manage_GetProjectInfo("/manage/getProjectInfo"),

    Manage_Jude_Lib("/manage/judge_lib.json"),

    Manage_GetProjectGroup("/manage/getProjectGroup"),

    Manage_GetProjectItem("/manage/getProjectItem"),
    Manage_GetProjectStatus("/manage/getProjectStatus"),

    Manage_GetRunModes("/manage/getRunModes"),


    Manage_GetProjectPort("/manage/getProjectPort"),

    Manage_Recover_List_Data("/manage/recover/list_data"),

    Manage_Recover_Item_Data("/manage/recover/item_data"),


    Manage_File_GetFileList("/manage/file/getFileList"),

    Manage_File_Upload("/manage/file/upload"),

    Manage_File_DeleteFile("/manage/file/deleteFile"),

    Manage_File_Download("/manage/file/download"),


    Manage_Log_LogSize("/manage/log/logSize"),

    Manage_Log_ResetLog("/manage/log/resetLog"),

    Manage_Log_logBack_delete("/manage/log/logBack_delete"),

    Manage_Log_logBack_download("/manage/log/logBack_download"),

    Manage_Log_logBack("/manage/log/logBack"),

    Manage_Log_export("/manage/log/export.html"),


    System_alioss_config("/system/alioss_config"),
    System_alioss_submit("/system/alioss_submit"),

    Manage_build_data("/manage/build_data"),
    Manage_build_download("/manage/build_download"),

    Manage_build_install("/manage/build_install"),

    Manage_internal_data("/manage/internal_data"),
    Manage_internal_stack("/manage/internal_stack"),
    Manage_internal_ram("/manage/internal_ram"),

    System_Nginx_list_data("/system/nginx/list_data.json"),

    System_Nginx_item_data("/system/nginx/item_data"),

    System_Nginx_updateNgx("/system/nginx/updateNgx"),

    System_Nginx_delete("/system/nginx/delete"),

    System_Certificate_saveCertificate("/system/certificate/saveCertificate"),

    System_Certificate_getCertList("/system/certificate/getCertList"),

    System_Certificate_delete("/system/certificate/delete"),

    System_Certificate_export("/system/certificate/export"),

    ;
    /**
     * 相对请求地址
     */
    private String url;

    public String getUrl() {
        return url;
    }

    NodeUrl(String url) {
        this.url = url;
    }
}
