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
package io.jpom.controller.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.script.ProjectFileBackupUtil;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目备份文件管理
 *
 * @author bwcx_jzy
 * @since 2022/5/11
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileBackupController extends BaseAgentController {

    /**
     * 查询备份列表
     *
     * @param id 项目ID
     * @return list
     */
    @RequestMapping(value = "list-backup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String listBackup(String id) {
        //
        super.getProjectInfoModel(id);
        File path = ProjectFileBackupUtil.path(id);
        //
        List<File> collect = Arrays.stream(Optional.ofNullable(path.listFiles()).orElse(new File[0]))
            .filter(FileUtil::isDirectory)
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(collect)) {
            return JsonMessage.getString(200, "查询成功", Collections.EMPTY_LIST);
        }
        List<JSONObject> arrayFile = FileUtils.parseInfo(collect, true, path.getAbsolutePath());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("path", FileUtil.getAbsolutePath(path));
        jsonObject.put("list", arrayFile);
        return JsonMessage.getString(200, "查询成功", jsonObject);
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
    public String backupItemFiles(String id, String path, @ValidatorItem String backupId) {
        // 查询项目路径
        super.getProjectInfoModel();

        File lib = ProjectFileBackupUtil.path(id, backupId);
        File fileDir = FileUtil.file(lib, StrUtil.emptyToDefault(path, FileUtil.FILE_SEPARATOR));
        //
        File[] filesAll = FileUtil.exist(fileDir) ? fileDir.listFiles() : new File[]{};
        if (ArrayUtil.isEmpty(filesAll)) {
            return JsonMessage.getString(200, "查询成功", Collections.EMPTY_LIST);
        }
        List<JSONObject> arrayFile = FileUtils.parseInfo(filesAll, false, lib.getAbsolutePath());
        return JsonMessage.getString(200, "查询成功", arrayFile);
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
    public void download(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName) {
        HttpServletResponse response = getResponse();
        try {
            super.getProjectInfoModel();
            File lib = ProjectFileBackupUtil.path(id, backupId);
            File file = FileUtil.file(lib, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
            if (file.isDirectory()) {
                ServletUtil.write(response, "暂不支持下载文件夹", MediaType.TEXT_HTML_VALUE);
                return;
            }
            ServletUtil.write(response, file);
        } catch (Exception e) {
            log.error("下载文件异常", e);
            ServletUtil.write(response, "下载文件异常:" + e.getMessage(), MediaType.TEXT_HTML_VALUE);
        }
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
    public String deleteFile(String id, @ValidatorItem String backupId, @ValidatorItem String filename, String levelName) {
        super.getProjectInfoModel();
        File lib = ProjectFileBackupUtil.path(id, backupId);
        File file = FileUtil.file(lib, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
        CommandUtil.systemFastDel(file);
        return JsonMessage.getString(200, "删除成功");
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
    public String recoverFile(String id, @ValidatorItem String backupId, String type, String filename, String levelName) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel();
        File backupPath = ProjectFileBackupUtil.path(id, backupId);
        String projectPath = projectInfoModel.allLib();
        //
        File backupFile;
        File projectFile;
        if (StrUtil.isEmpty(filename)) {
            // 目录
            backupFile = FileUtil.file(backupPath, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR));
            Assert.state(FileUtil.exist(backupFile), "对应的文件不存在");
            projectFile = FileUtil.file(projectPath, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR));
            // 创建文件
            FileUtil.mkdir(projectFile);
            // 清空
            if (StrUtil.equalsIgnoreCase(type, "clear")) {
                FileUtil.clean(projectFile);
            }
            //
            FileUtil.copyContent(backupFile, projectFile, true);
        } else {
            // 文件
            backupFile = FileUtil.file(backupPath, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
            Assert.state(FileUtil.exist(backupFile), "对应的文件不存在");
            projectFile = FileUtil.file(projectPath, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
            FileUtil.copy(backupFile, projectFile, true);
        }
        return JsonMessage.getString(200, "还原成功");
    }


}
