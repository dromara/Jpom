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
package org.dromara.jpom.common.forward;

import lombok.Getter;

/**
 * agent 端的请求地址枚举
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Getter
public enum NodeUrl {
    /**
     * Jpom agent 信息
     */
    Info("/info"),
    /**
     *
     */
//    GetTop("/getTop"),
    GetStatInfo("/get-stat-info"),
    exportTop("/exportTop"),
    Kill("/kill.json"),
    DiskInfo("/disk-info"),
    HwDiskInfo("/hw-disk--info"),

    NetworkInterfaces("/network-interfaces"),

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
     * 自由脚本
     */
    FreeScriptRun("/free-script-run"),
    /**
     * 系统日志
     */
    Socket_SystemLog("/system_log"),
    /**
     * 节点升级
     */
    NodeUpdate("/node_update"),

    WhitelistDirectory_Submit("/system/whitelistDirectory_submit"),

    WhitelistDirectory_data("/system/whitelistDirectory_data"),

    Manage_SaveProject("/manage/saveProject"),

    Manage_DeleteProject("/manage/deleteProject"),

    Manage_ReleaseOutGiving("/manage/releaseOutGiving"),
    Manage_ChangeWorkspaceId("/manage/change-workspace-id"),

    Manage_GetProjectInfo("/manage/getProjectInfo"),

//    Manage_Jude_Lib("/manage/judge_lib.json"),

//	Manage_GetProjectGroup("/manage/getProjectGroup"),

    Manage_GetProjectItem("/manage/getProjectItem"),

    Manage_GetProjectStatus("/manage/getProjectStatus"),

    Manage_Operate("/manage/operate"),

    Manage_GetProjectPort("/manage/getProjectPort"),


    Manage_Recover_List_Data("/manage/recover/list_data"),

    Manage_Recover_Item_Data("/manage/recover/item_data"),
    Manage_File_GetFileList("/manage/file/getFileList"),
    MANAGE_FILE_BACKUP_LIST_BACKUP("/manage/file/list-backup"),
    MANAGE_FILE_BACKUP_LIST_ITEM_FILES("/manage/file/backup-item-files"),
    MANAGE_FILE_BACKUP_DOWNLOAD("/manage/file/backup-download", true),
    MANAGE_FILE_BACKUP_DELETE("/manage/file/backup-delete"),
    MANAGE_FILE_BACKUP_RECOVER("/manage/file/backup-recover"),
    Manage_File_Upload_Sharding("/manage/file/upload-sharding", true),
    Manage_File_Sharding_Merge("/manage/file/sharding-merge", true),
    Manage_File_Upload_Sharding2("/manage/file2/upload-sharding", true),
    Manage_File_Sharding_Merge2("/manage/file2/sharding-merge", true),

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

    Manage_File_Remote_Download("/manage/file/remote_download", true),
    MANAGE_FILE_NEW_FILE_FOLDER("/manage/file/new_file_folder.json"),
    MANAGE_FILE_RENAME_FILE_FOLDER("/manage/file/rename.json"),

    Manage_File_Download("/manage/file/download", true),


    Manage_Log_LogSize("/manage/log/logSize"),

    Manage_Log_ResetLog("/manage/log/resetLog"),

    Manage_Log_logBack_delete("/manage/log/logBack_delete"),

    Manage_Log_logBack_download("/manage/log/logBack_download", true),

    Manage_Log_logBack("/manage/log/logBack"),

    Manage_Log_export("/manage/log/export", true),


    Script_List("/script/list.json"),
    SCRIPT_PULL_EXEC_LOG("/script/pull_exec_log"),
    SCRIPT_DEL_EXEC_LOG("/script/del_exec_log"),
    Script_Item("/script/item.json"),
    Script_Save("/script/save.json"),
    SCRIPT_LOG("/script/log"),
    SCRIPT_EXEC("/script/exec"),
    SCRIPT_DEL_LOG("/script/del_log"),
    //    Script_Upload("/script/upload.json"),
    Script_Del("/script/del.json"),

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

    DownloadSystemLog("/system/log_download", true),
    /**
     * 更新系统jar包
     */
    SystemUploadJar("/system/upload-jar-sharding", true),
    /**
     * 更新系统jar包
     */
    SystemUploadJarMerge("/system/upload-jar-sharding-merge", true),
    /**
     * 更新系统jar包
     */
    CHECK_VERSION("/system/check_version.json"),
    /**
     * 远程升级
     */
    REMOTE_UPGRADE("/system/remote_upgrade.json", true),
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
    private int timeout;
    private boolean fileTimeout = false;

    NodeUrl(String url, int timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    NodeUrl(String url, boolean fileTimeout) {
        this.url = url;
        this.fileTimeout = fileTimeout;
    }

    NodeUrl(String url) {
        this.url = url;
    }
}
