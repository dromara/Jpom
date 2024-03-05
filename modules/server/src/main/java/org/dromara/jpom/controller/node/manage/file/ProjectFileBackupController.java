/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.manage.file;


import cn.keepbx.jpom.IJsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author bwcx_jzy
 * @since 2022/5/11
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectFileBackupController extends BaseServerController {

    /**
     * 查询备份列表
     *
     * @param id 项目ID
     * @return list
     */
    @RequestMapping(value = "list-backup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> listBackup(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_LIST_BACKUP);
    }

    /**
     * 获取指定备份的文件列表
     *
     * @param id       项目
     * @param path     读取的二级目录
     * @param backupId 备份id
     * @return list
     */
    @RequestMapping(value = "backup-item-files", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> backupItemFiles(String id, String path, @ValidatorItem String backupId, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_LIST_ITEM_FILES);
    }

    /**
     * 将执行文件下载到客户端 本地
     *
     * @param id        项目id
     * @param filename  文件名
     * @param levelName 文件夹名
     * @param backupId  备份id
     */
    @GetMapping(value = "backup-download", produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName,
                         HttpServletResponse response,
                         HttpServletRequest request) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.MANAGE_FILE_BACKUP_DOWNLOAD);
    }

    /**
     * 删除文件
     *
     * @param id        项目ID
     * @param backupId  备份ID
     * @param filename  文件名
     * @param levelName 层级目录
     * @return msg
     */
    @RequestMapping(value = "backup-delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> deleteFile(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_DELETE);
    }

    /**
     * 还原项目文件
     *
     * @param id        项目ID
     * @param backupId  备份ID
     * @param type      类型 clear 清空还原
     * @param filename  文件名
     * @param levelName 目录
     * @return msg
     */
    @RequestMapping(value = "backup-recover", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> recoverFile(String id, @ValidatorItem String backupId, String type, String filename, String levelName, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_BACKUP_RECOVER);
    }
}
