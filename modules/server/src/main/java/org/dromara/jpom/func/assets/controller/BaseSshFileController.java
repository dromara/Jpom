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
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.CompressionFileUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@Slf4j
public abstract class BaseSshFileController extends BaseServerController {

    @Resource
    protected SshService sshService;
    @Resource
    protected MachineSshServer machineSshServer;
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
    protected abstract <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function);

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
    protected abstract <T> T checkConfigPathChildren(String id, String allowPathParent, String nextPath, BiFunction<MachineSshModel, ItemConfig, T> function);

    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(@ValidatorItem String id,
                         @ValidatorItem String allowPathParent,
                         @ValidatorItem String nextPath,
                         @ValidatorItem String name,
                         HttpServletResponse response) throws IOException {
        MachineSshModel machineSshModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel1, itemConfig) -> machineSshModel1);
        if (machineSshModel == null) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.ssh_error_or_folder_not_configured.c087"), MediaType.TEXT_HTML_VALUE);
            return;
        }
        String remotePath = FileUtils.resolveChildPath(allowPathParent, nextPath, name);
        try {
            this.downloadFile(machineSshModel, remotePath, response);
        } catch (SftpException e) {
            log.error(I18nMessageUtil.get("i18n.download_failed.65e2"), e);
            ServletUtil.write(response, "download error", MediaType.TEXT_HTML_VALUE);
        }
    }

    /**
     * 根据 id 获取 fileDirs 目录集合
     *
     * @param id ssh id
     * @return json
     * @author Hotstrip
     * @since for dev 3.x
     */
    @RequestMapping(value = "root_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONArray> rootFileList(@ValidatorItem String id) {
        //
        return this.checkConfigPath(id, (machineSshModel, itemConfig) -> {
            JSONArray listDir = listRootDir(machineSshModel, itemConfig.fileDirs());
            return JsonMessage.success("", listDir);
        });
    }


    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONArray> listData(@ValidatorItem String id,
                                            @ValidatorItem String allowPathParent,
                                            @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            try {
                JSONArray listDir = listDir(machineSshModel, allowPathParent, nextPath, itemConfig);
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
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            //
            String remotePath = FileUtils.resolveChildPath(allowPathParent, nextPath, name);
            String content = this.readFile(machineSshModel, remotePath, charset);
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
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            String remotePath = FileUtils.resolveChildPath(allowPathParent, nextPath, name);
            // 缓存到本地：目录名使用随机值，禁止客户端传入的路径参与本地文件路径拼接
            File tempDir = FileUtil.file(serverConfig.getUserTempPath(), "ssh", machineSshModel.getId(), IdUtil.fastSimpleUUID());
            File file = FileUtil.file(tempDir, FileUtil.getName(remotePath));
            try {
                FileUtil.mkParentDirs(file);
                FileUtil.writeString(content, file, charset);
                // 上传
                this.syncFile(machineSshModel, remotePath, file);
            } finally {
                //
                FileUtil.del(tempDir);
            }
            //
            return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
        });
    }

    /**
     * 读取文件
     *
     * @param machineSshModel ssh
     * @param remotePath      已经过白名单校验的远端文件路径
     * @param charset         编码格式
     */
    private String readFile(MachineSshModel machineSshModel, String remotePath, Charset charset) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            sftp.download(remotePath, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return new String(bytes, charset);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 上传文件
     *
     * @param machineSshModel ssh
     * @param remotePath      已经过白名单校验的远端文件路径
     * @param file            同步上传文件
     */
    private void syncFile(MachineSshModel machineSshModel,
                          String remotePath,
                          File file) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            sftp.upload(remotePath, file);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 下载文件
     *
     * @param machineSshModel ssh
     * @param remotePath      已经过白名单校验的远端文件路径
     * @param response        响应
     * @throws IOException   io
     * @throws SftpException sftp
     */
    private void downloadFile(MachineSshModel machineSshModel, String remotePath, HttpServletResponse response) throws IOException, SftpException {
        final String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        String fileName = FileUtil.getName(remotePath);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename={}", URLUtil.encode(fileName, Charset.forName(charset))));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(machineSshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            channel.get(remotePath, response.getOutputStream());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 查询文件夹下所有文件
     *
     * @param sshModel        ssh
     * @param allowPathParent 允许的路径
     * @param nextPath        下 N 级的文件夹
     * @return array
     * @throws SftpException sftp
     */
    @SuppressWarnings("unchecked")
    private JSONArray listDir(MachineSshModel sshModel, String allowPathParent, String nextPath, ItemConfig itemConfig) throws SftpException {
        Session session = null;
        ChannelSftp channel = null;
        List<String> allowEditSuffix = itemConfig.allowEditSuffix();
        try {
            session = sshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);

            String children2 = StrUtil.emptyToDefault(nextPath, StrUtil.SLASH);
            String allPath = FileUtils.resolveChildDir(allowPathParent, children2);
            JSONArray jsonArray = new JSONArray();
            Vector<ChannelSftp.LsEntry> vector;
            try {
                vector = channel.ls(allPath);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.get_folder_failure.0fda"), e);
                Throwable causedBy = ExceptionUtil.getCausedBy(e, SftpException.class);
                if (causedBy != null) {
                    throw new IllegalStateException(I18nMessageUtil.get("i18n.query_folder_sftp_failed.9d35") + causedBy.getMessage());
                }
                throw new IllegalStateException(I18nMessageUtil.get("i18n.query_folder_failed.3f0e") + e.getMessage());
            }
            for (ChannelSftp.LsEntry lsEntry : vector) {
                String filename = lsEntry.getFilename();
                if (StrUtil.DOT.equals(filename) || StrUtil.DOUBLE_DOT.equals(filename)) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", filename);
                jsonObject.put("id", SecureUtil.sha1(allPath + StrUtil.SLASH + filename));
                SftpATTRS attrs = lsEntry.getAttrs();
                int mTime = attrs.getMTime();
                //String format = DateUtil.format(DateUtil.date(mTime * 1000L), DatePattern.NORM_DATETIME_MINUTE_PATTERN);
                jsonObject.put("modifyTime", mTime * 1000L);
                if (attrs.isDir()) {
                    jsonObject.put("dir", true);
                } else {
                    long fileSize = attrs.getSize();
                    jsonObject.put("size", fileSize);
                    // 允许编辑
                    jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(allowEditSuffix, filename));
                }
                String longname = lsEntry.getLongname();
                jsonObject.put("longname", longname);
                jsonObject.put("link", attrs.isLink());
                jsonObject.put("extended", attrs.getExtended());
                jsonObject.put("permissions", attrs.getPermissionsString());
                jsonObject.put("allowPathParent", allowPathParent);
                //
                jsonObject.put("nextPath", FileUtil.normalize(children2));
//                jsonObject.put("absolutePath", FileUtil.normalize(StrUtil.format("{}/{}", nextPath, filename)));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 列出目前，判断是否存在
     *
     * @param sshModel 数据信息
     * @param list     目录
     * @return Array
     */
    private JSONArray listRootDir(MachineSshModel sshModel, List<String> list) {
        JSONArray jsonArray = new JSONArray();
        if (CollUtil.isEmpty(list)) {
            return jsonArray;
        }
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            for (String allowPathParent : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", SecureUtil.sha1(allowPathParent));
                jsonObject.put("allowPathParent", allowPathParent);
                try {
                    channel.ls(allowPathParent);
                } catch (SftpException e) {
                    // 标记文件夹不存在
                    jsonObject.put("error", true);
                }
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
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
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            Sftp sftp = null;
            try {
                //
                String normalize = StrUtil.isEmpty(name2)
                        ? FileUtils.resolveChildDir(allowPathParent, nextPath)
                        : FileUtils.resolveChildPath(allowPathParent, nextPath, name2);
                Assert.state(!StrUtil.equals(normalize, FileUtils.resolveChildDir(allowPathParent, StrUtil.EMPTY)), I18nMessageUtil.get("i18n.cannot_delete_root_dir.fcdc"));
                session = sshService.getSessionByModel(machineSshModel);
                sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
                // 尝试删除
                boolean dirOrFile = this.tryDelDirOrFile(sftp, normalize);
                if (dirOrFile) {
                    String parent = FileUtil.getParent(normalize, 1);
                    return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"), parent);
                }
                return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_file_deletion_exception.5ba5"), e);
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.delete_failure_with_colon.b429") + e.getMessage());
            } finally {
                IoUtil.close(sftp);
                JschUtil.close(session);
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

        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            ChannelSftp channel = null;

            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                String oldPath = FileUtils.resolveChildPath(allowPathParent, nextPath, name);
                String newPath = FileUtils.resolveChildPath(allowPathParent, nextPath, newname);
                channel.rename(oldPath, newPath);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_rename_failed_exception.94aa"), e);
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.rename_failed.0c76") + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
        });
    }

    /**
     * 删除文件 或者 文件夹
     *
     * @param sftp ftp
     * @param path 路径
     * @return true 删除的是 文件夹
     */
    private boolean tryDelDirOrFile(Sftp sftp, String path) {
        try {
            // 先尝试删除文件夹
            sftp.delDir(path);
            return true;
        } catch (Exception e) {
            // 删除文件
            sftp.delFile(path);
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
    @PostMapping(value = "upload-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
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
            String remotePath = FileUtils.resolveChildDir(allowPathParent, nextPath);
            Session session = null;
            ChannelSftp channel = null;
            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                channel.cd(remotePath);
                String originalFilename = file.getOriginalFilename();
                // 分片上传的文件名先按单段名称校验，再按 xxxx.txt.1 规则还原原名
                FileUtils.checkPathItemName(originalFilename);
                originalFilename = StrUtil.subBefore(originalFilename, ".", true);
                String target = FileUtils.resolveChildPath(allowPathParent, nextPath, originalFilename);
                if (nowSlice == 0) {
                    channel.put(file.getInputStream(), target, ChannelSftp.OVERWRITE);
                } else {
                    channel.put(file.getInputStream(), target, ChannelSftp.APPEND);
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


    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> upload(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       String unzip,
                                       MultipartFile file) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            String remotePath = FileUtils.resolveChildDir(allowPathParent, nextPath);
            String originalFilename = file.getOriginalFilename();
            // multipart 的文件名同样按单段名称校验，禁止把 ../ 拼进远端/本地路径
            FileUtils.checkPathItemName(originalFilename);
            String remoteFilePath = FileUtils.resolveChildPath(allowPathParent, nextPath, originalFilename);
            Session session = null;
            ChannelSftp channel = null;
            // 本地缓存目录使用随机值，文件名只取单段名称
            File tempDir = FileUtil.file(serverConfig.getUserTempPath(), "ssh", machineSshModel.getId(), IdUtil.fastSimpleUUID());

            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                // 保存路径
                FileUtil.mkdir(tempDir);
                File filePath = FileUtil.file(tempDir, originalFilename);
                //
                if (Convert.toBool(unzip, false)) {
                    String extName = FileUtil.extName(originalFilename);
                    Assert.state(StrUtil.containsAnyIgnoreCase(extName, StringUtil.PACKAGE_EXT), I18nMessageUtil.get("i18n.file_type_not_supported2.d497") + extName);
                    file.transferTo(filePath);
                    // 解压
                    File tempUnzipPath = FileUtil.file(tempDir, "unzip");
                    try {
                        FileUtil.mkdir(tempUnzipPath);
                        CompressionFileUtil.unCompress(filePath, tempUnzipPath);
                        // 同步上传文件
                        sshService.uploadDir(machineSshModel, remotePath, tempUnzipPath);
                    } finally {
                        CommandUtil.systemFastDel(tempUnzipPath);
                    }
                } else {
                    file.transferTo(filePath);
                    try (FileInputStream src = IoUtil.toStream(filePath)) {
                        channel.put(src, remoteFilePath);
                    }
                }

            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_file_upload_exception.5c1c"), e);
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.upload_failed.b019") + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
                CommandUtil.systemFastDel(tempDir);
            }
            return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
        });
    }

    /**
     * @return json
     * @api {post} new_file_folder.json ssh 中创建文件夹/文件
     * @apiGroup ssh
     * @apiUse defResultJson
     * @apiParam {String} id ssh id
     * @apiParam {String} path ssh 选择到目录
     * @apiParam {String} name 文件名
     * @apiParam {String} unFolder true/1 为文件，false/0 为文件夹
     * @apiSuccess {JSON}  data
     */
    @RequestMapping(value = "new_file_folder.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> newFileFolder(String id,
                                              @ValidatorItem String allowPathParent,
                                              @ValidatorItem String nextPath,
                                              @ValidatorItem String name, String unFolder) {
        checkNewFileName(name);
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            try {
                session = sshService.getSessionByModel(machineSshModel);
                String remotePath = FileUtils.resolveChildPath(allowPathParent, nextPath, name);
                Charset charset = machineSshModel.charset();
                int timeout = machineSshModel.timeout();
                try (Sftp sftp = new Sftp(session, charset, timeout)) {
                    if (sftp.exist(remotePath)) {
                        return new JsonMessage<>(400, I18nMessageUtil.get("i18n.folder_or_file_exists.c687"));
                    }
                    if (Convert.toBool(unFolder, false)) {
                        // 文件：通过 SFTP 创建空文件，禁止把用户输入拼进远程 shell 命令
                        try {
                            createEmptyFile(sftp.getClient(), remotePath);
                        } catch (SftpException e) {
                            throw Lombok.sneakyThrow(e);
                        }
                        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
                    }
                    // 目录
                    try {
                        if (sftp.mkdir(remotePath)) {
                            // 创建成功
                            return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
                        }
                    } catch (Exception e) {
                        log.error(I18nMessageUtil.get("i18n.ssh_folder_creation_exception.6ed2"), e);
                        return new JsonMessage<>(500, I18nMessageUtil.get("i18n.create_folder_failure.b632") + e.getMessage());
                    }
                    return new JsonMessage<>(500, I18nMessageUtil.get("i18n.create_folder_failure.b632"));
                }
            } finally {
                JschUtil.close(session);
            }
        });
    }

    /**
     * 校验 SSH 文件管理新建文件/文件夹的名称。
     * <p>
     * 名称只能是单个路径片段：禁止 /、\（路径分隔符）、回车/换行/NUL（控制字符）以及 . 和 ..（目录越界）。
     * 创建动作已改为 SFTP 调用，shell 元字符只会作为普通文件名字符，不会被执行。
     *
     * @param name 文件/文件夹名称
     */
    static void checkNewFileName(String name) {
        FileUtils.checkPathItemName(name);
    }

    /**
     * 通过 SFTP 在远程主机创建空文件（不经过远程 shell）。
     *
     * @param client     SFTP 客户端
     * @param remotePath 远程文件路径
     * @throws SftpException 创建失败
     */
    static void createEmptyFile(ChannelSftp client, String remotePath) throws SftpException {
        client.put(new ByteArrayInputStream(new byte[0]), remotePath);
    }

    /**
     * 修改文件权限
     *
     * @param id
     * @param allowPathParent
     * @param nextPath
     * @param fileName
     * @param permissionValue
     * @return
     */
    @RequestMapping(value = "change_file_permission.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONArray> changeFilePermissions(@ValidatorItem String id,
                                                         @ValidatorItem String allowPathParent,
                                                         @ValidatorItem String nextPath,
                                                         @ValidatorItem String fileName,
                                                         @ValidatorItem String permissionValue) {
        MachineSshModel machineSshModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel1, itemConfig) -> machineSshModel1);
        if (machineSshModel == null) {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.ssh_error_or_folder_not_configured.c087"));
        }
        Session session = sshService.getSessionByModel(machineSshModel);
        Charset charset = machineSshModel.charset();
        int timeout = machineSshModel.timeout();
        String remotePath = FileUtils.resolveChildPath(allowPathParent, nextPath, fileName);
        try (Sftp sftp = new Sftp(session, charset, timeout)) {
            ChannelSftp client = sftp.getClient();
            //
            int permissions = Integer.parseInt(permissionValue, 8);
            client.chmod(permissions, remotePath);
        } catch (SftpException e) {
            log.error(I18nMessageUtil.get("i18n.ssh_modify_permission_error.0cd3"), remotePath, permissionValue, e);
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.operation_failed.3d94") + e.getMessage());
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }
}
