package cn.keepbx.jpom.controller.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.service.node.ssh.SshService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * @author bwcx_jzy
 * @date 2019/8/10
 */
@Controller
@RequestMapping(value = "/node/ssh")
public class SshFileController extends BaseServerController {

    @Resource
    private SshService sshService;

    @RequestMapping(value = "file.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String file(String id) throws IOException {
        SshModel sshModel = sshService.getItem(id);
        if (sshModel != null) {
            List<String> fileDirs = sshModel.getFileDirs();
            if (fileDirs != null && !fileDirs.isEmpty()) {
                try {
                    JSONArray jsonArray = listDir(sshModel, fileDirs);
                    setAttribute("dirs", jsonArray);
                } catch (Exception e) {
                    DefaultSystemLog.ERROR().error("sftp错误", e);
                }
            }
        }
        return "node/ssh/file";
    }

    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String listData(String id, String path, String children, String parentIndexKey) throws IOException, SftpException {
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
        JSONArray jsonArray = listDir(sshModel, path, children, parentIndexKey);
        return JsonMessage.getString(200, "ok", jsonArray);
    }

    private JSONArray listDir(SshModel sshModel, String path, String children, String parentIndexKey) throws SftpException {
        Session session = null;
        ChannelSftp channel = null;
        int level = StrUtil.split(children, StrUtil.SLASH).length;
        try {
            session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
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
                if (lsEntry.getAttrs().isDir()) {
                    jsonObject.put("title", StrUtil.format("{}【文件夹】", lsEntry.getFilename()));
                    jsonObject.put("dir", true);
                } else {
                    jsonObject.put("title", lsEntry.getFilename());
                }
                int index = jsonArray.size();
                if (StrUtil.isEmpty(parentIndexKey)) {
                    jsonObject.put("parentIndexKey", index);

                } else {
                    jsonObject.put("parentIndexKey", StrUtil.format("{},{}", parentIndexKey, index));
                }
                //
                if (StrUtil.isEmpty(children)) {
                    jsonObject.put("parentDir", lsEntry.getFilename());
                } else {
                    jsonObject.put("parentDir", StrUtil.format("{}/{}", children, lsEntry.getFilename()));
                }
                jsonObject.put("level", level);
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
            session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            JSONArray jsonArray = new JSONArray();
            for (String item : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", item);
                try {
                    channel.ls(item);
                } catch (SftpException e) {
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
}
