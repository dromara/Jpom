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
package io.jpom.controller.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.SshModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.CompressionFileUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

/**
 * ssh 文件管理
 *
 * @author bwcx_jzy
 * @since 2019/8/10
 */
@RestController
@RequestMapping("node/ssh")
@Feature(cls = ClassFeature.SSH_FILE)
@Slf4j
public class SshFileController extends BaseServerController {

    private final SshService sshService;

    public SshFileController(SshService sshService) {
        this.sshService = sshService;
    }

    @RequestMapping(value = "download.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(String id, String path, String name) throws IOException {
        HttpServletResponse response = getResponse();
        SshModel sshModel = sshService.getByKey(id, false);
        if (sshModel == null) {
            ServletUtil.write(response, "ssh error", MediaType.TEXT_HTML_VALUE);
            return;
        }
        List<String> fileDirs = sshModel.fileDirs();
        //
        if (StrUtil.isEmpty(path) || !fileDirs.contains(path)) {
            ServletUtil.write(response, "没有配置此文件夹", MediaType.TEXT_HTML_VALUE);
            return;
        }
        if (StrUtil.isEmpty(name)) {
            ServletUtil.write(response, "name error", MediaType.TEXT_HTML_VALUE);
            return;
        }
        try {
            this.downloadFile(sshModel, path, name, response);
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
    public String rootFileList(String id) {
        SshModel sshModel = sshService.getByKey(id, false);
        Assert.notNull(sshModel, "不存在对应ssh");
        List<String> fileDirs = sshModel.fileDirs();
        Assert.notEmpty(fileDirs, "未设置授权目录");
        JSONArray jsonArray = this.listDir(sshModel, fileDirs);
        return JsonMessage.getString(200, "ok", jsonArray);
    }

    private SshModel check(String id, String path, String children) {
        SshModel sshModel = sshService.getByKey(id, false);
        Assert.notNull(sshModel, "不存在对应ssh");
        Assert.hasText(path, "请选择文件夹");
        List<String> fileDirs = sshModel.fileDirs();
        Assert.state(CollUtil.contains(fileDirs, path), "没有配置此文件夹");
        //
        if (StrUtil.isNotEmpty(children)) {
            // 判断是否合法
            children = FileUtil.normalize(children);
            FileUtil.file(path, children);
        }
        return sshModel;
    }

    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String listData(String id, String path, String children) throws SftpException {
        SshModel sshModel = this.check(id, path, children);
        //
        JSONArray jsonArray = listDir(sshModel, path, children);
        return JsonMessage.getString(200, "ok", jsonArray);
    }

    @RequestMapping(value = "read_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String readFileData(String id, String path, String children) {
        SshModel sshModel = this.check(id, path, children);
        //
        List<String> allowEditSuffix = sshModel.allowEditSuffix();
        Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, children);
        //
        String content = this.readFile(sshModel, path, children, charset);
        return JsonMessage.getString(200, "ok", content);
    }

    @RequestMapping(value = "update_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String updateFileData(String id, String path, String children, String content) {
        SshModel sshModel = this.check(id, path, children);
        //
        List<String> allowEditSuffix = sshModel.allowEditSuffix();
        Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, children);
        // 缓存到本地
        File file = FileUtil.file(ServerConfigBean.getInstance().getUserTempPath(), sshModel.getId(), children);
        FileUtil.writeString(content, file, charset);
        // 上传
        this.syncFile(sshModel, path, children, file);
        //
        FileUtil.del(file);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 读取文件
     *
     * @param sshModel ssh
     * @param path     路径
     * @param name     文件
     * @param charset  编码格式
     */
    private String readFile(SshModel sshModel, String path, String name, Charset charset) {
        Sftp sftp = null;
        try {
            Session session = SshService.getSessionByModel(sshModel);
            sftp = new Sftp(session, sshModel.charset(), sshModel.timeout());
            String normalize = FileUtil.normalize(path + StrUtil.SLASH + name);
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
     * @param sshModel ssh
     * @param path     路径
     * @param name     文件
     * @param file     同步上传文件
     */
    private void syncFile(SshModel sshModel, String path, String name, File file) {
        Sftp sftp = null;
        try {
            Session session = SshService.getSessionByModel(sshModel);
            sftp = new Sftp(session, sshModel.charset(), sshModel.timeout());
            String normalize = FileUtil.normalize(path + StrUtil.SLASH + name);
            sftp.upload(normalize, file);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 下载文件
     *
     * @param sshModel ssh
     * @param path     路径
     * @param name     文件
     * @param response 响应
     * @throws IOException   io
     * @throws SftpException sftp
     */
    private void downloadFile(SshModel sshModel, String path, String name, HttpServletResponse response) throws IOException, SftpException {
        final String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        String fileName = FileUtil.getName(name);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename={}", URLUtil.encode(fileName, Charset.forName(charset))));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            String normalize = FileUtil.normalize(path + StrUtil.SLASH + name);
            channel.get(normalize, response.getOutputStream());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 查询文件夹下所有文件
     *
     * @param sshModel ssh
     * @param path     路径
     * @param children 文件夹
     * @return array
     * @throws SftpException sftp
     */
    @SuppressWarnings("unchecked")
    private JSONArray listDir(SshModel sshModel, String path, String children) throws SftpException {
        Session session = null;
        ChannelSftp channel = null;
        List<String> allowEditSuffix = sshModel.allowEditSuffix();
        try {
            session = SshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            Vector<ChannelSftp.LsEntry> vector;
            if (StrUtil.isNotEmpty(children)) {
                String allPath = StrUtil.format("{}/{}", path, children);
                allPath = FileUtil.normalize(allPath);
                vector = channel.ls(allPath);
            } else {
                vector = channel.ls(path);
            }
            JSONArray jsonArray = new JSONArray();
            for (ChannelSftp.LsEntry lsEntry : vector) {
                String filename = lsEntry.getFilename();
                if (StrUtil.DOT.equals(filename) || StrUtil.DOUBLE_DOT.equals(filename)) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", filename);
                jsonObject.put("id", IdUtil.fastSimpleUUID());
                int mTime = lsEntry.getAttrs().getMTime();
                String format = DateUtil.format(DateUtil.date(mTime * 1000L), DatePattern.NORM_DATETIME_MINUTE_PATTERN);
                jsonObject.put("modifyTime", format);
                if (lsEntry.getAttrs().isDir()) {
                    jsonObject.put("dir", true);
                    jsonObject.put("title", filename);
                } else {
                    jsonObject.put("title", filename);
                    long fileSize = lsEntry.getAttrs().getSize();
                    jsonObject.put("size", FileUtil.readableFileSize(fileSize));
                    // 允许编辑
                    jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(allowEditSuffix, filename));
                }
                //
                if (StrUtil.isEmpty(children)) {
                    jsonObject.put("parentDir", filename);
                } else {
                    jsonObject.put("parentDir", FileUtil.normalize(StrUtil.format("{}/{}", children, filename)));
                }
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
    private JSONArray listDir(SshModel sshModel, List<String> list) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            JSONArray jsonArray = new JSONArray();
            for (String item : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", item);
                try {
                    channel.ls(item);
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
    public String delete(String id, String path, String name) {
        Assert.hasText(name, "name error");
        SshModel sshModel = this.check(id, path, name);
        name = FileUtil.normalize(name);
        Assert.state(!StrUtil.equals(name, StrUtil.SLASH), "不能删除根目录");
        Session session = null;
        Sftp sftp = null;
        try {
            // 验证合法性，防止越权
            FileUtil.file(path, name);
            //
            String normalize = FileUtil.normalize(path + StrUtil.SLASH + name);
            session = SshService.getSessionByModel(sshModel);
            sftp = new Sftp(session, sshModel.charset(), sshModel.timeout());
            // 尝试删除
            boolean dirOrFile = this.tryDelDirOrFile(sftp, normalize);
            if (dirOrFile) {
                String parent = FileUtil.getParent(name, 1);
                return JsonMessage.getString(200, "删除成功", parent);
            }
            return JsonMessage.getString(200, "删除成功");
        } catch (Exception e) {
            log.error("ssh删除文件异常", e);
            return JsonMessage.getString(400, "删除失败:" + e.getMessage());
        } finally {
            IoUtil.close(sftp);
            JschUtil.close(session);
        }
    }

    @RequestMapping(value = "rename.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String rename(String id, String path, String name, String newname) {
        Assert.hasText(name, "name error");
        Assert.hasText(newname, "newname error");
        SshModel sshModel = this.check(id, path, name);
        name = FileUtil.normalize(name);
        FileUtil.file(path, newname);
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = SshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            channel.rename(FileUtil.normalize(path + StrUtil.SLASH + name), FileUtil.normalize(path + StrUtil.SLASH + newname));
        } catch (Exception e) {
            log.error("ssh重命名失败异常", e);
            return JsonMessage.getString(400, "重命名失败:" + e.getMessage());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
        return JsonMessage.getString(200, "操作成功");
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

//	/**
//	 * 删除文件或者文件夹
//	 *
//	 * @param channel channel
//	 * @param path    文件路径
//	 * @throws SftpException SftpException
//	 */
//	private void deleteFile(ChannelSftp channel, String path) throws SftpException {
//		Vector<ChannelSftp.LsEntry> vector = channel.ls(path);
//		if (null == vector) {
//			return;
//		}
//		int size = vector.size();
//		if (size == 1) {
//			// 文件，直接删除
//			channel.rm(path);
//		} else if (size == 2) {
//			// 空文件夹，直接删除
//			channel.rmdir(path);
//		} else {
//			// 删除文件夹下所有文件
//			String fileName;
//			for (ChannelSftp.LsEntry en : vector) {
//				fileName = en.getFilename();
//				if (!".".equals(fileName) && !"..".equals(fileName)) {
//					deleteFile(channel, path + "/" + fileName);
//				}
//			}
//			channel.rmdir(path);
//		}
//	}

    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public String upload(String id, String path, String name, String unzip) {
        SshModel sshModel = sshService.getByKey(id, false);
        Assert.notNull(sshModel, "ssh error");
        List<String> fileDirs = sshModel.fileDirs();
        Assert.state(CollUtil.contains(fileDirs, path), "没有配置此文件夹");
        String remotePath = FileUtil.normalize(path + StrUtil.SLASH + name);
        Session session = null;
        ChannelSftp channel = null;
        String localPath = null;
        try {
            session = SshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            MultipartFileBuilder multipart = createMultipart();
            // 保存路径
            File tempPath = ServerConfigBean.getInstance().getUserTempPath();
            File savePath = FileUtil.file(tempPath, "ssh", sshModel.getId());
            multipart.setSavePath(FileUtil.getAbsolutePath(savePath));
            multipart.addFieldName("file")
                .setUseOriginalFilename(true);
            //
            if (Convert.toBool(unzip, false)) {
                multipart.setFileExt(StringUtil.PACKAGE_EXT);
                localPath = multipart.save();
                // 解压
                File file = new File(localPath);
                File tempUnzipPath = FileUtil.file(savePath, IdUtil.fastSimpleUUID());
                try {
                    CompressionFileUtil.unCompress(file, tempUnzipPath);
                    // 同步上传文件
                    sshService.uploadDir(sshModel, remotePath, tempUnzipPath);
                } finally {
                    // 删除临时文件
                    CommandUtil.systemFastDel(file);
                    CommandUtil.systemFastDel(tempUnzipPath);
                }
            } else {
                localPath = multipart.save();
                File file = FileUtil.file(localPath);
                channel.cd(remotePath);
                try (FileInputStream src = IoUtil.toStream(file)) {
                    channel.put(src, file.getName());
                }
            }

        } catch (Exception e) {
            log.error("ssh上传文件异常", e);
            return JsonMessage.getString(400, "上传失败:" + e.getMessage());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
            FileUtil.del(localPath);
        }
        return JsonMessage.getString(200, "上传成功");
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
    public String newFileFolder(String id, @ValidatorItem String path, @ValidatorItem String name, String unFolder) throws IOException {
        SshModel sshModel = sshService.getByKey(id, false);
        Assert.notNull(sshModel, "ssh error");
        Session session = SshService.getSessionByModel(sshModel);
        String normalize = FileUtil.normalize(name);
        Assert.state(!StrUtil.contains(normalize, StrUtil.SLASH), "文件名不能包含/");
        // 验证合法性，防止越权
        FileUtil.file(path, name);
        String remotePath = FileUtil.normalize(path + StrUtil.SLASH + name);
        try (Sftp sftp = new Sftp(session, sshModel.charset(), sshModel.timeout())) {
            if (sftp.exist(remotePath)) {
                return JsonMessage.getString(400, "文件夹或者文件已存在");
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
                        return JsonMessage.getString(200, "操作成功");
                    }
                } catch (Exception e) {
                    log.error("ssh创建文件夹异常", e);
                    return JsonMessage.getString(500, "创建文件夹失败（文件夹名可能已经存在啦）:" + e.getMessage());
                }
            }
            String result = sshService.exec(sshModel, String.valueOf(command));
            return JsonMessage.getString(200, "操作成功 " + result);
        }
    }

}
