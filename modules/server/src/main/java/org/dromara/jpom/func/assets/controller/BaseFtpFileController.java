/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BiFunction;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.ftp.FtpService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.CompressionFileUtil;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author wxyShine
 * @since 2025/05/28
 * 临时测试ftp服务器 https://sftpcloud.io/tools/free-ftp-server
 */
@Slf4j
public abstract class BaseFtpFileController extends BaseServerController {

    @Resource
    protected FtpService ftpService;
    @Resource
    protected MachineFtpServer machineFtpServer;
    @Resource
    private ServerConfig serverConfig;

    public interface ItemConfig {
        /**
         * 允许编辑的文件后缀
         *
         * @return 文件后缀
         */
        List<String> allowEditSuffix();

        /**
         * 允许管理的文件目录
         *
         * @return 文件目录
         */
        List<String> fileDirs();
    }

    /**
     * 验证数据id 和目录合法性
     *
     * @param id       数据id
     * @param function 回调
     * @param <T>      泛型
     * @return 处理后的数据
     */
    protected abstract <T> T checkConfigPath(String id, BiFunction<MachineFtpModel, ItemConfig, T> function);

    /**
     * 验证数据id 和目录合法性
     *
     * @param id              数据id
     * @param allowPathParent 想要验证的目录 （授权）
     * @param nextPath        授权后的二级路径
     * @param function        回调
     * @param <T>             泛型
     * @return 处理后的数据
     */
    protected abstract <T> T checkConfigPathChildren(String id, String allowPathParent, String nextPath, BiFunction<MachineFtpModel, ItemConfig, T> function);

    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(@ValidatorItem String id,
                         @ValidatorItem String allowPathParent,
                         @ValidatorItem String nextPath,
                         @ValidatorItem String name,
                         HttpServletResponse response) throws IOException {
        MachineFtpModel machineFtpModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel1, itemConfig) -> machineFtpModel1);
        if (machineFtpModel == null) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.ssh_error_or_folder_not_configured.c087"), MediaType.TEXT_HTML_VALUE);
            return;
        }
        this.downloadFile(machineFtpModel, allowPathParent, nextPath, name, response);
    }

    /**
     * 根据 id 获取 fileDirs 目录集合
     *
     * @param id ftp id
     * @return json
     * @author Hotstrip
     * @since for dev 3.x
     */
    @RequestMapping(value = "root_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONArray> rootFileList(@ValidatorItem String id) {
        //
        return this.checkConfigPath(id, (machineFtpModel, itemConfig) -> {
            JSONArray listDir = listRootDir(machineFtpModel, itemConfig.fileDirs());
            return JsonMessage.success("", listDir);
        });
    }


    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONArray> listData(@ValidatorItem String id,
                                            @ValidatorItem String allowPathParent,
                                            @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {
            try {
                JSONArray listDir = listDir(machineFtpModel, allowPathParent, nextPath, itemConfig);
                return JsonMessage.success("", listDir);
            } catch (SftpException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    @RequestMapping(value = "read_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<String> readFileData(@ValidatorItem String id,
                                             @ValidatorItem String allowPathParent,
                                             @ValidatorItem String nextPath,
                                             @ValidatorItem String name) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {

            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            //
            String content = this.readFile(machineFtpModel, allowPathParent, nextPath, name, charset);
            return JsonMessage.success("", content);
        });
    }

    @RequestMapping(value = "update_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> updateFileData(@ValidatorItem String id,
                                               @ValidatorItem String allowPathParent,
                                               @ValidatorItem String nextPath,
                                               @ValidatorItem String name,
                                               @ValidatorItem String content) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            // 缓存到本地
            File file = FileUtil.file(serverConfig.getUserTempPath(), machineFtpModel.getId(), allowPathParent, nextPath, name);
            try {
                FileUtil.writeString(content, file, charset);
                // 上传
                this.syncFile(machineFtpModel, allowPathParent, nextPath, name, file);
            } finally {
                //
                FileUtil.del(file);
            }
            //
            return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
        });
    }

    /**
     * 读取文件
     *
     * @param machineFtpModel ftp
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param charset         编码格式
     */
    private String readFile(MachineFtpModel machineFtpModel, String allowPathParent, String nextPath, String name, Charset charset) {
        String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);

        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
            EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active));
             InputStream inputStream = ftp.getClient().retrieveFileStream(normalize);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new RuntimeException("文件不存在或无法打开: " + normalize);
            }

            IoUtil.copy(inputStream, outputStream);
            ftp.getClient().completePendingCommand();

            return outputStream.toString(charset.name());

        } catch (IOException e) {
            throw new RuntimeException("FTP 读取文件失败", e);
        }
    }

    /**
     * 上传文件
     *
     * @param machineFtpModel ftp
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param file            同步上传文件
     */
    private void syncFile(MachineFtpModel machineFtpModel,
                          String allowPathParent,
                          String nextPath,
                          String name,
                          File file) {
        String normalizeDir = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath);

        // 通过 EnumUtil.fromString 获取枚举，默认 FtpMode.Active
        FtpMode ftpMode = EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active);

        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel), ftpMode)) {
            // 直接upload到原来目录 会覆盖更新文件
            boolean success = ftp.upload(normalizeDir, file);
            if (!success) {
                throw new RuntimeException("FTP 上传失败");
            }
        } catch (IOException e) {
            throw new RuntimeException("FTP 连接或操作异常", e);
        }
    }

    /**
     * 下载文件
     *
     * @param machineFtpModel ftp
     * @param allowPathParent 路径
     * @param name            文件
     * @param response        响应
     * @throws IOException   io
     * @throws SftpException sftp
     */
    private void downloadFile(MachineFtpModel machineFtpModel, String allowPathParent, String nextPath, String name, HttpServletResponse response) throws IOException {
        final String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        String fileName = FileUtil.getName(name);

        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename={}", URLUtil.encode(fileName, Charset.forName(charset))));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);

        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
            EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active));
             InputStream inputStream = ftp.getClient().retrieveFileStream(normalize)) {

            if (inputStream == null) {
                throw new RuntimeException("文件不存在或无法下载: " + normalize);
            }

            IoUtil.copy(inputStream, response.getOutputStream());
            ftp.getClient().completePendingCommand(); // 关键！

        } catch (IOException e) {
            throw new RuntimeException("FTP 下载文件失败", e);
        }
    }

    /**
     * 查询文件夹下所有文件
     *
     * @param ftpModel        ssh
     * @param allowPathParent 允许的路径
     * @param nextPath        下 N 级的文件夹
     * @return array
     * @throws SftpException sftp
     */
    @SuppressWarnings("unchecked")
    private JSONArray listDir(MachineFtpModel ftpModel, String allowPathParent, String nextPath, ItemConfig itemConfig) throws SftpException {

        List<String> allowEditSuffix = itemConfig.allowEditSuffix();
        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(ftpModel),
            EnumUtil.fromString(FtpMode.class, ftpModel.getMode(), FtpMode.Active))) {

            String children2 = StrUtil.emptyToDefault(nextPath, StrUtil.SLASH);
            String allPath = StrUtil.format("{}/{}", allowPathParent, children2);
            allPath = FileUtil.normalize(allPath);
            JSONArray jsonArray = new JSONArray();
            FTPFile[] ftpFiles;
            try {
                ftpFiles = ftp.lsFiles(allPath);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.get_folder_failure.0fda"), e);
                Throwable causedBy = ExceptionUtil.getCausedBy(e, SftpException.class);
                if (causedBy != null) {
                    throw new IllegalStateException("无法查询文件夹FTP，" + causedBy.getMessage());
                }
                throw new IllegalStateException(I18nMessageUtil.get("i18n.query_folder_failed.3f0e") + e.getMessage());
            }
            for (FTPFile file : ftpFiles) {
                String filename = file.getName();
                if (StrUtil.DOT.equals(filename) || StrUtil.DOUBLE_DOT.equals(filename)) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", filename);
                jsonObject.put("id", SecureUtil.sha1(allPath + StrUtil.SLASH + filename));

                Calendar timestamp = file.getTimestamp();
                timestamp.setTimeZone(TimeZone.getTimeZone("UTC"));
                jsonObject.put("modifyTime", timestamp);

                jsonObject.put("dir", file.isDirectory());
                jsonObject.put("size", file.getSize());
                jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(allowEditSuffix, filename));
                jsonObject.put("longname", file.toFormattedString(TimeZone.getDefault().toString()));
                jsonObject.put("link", file.getLink());
                jsonObject.put("permissions", getPermissionString(file));
                jsonObject.put("allowPathParent", allowPathParent);
                jsonObject.put("nextPath", FileUtil.normalize(children2));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 列出目前，判断是否存在
     *
     * @param ftpModel 数据信息
     * @param list     目录
     * @return Array
     */
    private JSONArray listRootDir(MachineFtpModel ftpModel, List<String> list) {
        JSONArray jsonArray = new JSONArray();
        if (CollUtil.isEmpty(list)) {
            return jsonArray;
        }

        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(ftpModel),
            EnumUtil.fromString(FtpMode.class, ftpModel.getMode(), FtpMode.Active))) {
            for (String allowPathParent : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", SecureUtil.sha1(allowPathParent));
                jsonObject.put("allowPathParent", allowPathParent);
                ftp.ls(allowPathParent);
                jsonArray.add(jsonObject);
            }
        } catch (Exception e) {
            log.error("连接FTP失败", e);
        }
        return jsonArray;
    }


    @RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       String name) {
        // name 可能为空，为空情况是删除目录
        String name2 = StrUtil.emptyToDefault(name, StrUtil.EMPTY);
        Assert.state(!StrUtil.equals(name2, StrUtil.SLASH), I18nMessageUtil.get("i18n.cannot_delete_root_dir.fcdc"));
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {

            try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
                EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active))) {
                String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name2);
                Assert.state(!StrUtil.equals(normalize, StrUtil.SLASH), I18nMessageUtil.get("i18n.cannot_delete_root_dir.fcdc"));
                // 尝试删除
                boolean dirOrFile = this.tryDelDirOrFile(ftp, normalize);
                if (dirOrFile) {
                    String parent = FileUtil.getParent(normalize, 1);
                    return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"), parent);
                }
                return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_file_deletion_exception.5ba5"), e);
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.delete_failure_with_colon.b429") + e.getMessage());
            }
        });
    }

    @RequestMapping(value = "rename.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> rename(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       @ValidatorItem String name,
                                       @ValidatorItem String newname) {

        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {

            try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
                EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active))) {
                String oldPath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
                String newPath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + newname);
                ftp.getClient().rename(oldPath, newPath);
            } catch (Exception e) {
                log.error("FTP重命名失败异常", e);
                return new JsonMessage<>(400, "FTP重命名失败异常" + e.getMessage());
            }
            return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
        });
    }

    /**
     * 删除文件 或者 文件夹
     *
     * @param ftp  ftp
     * @param path 路径
     * @return true 删除的是 文件夹
     */
    private boolean tryDelDirOrFile(Ftp ftp, String path) {
        try {
            // 先尝试删除文件夹
            ftp.delDir(path);
            return true;
        } catch (Exception e) {
            // 删除文件
            ftp.delFile(path);
        }
        return false;
    }

    /**
     * 上传分片
     *
     * @param file       文件对象
     * @param sliceId    分片id
     * @param totalSlice 总分片
     * @param nowSlice   当前分片
     * @param fileSumMd5 文件 md5
     * @return json
     */
   /* @PostMapping(value = "upload-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public IJsonMessage<String> uploadSharding(MultipartFile file,
                                               String sliceId,
                                               Integer totalSlice,
                                               Integer nowSlice,
                                               String fileSumMd5,
                                               @ValidatorItem String id,
                                               @ValidatorItem String allowPathParent,
                                               @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            String remotePath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath);
            Session session = null;
            ChannelSftp channel = null;
            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                channel.cd(remotePath);
                String originalFilename = file.getOriginalFilename();
                // xxxx.txt.1
                originalFilename = StrUtil.subBefore(originalFilename, ".", true);
                if (nowSlice == 0) {
                    channel.put(file.getInputStream(), originalFilename, ChannelSftp.OVERWRITE);
                } else {
                    channel.put(file.getInputStream(), originalFilename, ChannelSftp.APPEND);
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_file_upload_exception.5c1c"), e);
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.upload_failed.b019") + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
        });
    }
*/
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> upload(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       String unzip,
                                       MultipartFile file) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {

            String remotePath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath);
            File filePath = null;
            File tempUnzipPath = null;
            try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
                EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active))) {

                // 保存路径
                File tempPath = serverConfig.getUserTempPath();
                File savePath = FileUtil.file(tempPath, "ftp", machineFtpModel.getId());
                FileUtil.mkdir(savePath);
                String originalFilename = file.getOriginalFilename();
                filePath = FileUtil.file(savePath, originalFilename);
                //
                if (Convert.toBool(unzip, false)) {
                    String extName = FileUtil.extName(originalFilename);
                    Assert.state(StrUtil.containsAnyIgnoreCase(extName, StringUtil.PACKAGE_EXT), I18nMessageUtil.get("i18n.file_type_not_supported2.d497") + extName);
                    file.transferTo(filePath);
                    // 解压
                    tempUnzipPath = FileUtil.file(savePath, IdUtil.fastSimpleUUID());

                    FileUtil.mkdir(tempUnzipPath);
                    CompressionFileUtil.unCompress(filePath, tempUnzipPath);
                    ftp.uploadFileOrDirectory(remotePath, tempUnzipPath);
                } else {
                    file.transferTo(filePath);
                    ftp.uploadFileOrDirectory(remotePath, filePath);
                }
            } catch (Exception e) {
                log.error("FTP上传文件异常", e);
                return new JsonMessage<>(400, "FTP上传文件异常" + e.getMessage());
            } finally {
                CommandUtil.systemFastDel(filePath);
                CommandUtil.systemFastDel(tempUnzipPath);
            }
            return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
        });
    }

    /**
     * @return json
     * @api {post} new_file_folder.json ssh 中创建文件夹/文件
     * @apiGroup ftp
     * @apiUse defResultJson
     * @apiParam {String} id ftp id
     * @apiParam {String} path ftp 选择到目录
     * @apiParam {String} name 文件名
     * @apiParam {String} unFolder true/1 为文件夹，false/0 为文件
     * @apiSuccess {JSON}  data
     */
    @RequestMapping(value = "new_file_folder.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> newFileFolder(String id,
                                              @ValidatorItem String allowPathParent,
                                              @ValidatorItem String nextPath,
                                              @ValidatorItem String name, String unFolder) {
        Assert.state(!StrUtil.contains(name, StrUtil.SLASH), I18nMessageUtil.get("i18n.file_name_error_message.7a25"));
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineFtpModel, itemConfig) -> {

            try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel),
                EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active))) {
                String remotePath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);

                File filePath = null;
                try {
                    if (ftp.exist(remotePath)) {
                        return new JsonMessage<>(400, "文件夹或文件已经存在");
                    }
                    if (Convert.toBool(unFolder, false)) {
                        // 创建空文件到临时保存路径
                        File tempPath = serverConfig.getUserTempPath();
                        File savePath = FileUtil.file(tempPath, "ftp", machineFtpModel.getId());
                        FileUtil.mkdir(savePath);
                        filePath = FileUtil.file(savePath, name);
                        FileUtil.touch(filePath);

                        // 上传文件到ftp服务器 (需去掉目录中的文件名 防止创建文件同名目录)
                        ftp.upload(StrUtil.removeAll(remotePath, StrUtil.SLASH + name), filePath);

                    } else {
                        // 目录
                        try {
                            if (ftp.mkdir(remotePath)) {
                                // 创建成功
                                return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
                            }
                        } catch (Exception e) {
                            log.error("FTP创建文件夹异常", e);
                            return new JsonMessage<>(500, "FTP创建文件夹异常" + e.getMessage());
                        }
                    }
                    List<String> result = new ArrayList<>();
                    return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded_with_details.c773") + CollUtil.join(result, StrUtil.LF));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 删除临时文件
                    CommandUtil.systemFastDel(filePath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public String getPermissionString(FTPFile file) {
        // 如果权限信息无效，说明是 Windows FTP，返回占位符或空字符串
        if (!file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)
            && !file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)
            && !file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) {
            return "---------"; // 或返回 "N/A"、空串 ""
        }

        // 否则按照 Unix 风格格式化
        StringBuilder sb = new StringBuilder();
        sb.append(file.isDirectory() ? 'd' : '-');
        sb.append(file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-');
        sb.append(file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-');
        sb.append(file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-');

        sb.append(file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-');
        sb.append(file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-');
        sb.append(file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-');

        sb.append(file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-');
        sb.append(file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-');
        sb.append(file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-');

        return sb.toString();
    }
}
