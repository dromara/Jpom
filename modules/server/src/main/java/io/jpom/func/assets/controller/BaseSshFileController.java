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
package io.jpom.func.assets.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.func.assets.server.MachineSshServer;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerConfig;
import io.jpom.util.CommandUtil;
import io.jpom.util.CompressionFileUtil;
import io.jpom.util.StringUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
     * @param allowPathParent 想要验证的目录 （白名单）
     * @param nextPath        白名单后的二级路径
     * @param function        回调
     * @param <T>             泛型
     * @return 处理后的数据
     */
    protected abstract <T> T checkConfigPathChildren(String id, String allowPathParent, String nextPath, BiFunction<MachineSshModel, ItemConfig, T> function);

    @RequestMapping(value = "download.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(@ValidatorItem String id,
                         @ValidatorItem String allowPathParent,
                         @ValidatorItem String nextPath,
                         @ValidatorItem String name,
                         HttpServletResponse response) throws IOException {
        MachineSshModel machineSshModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel1, itemConfig) -> machineSshModel1);
        if (machineSshModel == null) {
            ServletUtil.write(response, "ssh error 或者 没有配置此文件夹", MediaType.TEXT_HTML_VALUE);
            return;
        }
        try {
            this.downloadFile(machineSshModel, allowPathParent, nextPath, name, response);
        } catch (SftpException e) {
            log.error("下载失败", e);
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
    public JsonMessage<JSONArray> rootFileList(@ValidatorItem String id) {
        //
        return this.checkConfigPath(id, (machineSshModel, itemConfig) -> {
            JSONArray listDir = listRootDir(machineSshModel, itemConfig.fileDirs());
            return JsonMessage.success("ok", listDir);
        });
    }


    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONArray> listData(@ValidatorItem String id,
                                           @ValidatorItem String allowPathParent,
                                           @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            try {
                JSONArray listDir = listDir(machineSshModel, allowPathParent, nextPath, itemConfig);
                return JsonMessage.success("ok", listDir);
            } catch (SftpException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    @RequestMapping(value = "read_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<String> readFileData(@ValidatorItem String id,
                                            @ValidatorItem String allowPathParent,
                                            @ValidatorItem String nextPath,
                                            @ValidatorItem String name) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            //
            String content = this.readFile(machineSshModel, allowPathParent, nextPath, name, charset);
            return JsonMessage.success("ok", content);
        });
    }

    @RequestMapping(value = "update_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> updateFileData(@ValidatorItem String id,
                                              @ValidatorItem String allowPathParent,
                                              @ValidatorItem String nextPath,
                                              @ValidatorItem String name,
                                              @ValidatorItem String content) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            // 缓存到本地
            File file = FileUtil.file(serverConfig.getUserTempPath(), machineSshModel.getId(), allowPathParent, nextPath, name);
            try {
                FileUtil.writeString(content, file, charset);
                // 上传
                this.syncFile(machineSshModel, allowPathParent, nextPath, name, file);
            } finally {
                //
                FileUtil.del(file);
            }
            //
            return JsonMessage.success("修改成功");
        });
    }

    /**
     * 读取文件
     *
     * @param machineSshModel ssh
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param charset         编码格式
     */
    private String readFile(MachineSshModel machineSshModel, String allowPathParent, String nextPath, String name, Charset charset) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            sftp.download(normalize, byteArrayOutputStream);
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
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param file            同步上传文件
     */
    private void syncFile(MachineSshModel machineSshModel,
                          String allowPathParent,
                          String nextPath,
                          String name,
                          File file) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
            sftp.upload(normalize, file);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 下载文件
     *
     * @param machineSshModel ssh
     * @param allowPathParent 路径
     * @param name            文件
     * @param response        响应
     * @throws IOException   io
     * @throws SftpException sftp
     */
    private void downloadFile(MachineSshModel machineSshModel, String allowPathParent, String nextPath, String name, HttpServletResponse response) throws IOException, SftpException {
        final String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        String fileName = FileUtil.getName(name);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename={}", URLUtil.encode(fileName, Charset.forName(charset))));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(machineSshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
            channel.get(normalize, response.getOutputStream());
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
            String allPath = StrUtil.format("{}/{}", allowPathParent, children2);
            allPath = FileUtil.normalize(allPath);
            Vector<ChannelSftp.LsEntry> vector = channel.ls(allPath);
            JSONArray jsonArray = new JSONArray();
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
                String format = DateUtil.format(DateUtil.date(mTime * 1000L), DatePattern.NORM_DATETIME_MINUTE_PATTERN);
                jsonObject.put("modifyTime", format);
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
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            JSONArray jsonArray = new JSONArray();
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
    public JsonMessage<String> delete(@ValidatorItem String id,
                                      @ValidatorItem String allowPathParent,
                                      @ValidatorItem String nextPath,
                                      String name) {
        // name 可能为空，为空情况是删除目录
        String name2 = StrUtil.emptyToDefault(name, StrUtil.EMPTY);
        Assert.state(!StrUtil.equals(name2, StrUtil.SLASH), "不能删除根目录");
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            Sftp sftp = null;
            try {
                //
                String normalize = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name2);
                Assert.state(!StrUtil.equals(normalize, StrUtil.SLASH), "不能删除根目录");
                session = sshService.getSessionByModel(machineSshModel);
                sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
                // 尝试删除
                boolean dirOrFile = this.tryDelDirOrFile(sftp, normalize);
                if (dirOrFile) {
                    String parent = FileUtil.getParent(normalize, 1);
                    return JsonMessage.success("删除成功", parent);
                }
                return JsonMessage.success("删除成功");
            } catch (Exception e) {
                log.error("ssh删除文件异常", e);
                return new JsonMessage<>(400, "删除失败:" + e.getMessage());
            } finally {
                IoUtil.close(sftp);
                JschUtil.close(session);
            }
        });
    }

    @RequestMapping(value = "rename.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> rename(@ValidatorItem String id,
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
                String oldPath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
                String newPath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + newname);
                channel.rename(oldPath, newPath);
            } catch (Exception e) {
                log.error("ssh重命名失败异常", e);
                return new JsonMessage<>(400, "重命名失败:" + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return JsonMessage.success("操作成功");
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


    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public JsonMessage<String> upload(@ValidatorItem String id,
                                      @ValidatorItem String allowPathParent,
                                      @ValidatorItem String nextPath,
                                      String unzip,
                                      MultipartFile file) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            String remotePath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath);
            Session session = null;
            ChannelSftp channel = null;

            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                // 保存路径
                File tempPath = serverConfig.getUserTempPath();
                File savePath = FileUtil.file(tempPath, "ssh", machineSshModel.getId());
                FileUtil.mkdir(savePath);
                String originalFilename = file.getOriginalFilename();
                File filePath = FileUtil.file(savePath, originalFilename);
                //
                if (Convert.toBool(unzip, false)) {
                    String extName = FileUtil.extName(originalFilename);
                    Assert.state(StrUtil.containsAnyIgnoreCase(extName, StringUtil.PACKAGE_EXT), "不支持的文件类型：" + extName);
                    file.transferTo(filePath);
                    // 解压
                    File tempUnzipPath = FileUtil.file(savePath, IdUtil.fastSimpleUUID());
                    try {
                        FileUtil.mkdir(tempUnzipPath);
                        CompressionFileUtil.unCompress(filePath, tempUnzipPath);
                        // 同步上传文件
                        sshService.uploadDir(machineSshModel, remotePath, tempUnzipPath);
                    } finally {
                        // 删除临时文件
                        CommandUtil.systemFastDel(filePath);
                        CommandUtil.systemFastDel(tempUnzipPath);
                    }
                } else {
                    file.transferTo(filePath);
                    channel.cd(remotePath);
                    try (FileInputStream src = IoUtil.toStream(filePath)) {
                        channel.put(src, filePath.getName());
                    }
                }

            } catch (Exception e) {
                log.error("ssh上传文件异常", e);
                return new JsonMessage<>(400, "上传失败:" + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return JsonMessage.success("操作成功");
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
     * @apiParam {String} unFolder true/1 为文件夹，false/0 为文件
     * @apiSuccess {JSON}  data
     */
    @RequestMapping(value = "new_file_folder.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public JsonMessage<String> newFileFolder(String id,
                                             @ValidatorItem String allowPathParent,
                                             @ValidatorItem String nextPath,
                                             @ValidatorItem String name, String unFolder) {
        Assert.state(!StrUtil.contains(name, StrUtil.SLASH), "文件名不能包含/");
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = sshService.getSessionByModel(machineSshModel);
            String remotePath = FileUtil.normalize(allowPathParent + StrUtil.SLASH + nextPath + StrUtil.SLASH + name);
            try (Sftp sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout())) {
                if (sftp.exist(remotePath)) {
                    return new JsonMessage<>(400, "文件夹或者文件已存在");
                }
                StringBuilder command = new StringBuilder();
                if (Convert.toBool(unFolder, false)) {
                    // 文件
                    command.append("touch ").append(remotePath);
                } else {
                    // 目录
                    command.append("mkdir ").append(remotePath);
                    try {
                        if (sftp.mkdir(remotePath)) {
                            // 创建成功
                            return JsonMessage.success("操作成功");
                        }
                    } catch (Exception e) {
                        log.error("ssh创建文件夹异常", e);
                        return new JsonMessage<>(500, "创建文件夹失败（文件夹名可能已经存在啦）:" + e.getMessage());
                    }
                }
                String result = sshService.exec(machineSshModel, String.valueOf(command));
                return JsonMessage.success("操作成功 " + result);
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }
}
