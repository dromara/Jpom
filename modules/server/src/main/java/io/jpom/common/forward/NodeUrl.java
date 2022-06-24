/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common.forward;

import io.jpom.system.ServerExtConfigBean;

/**
 * agent 端的请求地址枚举
 *
 * @author jiangzeyin
 * @since 2019/4/16
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
    GetDirectTop("/getDirectTop"),
    Status("/status"),
    exportTop("/exportTop"),
    Kill("/kill.json"),

    ProcessList("/processList", -1),
    /**
     * socket 连接  ，第一节项目id 第二节用户信息
     */
    TopSocket("/console"),
    /**
     * 脚本模板  模板id
     */
    Script_Run("/script_run"),
    /**
     * Tomcat
     */
    Tomcat_Socket("/tomcat_log"),
    /**
     * 节点升级
     */
    NodeUpdate("/node_update"),

    WhitelistDirectory_Submit("/system/whitelistDirectory_submit"),

    WhitelistDirectory_data("/system/whitelistDirectory_data"),

    Manage_SaveProject("/manage/saveProject"),

    Manage_DeleteProject("/manage/deleteProject"),

    Manage_ReleaseOutGiving("/manage/releaseOutGiving"),

    Manage_GetProjectInfo("/manage/getProjectInfo"),

    Manage_Jude_Lib("/manage/judge_lib.json"),

//	Manage_GetProjectGroup("/manage/getProjectGroup"),

    Manage_GetProjectItem("/manage/getProjectItem"),

    Manage_GetProjectStatus("/manage/getProjectStatus"),

    Manage_Restart("/manage/restart"),

    Manage_Start("/manage/start"),

    Manage_Stop("/manage/stop"),

    Manage_GetProjectPort("/manage/getProjectPort"),

    Manage_GetProjectCopyPort("/manage/getProjectCopyPort"),

    Manage_ProjectCopyList("/manage/project_copy_list"),

    Manage_Recover_List_Data("/manage/recover/list_data"),

    Manage_Recover_Item_Data("/manage/recover/item_data"),
    Manage_File_GetFileList("/manage/file/getFileList"),
    MANAGE_FILE_BACKUP_LIST_BACKUP("/manage/file/list-backup"),
    MANAGE_FILE_BACKUP_LIST_ITEM_FILES("/manage/file/backup-item-files"),
    MANAGE_FILE_BACKUP_DOWNLOAD("/manage/file/backup-download"),
    MANAGE_FILE_BACKUP_DELETE("/manage/file/backup-delete"),
    MANAGE_FILE_BACKUP_RECOVER("/manage/file/backup-recover"),
    /**
     * jzy add  timeout
     */
    Manage_File_Upload("/manage/file/upload", ServerExtConfigBean.getInstance().getUploadFileTimeOut()),

    Manage_File_DeleteFile("/manage/file/deleteFile"),
    /**
     * 对比项目文件
     */
    MANAGE_FILE_DIFF_FILE("/manage/file/diff_file"),
    /**
     * 批量删除文件
     */
    MANAGE_FILE_BATCH_DELETE("/manage/file/batch_delete"),

    Manage_File_UpdateConfigFile("/manage/file/update_config_file"),

    Manage_File_ReadFile("/manage/file/read_file"),

    Manage_File_Remote_Download("/manage/file/remote_download"),
    MANAGE_FILE_NEW_FILE_FOLDER("/manage/file/new_file_folder.json"),
    MANAGE_FILE_RENAME_FILE_FOLDER("/manage/file/rename.json"),

    Manage_File_Download("/manage/file/download"),


    Manage_Log_LogSize("/manage/log/logSize"),

    Manage_Log_ResetLog("/manage/log/resetLog"),

    Manage_Log_logBack_delete("/manage/log/logBack_delete"),

    Manage_Log_logBack_download("/manage/log/logBack_download"),

    Manage_Log_logBack("/manage/log/logBack"),

    Manage_Log_export("/manage/log/export.html"),

    Manage_internal_data("/manage/internal_data"),
    Manage_internal_stack("/manage/internal_stack"),
    Manage_internal_ram("/manage/internal_ram"),
    Manage_internal_threadInfos("/manage/threadInfos"),

    /**
     * jdk
     */
    Manage_jdk_list("/manage/jdk/list"),
    Manage_jdk_update("/manage/jdk/update"),
    Manage_jdk_delete("/manage/jdk/delete"),

    System_Nginx_list_data("/system/nginx/list_data.json"),
    System_Nginx_Tree("/system/nginx/tree.json"),

    System_Nginx_item_data("/system/nginx/item_data"),

    System_Nginx_updateNgx("/system/nginx/updateNgx"),

    System_Nginx_delete("/system/nginx/delete"),

    System_Nginx_status("/system/nginx/status"),
    System_Nginx_config("/system/nginx/config"),
    System_Nginx_open("/system/nginx/open"),
    System_Nginx_close("/system/nginx/close"),
    System_Nginx_updateConf("/system/nginx/updateConf"),
    System_Nginx_reload("/system/nginx/reload"),

    System_Certificate_saveCertificate("/system/certificate/saveCertificate"),
    System_Certificate_getCertList("/system/certificate/getCertList"),
    System_Certificate_delete("/system/certificate/delete"),
    System_Certificate_export("/system/certificate/export"),

    Script_List("/script/list.json"),
    SCRIPT_PULL_EXEC_LOG("/script/pull_exec_log"),
    SCRIPT_DEL_EXEC_LOG("/script/del_exec_log"),
    Script_Item("/script/item.json"),
    Script_Save("/script/save.json"),
    SCRIPT_LOG("/script/log"),
    SCRIPT_DEL_LOG("/script/del_log"),
    Script_Upload("/script/upload.json"),
    Script_Del("/script/del.json"),

    Tomcat_List("/tomcat/list"),
    Tomcat_Add("/tomcat/add"),
    Tomcat_Update("/tomcat/update"),
    Tomcat_Delete("/tomcat/delete"),
    Tomcat_Start("/tomcat/start"),
    Tomcat_Stop("/tomcat/stop"),
    Tomcat_Restart("/tomcat/restart"),
    Tomcat_GetItem("/tomcat/getItem"),
    Tomcat_LOG_List("/tomcat/logList"),
    Tomcat_GetTomcatProjectList("/tomcat/getTomcatProjectList"),
    Tomcat_GetTomcatStatus("/tomcat/getTomcatStatus"),
    Tomcat_TomcatProjectManage("/tomcat/tomcatProjectManage"),
    Tomcat_File_GetFileList("/tomcat/getFileList"),
    Tomcat_File_DeleteFile("/tomcat/deleteFile"),
    Tomcat_File_Download("/tomcat/download"),
    Tomcat_File_Upload("/tomcat/upload"),
    Tomcat_File_UploadWar("/tomcat/uploadWar"),

    /**
     * Workspace
     */
    Workspace_EnvVar_Update("/system/workspace_env/update"),
    Workspace_EnvVar_Delete("/system/workspace_env/delete"),

    /**
     * 缓存
     */
    Cache("/system/cache"),
    /**
     * 缓存
     */
    ClearCache("/system/clearCache"),
    /**
     * 系统日志
     */
    SystemLog("/system/log_data.json"),

    DelSystemLog("/system/log_del.json"),

    DownloadSystemLog("/system/log_download"),
    /**
     * 更新系统jar包
     */
    SystemUploadJar("/system/uploadJar.json"),
    /**
     * 更新系统jar包
     */
    CHECK_VERSION("/system/check_version.json"),
    /**
     * 远程升级
     */
    REMOTE_UPGRADE("/system/remote_upgrade.json"),
    CHANGE_LOG("/system/change_log"),

    /**
     *
     */
    SystemGetConfig("/system/getConfig.json"),
    SystemSaveConfig("/system/save_config.json"),
    ;
    /**
     * 相对请求地址
     */
    private final String url;
    private int timeOut;

    public String getUrl() {
        return url;
    }

    public int getTimeOut() {
        return timeOut;
    }

    NodeUrl(String url, int timeOut) {
        this.url = url;
        this.timeOut = timeOut;
    }

    NodeUrl(String url) {
        this.url = url;
    }
}
