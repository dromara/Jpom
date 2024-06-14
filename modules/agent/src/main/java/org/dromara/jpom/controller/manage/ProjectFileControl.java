/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.archiver.Archiver;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.controller.manage.vo.DiffFileVo;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.ProjectFileBackupService;
import org.dromara.jpom.service.WhitelistDirectoryService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.CompressionFileUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目文件管理
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileControl extends BaseAgentController {

    private final WhitelistDirectoryService whitelistDirectoryService;
    private final AgentConfig agentConfig;
    private final ProjectFileBackupService projectFileBackupService;
    private final ProjectCommander projectCommander;

    public ProjectFileControl(WhitelistDirectoryService whitelistDirectoryService,
                              AgentConfig agentConfig,
                              ProjectFileBackupService projectFileBackupService,
                              ProjectCommander projectCommander) {
        this.whitelistDirectoryService = whitelistDirectoryService;
        this.agentConfig = agentConfig;
        this.projectFileBackupService = projectFileBackupService;
        this.projectCommander = projectCommander;
    }

    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> getFileList(String id, String path) {
        // 查询项目路径
        NodeProjectInfoModel pim = getProjectInfoModel();
        String lib = projectInfoService.resolveLibPath(pim);
        File fileDir = FileUtil.file(lib, StrUtil.emptyToDefault(path, FileUtil.FILE_SEPARATOR));
        boolean exist = FileUtil.exist(fileDir);
        if (!exist) {
            return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), Collections.emptyList());
        }
        //
        File[] filesAll = fileDir.listFiles();
        if (ArrayUtil.isEmpty(filesAll)) {
            return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), Collections.emptyList());
        }
        boolean disableScanDir = pim.isDisableScanDir();
        List<JSONObject> arrayFile = FileUtils.parseInfo(filesAll, false, lib, disableScanDir);
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        for (JSONObject jsonObject : arrayFile) {
            String filename = jsonObject.getString("filename");
            jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(whitelist.getAllowEditSuffix(), filename));
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), arrayFile);
    }

    /**
     * 对比文件
     *
     * @param diffFileVo 参数
     * @return json
     */
    @PostMapping(value = "diff_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> diffFile(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        //
        List<DiffFileVo.DiffItem> data = diffFileVo.getData();
        Assert.notEmpty(data, I18nMessageUtil.get("i18n.comparison_data_not_found.413e"));
        // 扫描项目目录下面的所有文件
        File lib = projectInfoService.resolveLibFile(projectInfoModel);
        String path = FileUtil.file(lib, Opt.ofBlankAble(diffFileVo.getDir()).orElse(StrUtil.SLASH)).getAbsolutePath();
        List<File> files = FileUtil.loopFiles(path);
        // 将所有的文件信息组装并签名
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject item = new JSONObject();
            item.put("name", StringUtil.delStartPath(file, path, true));
            item.put("sha1", SecureUtil.sha1(file));
            return item;
        }).collect(Collectors.toList());
        // 得到 当前下面文件夹下面所有的文件信息 map
        Map<String, String> nowMap = CollStreamUtil.toMap(collect,
            jsonObject12 -> jsonObject12.getString("name"),
            jsonObject1 -> jsonObject1.getString("sha1"));
        // 将需要对应的信息转为 map
        Map<String, String> tryMap = CollStreamUtil.toMap(data, DiffFileVo.DiffItem::getName, DiffFileVo.DiffItem::getSha1);
        // 对应需要 当前项目文件夹下没有的和文件内容有变化的
        List<JSONObject> canSync = tryMap.entrySet()
            .stream()
            .filter(stringStringEntry -> {
                String nowSha1 = nowMap.get(stringStringEntry.getKey());
                if (StrUtil.isEmpty(nowSha1)) {
                    // 不存在
                    return true;
                }
                // 如果 文件信息一致 则过滤
                return !StrUtil.equals(stringStringEntry.getValue(), nowSha1);
            })
            .map(stringStringEntry -> {
                //
                JSONObject item = new JSONObject();
                item.put("name", stringStringEntry.getKey());
                item.put("sha1", stringStringEntry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        // 对比项目文件夹下有对，但是需要对应对信息里面没有对。此类文件需要删除
        List<JSONObject> delArray = nowMap.entrySet()
            .stream()
            .filter(stringStringEntry -> !tryMap.containsKey(stringStringEntry.getKey()))
            .map(stringStringEntry -> {
                //
                JSONObject item = new JSONObject();
                item.put("name", stringStringEntry.getKey());
                item.put("sha1", stringStringEntry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        //
        JSONObject result = new JSONObject();
        result.put("diff", canSync);
        result.put("del", delArray);
        return JsonMessage.success("", result);
    }


    private void saveProjectFileBefore(File lib, NodeProjectInfoModel projectInfoModel) throws Exception {
        String closeFirstStr = getParameter("closeFirst");
        // 判断是否需要先关闭项目
        boolean closeFirst = BooleanUtil.toBoolean(closeFirstStr);
        if (closeFirst) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.stop, projectInfoModel);
            Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.close_project_failure.a1d2") + result.msgStr());
        }
        String clearType = getParameter("clearType");
        // 判断是否需要清空
        if ("clear".equalsIgnoreCase(clearType)) {
            CommandUtil.systemFastDel(lib);
        }
    }

    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<CommandOpResult> uploadSharding(MultipartFile file,
                                                        String sliceId,
                                                        Integer totalSlice,
                                                        Integer nowSlice,
                                                        String fileSumMd5) throws Exception {
        String tempPathName = agentConfig.getFixedTempPathName();
        this.uploadSharding(file, tempPathName, sliceId, totalSlice, nowSlice, fileSumMd5);

        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    @RequestMapping(value = "sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<CommandOpResult> shardingMerge(String type,
                                                       String levelName,
                                                       Integer stripComponents,
                                                       String sliceId,
                                                       Integer totalSlice,
                                                       String fileSumMd5,
                                                       String after) throws Exception {
        String tempPathName = agentConfig.getFixedTempPathName();
        File successFile = this.shardingTryMerge(tempPathName, sliceId, totalSlice, fileSumMd5);
        // 处理上传文件
        return this.upload(successFile, type, levelName, stripComponents, after);
    }

    /**
     * 处理上传文件
     *
     * @param file            上传的文件
     * @param type            上传类型
     * @param levelName       文件夹
     * @param stripComponents 剔除文件夹
     * @param after           上传之后
     * @return 结果
     * @throws Exception 异常
     */
    private IJsonMessage<CommandOpResult> upload(File file, String type, String levelName, Integer stripComponents, String after) throws Exception {
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        File lib = StrUtil.isEmpty(levelName) ? libFile : FileUtil.file(libFile, levelName);
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        try {
            //
            this.saveProjectFileBefore(lib, pim);
            if ("unzip".equals(type)) {
                // 解压
                try {
                    int stripComponentsValue = Convert.toInt(stripComponents, 0);
                    CompressionFileUtil.unCompress(file, lib, stripComponentsValue);
                } finally {
                    if (!FileUtil.del(file)) {
                        log.error(I18nMessageUtil.get("i18n.delete_file_failure_with_full_stop.6c96") + file.getPath());
                    }
                }
            } else {
                // 移动文件到对应目录
                FileUtil.mkdir(lib);
                FileUtil.move(file, lib, true);
            }
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "upload", "levelName", levelName, "fileType", type, "fileName", file.getName());
            //
            JsonMessage<CommandOpResult> resultJsonMessage = this.saveProjectFileAfter(after, pim);
            if (resultJsonMessage != null) {
                return resultJsonMessage;
            }
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    private JsonMessage<CommandOpResult> saveProjectFileAfter(String after, NodeProjectInfoModel pim) throws Exception {
        if (StrUtil.isEmpty(after)) {
            return null;
        }
        log.debug(I18nMessageUtil.get("i18n.prepare_restart.8251"), pim.getId(), after);
        //
        AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(after, AfterOpt.No.getCode()));
        if ("restart".equalsIgnoreCase(after) || afterOpt == AfterOpt.Restart) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.restart, pim);

            return new JsonMessage<>(result.isSuccess() ? 200 : 405, I18nMessageUtil.get("i18n.upload_success_and_restart.7bc3"), result);
        } else if (afterOpt == AfterOpt.Order_Restart || afterOpt == AfterOpt.Order_Must_Restart) {
            CommandOpResult result = projectCommander.execCommand(ConsoleCommandOp.restart, pim);

            return new JsonMessage<>(result.isSuccess() ? 200 : 405, I18nMessageUtil.get("i18n.upload_success_and_restart.7bc3"), result);
        }
        return null;
    }

    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> deleteFile(String filename, String type, String levelName) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, StrUtil.SLASH));
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        try {
            if ("clear".equalsIgnoreCase(type)) {
                // 清空文件
                if (FileUtil.clean(file)) {
                    projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "delete", "levelName", levelName, "deleteType", type, "fileName", filename);
                    return JsonMessage.success(I18nMessageUtil.get("i18n.clear_success_message.51f4"));
                }
                boolean run = projectCommander.isRun(pim);
                Assert.state(!run, I18nMessageUtil.get("i18n.file_in_use_stop_project_first.a2c3"));
                return new JsonMessage<>(500, I18nMessageUtil.get("i18n.delete_failure_with_colon_and_full_stop.bc42") + file.getAbsolutePath());
            } else {
                // 删除文件
                Assert.hasText(filename, I18nMessageUtil.get("i18n.select_file_to_delete.33d6"));
                file = FileUtil.file(file, filename);
                if (FileUtil.del(file)) {
                    projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "delete", "levelName", levelName, "deleteType", type, "fileName", filename);
                    return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
                }
                return new JsonMessage<>(500, I18nMessageUtil.get("i18n.delete_failure.acf0"));
            }
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }


    @RequestMapping(value = "batch_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> batchDelete(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        String dir = diffFileVo.getDir();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        // 备份文件
        String backupId = projectFileBackupService.backup(projectInfoModel);
        try {
            //
            List<DiffFileVo.DiffItem> data = diffFileVo.getData();
            Assert.notEmpty(data, I18nMessageUtil.get("i18n.comparison_data_not_found.413e"));
            File libFile = projectInfoService.resolveLibFile(projectInfoModel);
            //
            File path = FileUtil.file(libFile, Opt.ofBlankAble(dir).orElse(StrUtil.SLASH));
            for (DiffFileVo.DiffItem datum : data) {
                File file = FileUtil.file(path, datum.getName());
                if (FileUtil.del(file)) {
                    continue;
                }
                return new JsonMessage<>(500, I18nMessageUtil.get("i18n.delete_failure_with_colon_and_full_stop.bc42") + file.getAbsolutePath());
            }
            projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "batch-delete", "levelName", dir);
            return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
        } finally {
            projectFileBackupService.checkDiff(projectInfoModel, backupId);
        }

    }

    /**
     * 读取文件内容 （只能处理文本文件）
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 读取的文件名
     * @return json
     */
    @PostMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> readFile(String filePath, String filename) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        String ymlString = FileUtil.readString(file, charset);
        return JsonMessage.success("", ymlString);
    }

    /**
     * copy
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 文件名
     * @return json
     */
    @PostMapping(value = "copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> copy(String filePath, String filename) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        int counter = 1;
        String baseName = FileUtil.mainName(file);
        String extension = FileUtil.extName(file);
        if (StrUtil.isNotEmpty(extension)) {
            extension = StrUtil.DOT + extension;
        } else {
            extension = StrUtil.EMPTY;
        }
        String newName;
        File targetFile;
        // 生成不冲突的新文件名
        do {
            newName = StrUtil.format("{}({}){}", baseName, counter, extension);
            targetFile = FileUtil.file(libFile, filePath, newName);
            counter++;
        } while (FileUtil.exist(targetFile));
        if (FileUtil.isDirectory(file)) {
            FileUtil.copyContent(file, targetFile, false);
        } else {
            FileUtil.copy(file, targetFile, false);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.copy_success.20a4"));
    }

    /**
     * compress
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 文件名
     * @return json
     */
    @PostMapping(value = "compress", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> compress(String filePath, String filename, String type) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        File libFile = projectInfoService.resolveLibFile(pim);
        File file = FileUtil.file(libFile, filePath, filename);
        Assert.state(FileUtil.isDirectory(file), I18nMessageUtil.get("i18n.select_folder_to_compress.915f"));
        String ext;
        if (StrUtil.equals(type, "zip")) {
            ext = ".zip";
        } else if (StrUtil.equals(type, "tar")) {
            ext = ".tar";
        } else if (StrUtil.equals(type, "tar.gz")) {
            ext = ".tar.gz";
        } else {
            return JsonMessage.fail(I18nMessageUtil.get("i18n.compression_type_not_supported.9dea") + type);
        }
        int counter = 0;
        String baseName = FileUtil.mainName(file);
        String newName;
        File targetFile;
        // 生成不冲突的新文件名
        do {
            if (counter == 0) {
                newName = StrUtil.format("{}{}", baseName, ext);
            } else {
                newName = StrUtil.format("{}({}){}", baseName, counter, ext);
            }
            targetFile = FileUtil.file(libFile, filePath, newName);
            counter++;
        } while (FileUtil.exist(targetFile));
        //
        try (Archiver archiver = CompressUtil.createArchiver(Charset.defaultCharset(), FileUtil.extName(targetFile), targetFile)) {
            archiver.add(file);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.compression_success.80b3"));
    }

    /**
     * 保存文件内容 （只能处理文本文件）
     *
     * @param filePath 相对项目文件的文件夹
     * @param filename 读取的文件名
     * @param fileText 文件内容
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> updateConfigFile(String filePath, String filename, String fileText) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        // 备份文件
        String backupId = projectFileBackupService.backup(pim);
        File libFile = projectInfoService.resolveLibFile(pim);
        try {
            FileUtil.writeString(fileText, FileUtil.file(libFile, filePath, filename), charset);
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "edit", "levelName", filePath, "fileName", filename);
            return JsonMessage.success(I18nMessageUtil.get("i18n.file_write_success.804a"));
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }


    /**
     * 将执行文件下载到客户端 本地
     *
     * @param id        项目id
     * @param filename  文件名
     * @param levelName 文件夹名
     */
    @GetMapping(value = "download", produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(String id, String filename, String levelName, HttpServletResponse response) {
        Assert.hasText(filename, I18nMessageUtil.get("i18n.select_file.9feb"));
//		String safeFileName = pathSafe(filename);
//		if (StrUtil.isEmpty(safeFileName)) {
//			return JsonMessage.getString(405, "非法操作");
//		}
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        try {
            File file = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
            if (file.isDirectory()) {
                ServletUtil.write(response, JsonMessage.getString(400, I18nMessageUtil.get("i18n.folder_download_not_supported.c3b7")), MediaType.APPLICATION_JSON_VALUE);
                return;
            }
            ServletUtil.write(response, file);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.download_exception.e616"), e);
            ServletUtil.write(response, JsonMessage.getString(400, I18nMessageUtil.get("i18n.download_failed_retry.c113"), e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     * 下载远程文件
     *
     * @param id              项目id
     * @param url             远程 url 地址
     * @param levelName       保存的文件夹
     * @param unzip           是否为压缩包、true 将自动解压
     * @param stripComponents 剔除层级
     * @return json
     */
    @PostMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> remoteDownload(String id, String url, String levelName, String unzip, Integer stripComponents) {
        Assert.hasText(url, I18nMessageUtil.get("i18n.correct_remote_address_required.0ce1"));
        NodeProjectInfoModel pim = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(pim);
        String tempPathName = agentConfig.getTempPathName();
        //
        String backupId = null;
        try {
            File downloadFile = HttpUtil.downloadFileFromUrl(url, tempPathName);
            String fileSize = FileUtil.readableFileSize(downloadFile);
            // 备份文件
            backupId = projectFileBackupService.backup(pim);
            File file = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR));
            FileUtil.mkdir(file);
            if (BooleanUtil.toBoolean(unzip)) {
                // 需要解压文件
                try {
                    int stripComponentsValue = Convert.toInt(stripComponents, 0);
                    CompressionFileUtil.unCompress(downloadFile, file, stripComponentsValue);
                } finally {
                    if (!FileUtil.del(downloadFile)) {
                        log.error(I18nMessageUtil.get("i18n.delete_file_failure_with_full_stop.6c96") + file.getPath());
                    }
                }
            } else {
                // 移动文件到对应目录
                FileUtil.move(downloadFile, file, true);
            }
            projectCommander.asyncWebHooks(pim, "fileChange", "changeEvent", "remoteDownload", "levelName", levelName, "fileName", file.getName(), "url", url);
            return JsonMessage.success(I18nMessageUtil.get("i18n.download_file_size.d4de") + fileSize);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.download_remote_file_exception.3ee0"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.download_remote_file_failed.fcc3") + e.getMessage());
        } finally {
            projectFileBackupService.checkDiff(pim, backupId);
        }
    }

    /**
     * 创建文件夹/文件
     *
     * @param id        项目ID
     * @param levelName 二级文件夹名
     * @param filename  文件名
     * @param unFolder  true/1 为文件夹，false/2 为文件
     * @return json
     */
    @PostMapping(value = "new_file_folder.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> newFileFolder(String id, String levelName, @ValidatorItem String filename, String unFolder) {
        NodeProjectInfoModel projectInfoModel = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(projectInfoModel);
        File file = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
        //
        Assert.state(!FileUtil.exist(file), I18nMessageUtil.get("i18n.folder_or_file_exists.c687"));
        boolean folder = !Convert.toBool(unFolder, false);
        if (folder) {
            FileUtil.mkdir(file);
        } else {
            FileUtil.touch(file);
        }
        projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "newFileOrFolder", "levelName", levelName, "fileName", filename, "folder", folder);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 修改文件夹/文件
     *
     * @param id        项目ID
     * @param levelName 二级文件夹名
     * @param filename  文件名
     * @param newname   新文件名
     * @return json
     */
    @PostMapping(value = "rename.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> rename(String id, String levelName, @ValidatorItem String filename, String newname) {
        NodeProjectInfoModel projectInfoModel = getProjectInfoModel();
        File libFile = projectInfoService.resolveLibFile(projectInfoModel);
        File file = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
        File newFile = FileUtil.file(libFile, StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), newname);

        Assert.state(FileUtil.exist(file), I18nMessageUtil.get("i18n.file_not_found.d952"));
        Assert.state(!FileUtil.exist(newFile), I18nMessageUtil.get("i18n.file_name_already_exists.0d4e"));

        FileUtil.rename(file, newname, false);
        projectCommander.asyncWebHooks(projectInfoModel, "fileChange", "changeEvent", "rename", "levelName", levelName, "fileName", filename, "newname", newname);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

}
