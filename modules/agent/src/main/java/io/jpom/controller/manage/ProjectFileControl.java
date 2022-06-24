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

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.controller.manage.vo.DiffFileVo;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.script.ProjectFileBackupUtil;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.service.manage.ConsoleService;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileControl extends BaseAgentController {

    private final ConsoleService consoleService;
    private final WhitelistDirectoryService whitelistDirectoryService;

    public ProjectFileControl(ConsoleService consoleService,
                              WhitelistDirectoryService whitelistDirectoryService) {
        this.consoleService = consoleService;
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFileList(String id, String path) {
        // 查询项目路径
        NodeProjectInfoModel pim = projectInfoService.getItem(id);
        Assert.notNull(pim, "查询失败：项目不存在");
        String lib = pim.allLib();
        File fileDir = FileUtil.file(lib, StrUtil.emptyToDefault(path, FileUtil.FILE_SEPARATOR));
        boolean exist = FileUtil.exist(fileDir);
        if (!exist) {
            return JsonMessage.getString(200, "查询成功", Collections.EMPTY_LIST);
        }
        //
        File[] filesAll = fileDir.listFiles();
        if (ArrayUtil.isEmpty(filesAll)) {
            return JsonMessage.getString(200, "查询成功", Collections.EMPTY_LIST);
        }
        List<JSONObject> arrayFile = FileUtils.parseInfo(filesAll, false, lib);
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        for (JSONObject jsonObject : arrayFile) {
            String filename = jsonObject.getString("filename");
            jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(whitelist.getAllowEditSuffix(), filename));
        }
        return JsonMessage.getString(200, "查询成功", arrayFile);
    }

    /**
     * 对比文件
     *
     * @param diffFileVo 参数
     * @return json
     */
    @PostMapping(value = "diff_file", produces = MediaType.APPLICATION_JSON_VALUE)
    public String diffFile(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        //
        List<DiffFileVo.DiffItem> data = diffFileVo.getData();
        Assert.notEmpty(data, "没有要对比的数据");
        // 扫描项目目录下面的所有文件
        String path = projectInfoModel.allLib();
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
        return JsonMessage.getString(200, "", result);
    }


    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String upload() throws Exception {
        NodeProjectInfoModel pim = getProjectInfoModel();
        MultipartFileBuilder multipartFileBuilder = createMultipart()
            .addFieldName("file")
            .setUseOriginalFilename(true);
        // 压缩文件
        String type = getParameter("type");
        // 是否清空
        String clearType = getParameter("clearType");
        String levelName = getParameter("levelName");
        File lib = StrUtil.isEmpty(levelName) ? new File(pim.allLib()) : FileUtil.file(pim.allLib(), levelName);
        // 备份文件
        String backupId = ProjectFileBackupUtil.backup(pim.getId(), pim.allLib());
        try {
            // 判断是否需要清空
            if ("clear".equalsIgnoreCase(clearType)) {
                CommandUtil.systemFastDel(lib);
            }
            if ("unzip".equals(type)) {
                multipartFileBuilder.setFileExt(StringUtil.PACKAGE_EXT);
                multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
                String path = multipartFileBuilder.save();
                // 解压
                File file = new File(path);
                try {
                    CompressionFileUtil.unCompress(file, lib);
                } finally {
                    if (!FileUtil.del(file)) {
                        log.error("删除文件失败：" + file.getPath());
                    }
                }
            } else {
                multipartFileBuilder.setSavePath(FileUtil.getAbsolutePath(lib));
                // 保存
                multipartFileBuilder.save();
            }
            // 修改使用状态
            projectInfoService.updateItem(pim);
            //
            String after = getParameter("after");
            if (StrUtil.isNotEmpty(after)) {
                //
                List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = pim.getJavaCopyItemList();
                //
                AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(after, AfterOpt.No.getCode()));
                if ("restart".equalsIgnoreCase(after) || afterOpt == AfterOpt.Restart) {
                    String result = consoleService.execCommand(ConsoleCommandOp.restart, pim, null);
                    // 自动处理副本集
                    if (javaCopyItemList != null) {
                        ThreadUtil.execute(() -> javaCopyItemList.forEach(javaCopyItem -> {
                            try {
                                consoleService.execCommand(ConsoleCommandOp.restart, pim, javaCopyItem);
                            } catch (Exception e) {
                                log.error("重启副本集失败", e);
                            }
                        }));
                    }
                    return JsonMessage.getString(200, "上传成功并重启：" + result);
                }
                if (afterOpt == AfterOpt.Order_Restart || afterOpt == AfterOpt.Order_Must_Restart) {
                    boolean restart = this.restart(pim, null, afterOpt);
                    if (javaCopyItemList != null) {
                        ThreadUtil.execute(() -> {
                            // 副本
                            for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
                                if (!this.restart(pim, javaCopyItem, afterOpt)) {
                                    return;
                                }
                                // 休眠30秒 等待之前项目正常启动
                                try {
                                    TimeUnit.SECONDS.sleep(30);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        });
                    }
                }
            }
        } finally {
            ProjectFileBackupUtil.checkDiff(pim.getId(), pim.allLib(), backupId, pim.dslConfig());
        }
        return JsonMessage.getString(200, "上传成功");
    }


    private boolean restart(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, AfterOpt afterOpt) {
        try {
            String result = consoleService.execCommand(ConsoleCommandOp.restart, nodeProjectInfoModel, javaCopyItem);
            int pid = ProjectCommanderUtil.parsePid(result);
            if (pid <= 0) {
                // 完整重启，不再继续剩余的节点项目
                return afterOpt != AfterOpt.Order_Must_Restart;
            }
            return true;
        } catch (Exception e) {
            log.error("重复失败", e);
            // 完整重启，不再继续剩余的节点项目
            return afterOpt != AfterOpt.Order_Must_Restart;
        }
    }

    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteFile(String filename, String type, String levelName) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        File file = FileUtil.file(pim.allLib(), StrUtil.emptyToDefault(levelName, StrUtil.SLASH));
//        if (StrUtil.isEmpty(levelName)) {
//            file = FileUtil.file(pim.allLib());
//        } else {
//            file = FileUtil.file(pim.allLib(), levelName);
//        }
        // 备份文件
        String backupId = ProjectFileBackupUtil.backup(pim.getId(), pim.allLib());
        try {
            if ("clear".equalsIgnoreCase(type)) {
                // 清空文件
                if (FileUtil.clean(file)) {
                    return JsonMessage.getString(200, "清除成功");
                }
                boolean run = AbstractProjectCommander.getInstance().isRun(pim, null);
                Assert.state(!run, "文件被占用，请先停止项目");
                return JsonMessage.getString(500, "删除失败：" + file.getAbsolutePath());
            } else {
                // 删除文件
                Assert.hasText(filename, "请选择要删除的文件");
                file = FileUtil.file(file, filename);

                if (file.exists()) {
                    if (FileUtil.del(file)) {
                        return JsonMessage.getString(200, "删除成功");
                    }
                } else {
                    return JsonMessage.getString(404, "文件不存在");
                }
                return JsonMessage.getString(500, "删除失败");
            }
        } finally {
            ProjectFileBackupUtil.checkDiff(pim.getId(), pim.allLib(), backupId, pim.dslConfig());
        }
    }


    @RequestMapping(value = "batch_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String batchDelete(@RequestBody DiffFileVo diffFileVo) {
        String id = diffFileVo.getId();
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        // 备份文件
        String backupId = ProjectFileBackupUtil.backup(projectInfoModel.getId(), projectInfoModel.allLib());
        try {
            //
            List<DiffFileVo.DiffItem> data = diffFileVo.getData();
            Assert.notEmpty(data, "没有要对比的数据");
            //
            String path = projectInfoModel.allLib();
            for (DiffFileVo.DiffItem datum : data) {
                File file = FileUtil.file(path, datum.getName());
                if (FileUtil.del(file)) {
                    continue;
                }
                return JsonMessage.getString(500, "删除失败");
            }
            return JsonMessage.getString(200, "删除成功");
        } finally {
            ProjectFileBackupUtil.checkDiff(projectInfoModel.getId(), projectInfoModel.allLib(), backupId, projectInfoModel.dslConfig());
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
    public String readFile(String filePath, String filename) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        File file = FileUtil.file(pim.allLib(), filePath, filename);
        String ymlString = FileUtil.readString(file, charset);
        return JsonMessage.getString(200, "", ymlString);
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
    public String updateConfigFile(String filePath, String filename, String fileText) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        filePath = StrUtil.emptyToDefault(filePath, File.separator);
        // 判断文件后缀
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Charset charset = AgentWhitelist.checkFileSuffix(whitelist.getAllowEditSuffix(), filename);
        // 备份文件
        String backupId = ProjectFileBackupUtil.backup(pim.getId(), pim.allLib());
        try {
            FileUtil.writeString(fileText, FileUtil.file(pim.allLib(), filePath, filename), charset);
            return JsonMessage.getString(200, "文件写入成功");
        } finally {
            ProjectFileBackupUtil.checkDiff(pim.getId(), pim.allLib(), backupId, pim.dslConfig());
        }
    }


    /**
     * 将执行文件下载到客户端 本地
     *
     * @param id        项目id
     * @param filename  文件名
     * @param levelName 文件夹名
     * @return 正常情况返回文件流，非正在返回 text plan
     */
    @GetMapping(value = "download", produces = MediaType.APPLICATION_JSON_VALUE)
    public String download(String id, String filename, String levelName) {
        Assert.hasText(filename, "请选择文件");
//		String safeFileName = pathSafe(filename);
//		if (StrUtil.isEmpty(safeFileName)) {
//			return JsonMessage.getString(405, "非法操作");
//		}
        try {
            NodeProjectInfoModel pim = projectInfoService.getItem(id);
            File file = FileUtil.file(pim.allLib(), StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
            if (file.isDirectory()) {
                return "暂不支持下载文件夹";
            }
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            log.error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    /**
     * 下载远程文件
     *
     * @param id        项目id
     * @param url       远程 url 地址
     * @param levelName 保存的文件夹
     * @param unzip     是否为压缩包、true 将自动解压
     * @return json
     */
    @PostMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    public String remoteDownload(String id, String url, String levelName, String unzip) {
        Assert.hasText(url, "请输入正确的远程地址");
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        Set<String> allowRemoteDownloadHost = whitelist.getAllowRemoteDownloadHost();
        Assert.state(CollUtil.isNotEmpty(allowRemoteDownloadHost), "还没有配置运行的远程地址");
        List<String> collect = allowRemoteDownloadHost.stream().filter(s -> StrUtil.startWith(url, s)).collect(Collectors.toList());
        Assert.state(CollUtil.isNotEmpty(collect), "不允许下载当前地址的文件");
        NodeProjectInfoModel pim = projectInfoService.getItem(id);
        // 备份文件
        String backupId = ProjectFileBackupUtil.backup(pim.getId(), pim.allLib());
        try {
            File file = FileUtil.file(pim.allLib(), StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR));
            File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
            if (BooleanUtil.toBoolean(unzip)) {
                // 需要解压文件
                try {
                    CompressionFileUtil.unCompress(downloadFile, file);
                } finally {
                    if (!FileUtil.del(downloadFile)) {
                        log.error("删除文件失败：" + file.getPath());
                    }
                }
            }
            return JsonMessage.getString(200, "下载成功文件大小：" + FileUtil.readableFileSize(downloadFile));
        } catch (Exception e) {
            log.error("下载远程文件异常", e);
            return JsonMessage.getString(500, "下载远程文件失败:" + e.getMessage());
        } finally {
            ProjectFileBackupUtil.checkDiff(pim.getId(), pim.allLib(), backupId, pim.dslConfig());
        }
    }

    /**
     * 创建文件夹/文件
     *
     * @param id        项目ID
     * @param levelName 二级文件夹名
     * @param filename  文件名
     * @param unFolder  true/1 为文件夹，false/0 为文件
     * @return json
     */
    @PostMapping(value = "new_file_folder.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String newFileFolder(String id, String levelName, @ValidatorItem String filename, String unFolder) {
        NodeProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        Assert.notNull(projectInfoModel, "没有对应到项目");
        File file = FileUtil.file(projectInfoModel.allLib(), StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
        //
        Assert.state(!FileUtil.exist(file), "文件夹或者文件已存在");
        if (Convert.toBool(unFolder, false)) {
            FileUtil.touch(file);
        } else {
            FileUtil.mkdir(file);
        }
        return JsonMessage.getString(200, "操作成功");
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
    public String rename(String id, String levelName, @ValidatorItem String filename, String newname) {
        NodeProjectInfoModel projectInfoModel = getProjectInfoModel();
        File file = FileUtil.file(projectInfoModel.allLib(), StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), filename);
        File newFile = FileUtil.file(projectInfoModel.allLib(), StrUtil.emptyToDefault(levelName, FileUtil.FILE_SEPARATOR), newname);

        Assert.state(FileUtil.exist(file), "文件不存在");
        Assert.state(!FileUtil.exist(newFile), "文件名已经存在拉");

        FileUtil.rename(file, newname, false);

        return JsonMessage.getString(200, "操作成功");
    }

}
