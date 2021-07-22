package io.jpom.controller.node.ssh;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.SshModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

/**
 * ssh 文件管理
 *
 * @author bwcx_jzy
 * @date 2019/8/10
 */
@Controller()
@RequestMapping("node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshFileController extends BaseServerController {

    @Resource
    private SshService sshService;

//    @RequestMapping(value = "file.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.FILE)
//    public String file(String id) {
//        SshModel sshModel = sshService.getItem(id);
//        if (sshModel != null) {
//            List<String> fileDirs = sshModel.getFileDirs();
//            if (fileDirs != null && !fileDirs.isEmpty()) {
//                try {
//                    JSONArray jsonArray = listDir(sshModel, fileDirs);
//                    setAttribute("dirs", jsonArray);
//                } catch (Exception e) {
//                    DefaultSystemLog.getLog().error("sftp错误", e);
//                }
//            }
//        }
//        return "node/ssh/file";
//    }


    @RequestMapping(value = "download.html", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(String id, String path, String name) throws IOException {
        HttpServletResponse response = getResponse();
        SshModel sshModel = sshService.getItem(id);
        if (sshModel == null) {
            ServletUtil.write(response, "ssh error", MediaType.TEXT_HTML_VALUE);
            return;
        }
        List<String> fileDirs = sshModel.getFileDirs();
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
            DefaultSystemLog.getLog().error("下载失败", e);
            ServletUtil.write(response, "download error", MediaType.TEXT_HTML_VALUE);
        }
    }

    /**
     * 根据 id 获取 fileDirs 目录集合
     * @description for dev 3.x
     * @author Hotstrip
     * @param id
     * @return
     */
    @RequestMapping(value = "root_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.FILE)
    public String rootFileList(String id) {
        SshModel sshModel = sshService.getItem(id);
        if (sshModel == null) {
            return JsonMessage.getString(404, "不存在对应ssh");
        }
        List<String> fileDirs = sshModel.getFileDirs();
        if (fileDirs == null && !fileDirs.isEmpty()) {
            return JsonMessage.getString(405, "未设置授权目录");
        }
        JSONArray jsonArray = listDir(sshModel, fileDirs);
        return JsonMessage.getString(200, "ok", jsonArray);
    }

    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.FILE)
    public String listData(String id, String path, String children) throws SftpException {
        SshModel sshModel = sshService.getItem(id);
        if (sshModel == null) {
            return JsonMessage.getString(404, "不存在对应ssh");
        }
        if (StrUtil.isEmpty(path)) {
            return JsonMessage.getString(405, "请选择文件夹");
        }
        if (StrUtil.isNotEmpty(children)) {
            // 判断是否合法
            children = FileUtil.normalize(children);
            FileUtil.file(path, children);
        }
        List<String> fileDirs = sshModel.getFileDirs();
        if (!fileDirs.contains(path)) {
            return JsonMessage.getString(405, "没有配置此文件夹");
        }
        JSONArray jsonArray = listDir(sshModel, path, children);
        return JsonMessage.getString(200, "ok", jsonArray);
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
            session = SshService.getSession(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            String normalize = FileUtil.normalize(path + "/" + name);
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
    private JSONArray listDir(SshModel sshModel, String path, String children) throws SftpException {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = SshService.getSession(sshModel);
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
                if (StrUtil.DOT.equals(lsEntry.getFilename()) || StrUtil.DOUBLE_DOT.equals(lsEntry.getFilename())) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", lsEntry.getFilename());
                jsonObject.put("id", IdUtil.fastSimpleUUID());
                int mTime = lsEntry.getAttrs().getMTime();
                String format = DateUtil.format(DateUtil.date(mTime * 1000L), DatePattern.NORM_DATETIME_MINUTE_PATTERN);
                jsonObject.put("modifyTime", format);
                if (lsEntry.getAttrs().isDir()) {
                    jsonObject.put("dir", true);
                    jsonObject.put("title", lsEntry.getFilename() + "【文件夹】");
                } else {
                    jsonObject.put("title", lsEntry.getFilename());
                    long fileSize = lsEntry.getAttrs().getSize();
                    jsonObject.put("size", FileUtil.readableFileSize(fileSize));
                }
                //
                if (StrUtil.isEmpty(children)) {
                    jsonObject.put("parentDir", lsEntry.getFilename());
                } else {
                    jsonObject.put("parentDir", StrUtil.format("{}/{}", children, lsEntry.getFilename()));
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
            session = sshService.getSession(sshModel);
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


    @RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public String delete(String id, String path, String name) {
        SshModel sshModel = sshService.getItem(id);
        if (sshModel == null) {
            return JsonMessage.getString(400, "ssh error");
        }
        List<String> fileDirs = sshModel.getFileDirs();
        //
        if (StrUtil.isEmpty(path) || !fileDirs.contains(path)) {
            return JsonMessage.getString(405, "没有配置此文件夹");
        }
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "name error");
        }
        Session session = null;
        ChannelSftp channel = null;
        try {
            String normalize = FileUtil.normalize(path + "/" + name);
            session = sshService.getSession(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            //删除文件或者文件夹
            deleteFile(channel, normalize);
            return JsonMessage.getString(200, "删除成功");
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("ssh删除文件异常", e);
            return JsonMessage.getString(400, "删除失败");
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 删除文件或者文件夹
     *
     * @param channel channel
     * @param path    文件路径
     * @throws SftpException SftpException
     */
    private void deleteFile(ChannelSftp channel, String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> vector = channel.ls(path);
        if (null == vector) {
            return;
        }
        int size = vector.size();
        if (size == 1) {
            // 文件，直接删除
            channel.rm(path);
        } else if (size == 2) {
            // 空文件夹，直接删除
            channel.rmdir(path);
        } else {
            // 删除文件夹下所有文件
            String fileName;
            for (ChannelSftp.LsEntry en : vector) {
                fileName = en.getFilename();
                if (!".".equals(fileName) && !"..".equals(fileName)) {
                    deleteFile(channel, path + "/" + fileName);
                }
            }
            channel.rmdir(path);
        }
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.UPLOAD)
    public String upload(String id, String path, String name) {
        SshModel sshModel = sshService.getItem(id);
        if (sshModel == null) {
            return JsonMessage.getString(400, "ssh error");
        }
        List<String> fileDirs = sshModel.getFileDirs();
        if (StrUtil.isEmpty(path) || !fileDirs.contains(path)) {
            return JsonMessage.getString(400, "没有配置此文件夹");
        }
        Session session = null;
        ChannelSftp channel = null;
        String localPath = null;
        try {
            session = sshService.getSession(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            MultipartFileBuilder multipartFileBuilder = createMultipart().addFieldName("file").setUseOriginalFilename(true);
            localPath = multipartFileBuilder.save();
            File file = FileUtil.file(localPath);
            String normalize = FileUtil.normalize(path + "/" + name);
            channel.cd(normalize);
            channel.put(IoUtil.toStream(file), file.getName());
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("ssh上传文件异常", e);
            return JsonMessage.getString(400, "上传失败");
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
            FileUtil.del(localPath);
        }
        return JsonMessage.getString(200, "上传成功");
    }

}
