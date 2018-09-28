package cn.jiangzeyin.controller.manage;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/file/")
public class FileControl extends AbstractBaseControl {

    @Resource
    private ManageService manageService;
    @Resource
    private CommandService commandService;

    /**
     * 文件管理页面
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "filemanage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String fileManage(String id) throws IOException {
        setAttribute("id", id);
        String logSize = null;
        ProjectInfoModel pim = manageService.getProjectInfo(id);
        File file = new File(pim.getLog());
        if (file.exists()) {
            logSize = FileUtil.readableFileSize(file);
        }
        setAttribute("logSize", logSize);
        return "manage/filemanage";
    }

    /**
     * 读取启动文件
     *
     * @return json
     */
    @RequestMapping(value = "getRunBoot")
    @ResponseBody
    public String getRunBoot() {
        File file = commandService.getCommandFile();
        String content = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
        Map<String, String> map = new HashMap<>();
        map.put("content", content);
        return JsonMessage.getString(200, "success", map);
    }

    /**
     * 修改启动文件
     *
     * @param content
     * @return
     */
    @RequestMapping(value = "saveRunBoot")
    @ResponseBody
    public String saveRunBoot(String content) {
        File file = commandService.getCommandFile();
        // 写入文件
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        return JsonMessage.getString(200, "success");
    }

    /**
     * 列出目录下的文件
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getFileList(String id) {
        try {
            // 查询项目路径
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            File fileDir = new File(pim.getLib());
            if (!fileDir.exists()) {
                return JsonMessage.getString(500, "目录不存在");
            }
            File[] filesAll = fileDir.listFiles();
            if (filesAll == null) {
                return JsonMessage.getString(500, "目录是空");
            }
            int size = filesAll.length;
            JSONArray arrayFile = new JSONArray(size);
            for (File aFilesAll : filesAll) {
                JSONObject jsonObject = new JSONObject(6);
                jsonObject.put("filename", aFilesAll.getName());
                jsonObject.put("projectid", id);
                jsonObject.put("modifytime", DateUtil.date(aFilesAll.lastModified()).toString());
                jsonObject.put("filesize", FileUtil.readableFileSize(aFilesAll.length()));
                arrayFile.add(jsonObject);
            }
            arrayFile.sort((o1, o2) -> {
                JSONObject jsonObject1 = (JSONObject) o1;
                JSONObject jsonObject2 = (JSONObject) o2;
                return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
            });
            final int[] i = {0};
            arrayFile.forEach(o -> {
                JSONObject jsonObject = (JSONObject) o;
                jsonObject.put("index", ++i[0]);
            });
            return PageUtil.getPaginate(200, "查询成功", arrayFile);
        } catch (IOException e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, "查询失败");
        }
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload")
    @ResponseBody
    public String upload() {
        String id = getParameter("id");
        List<MultipartFile> files = getFiles("file");
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            for (MultipartFile file : files) {
                if (null != file) {
                    File saveFile = new File(pim.getLib(), file.getOriginalFilename());
                    FileUtil.writeFromStream(file.getInputStream(), saveFile);
                }
            }
            return JsonMessage.getString(200, "上传成功");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, "上传失败:" + e.getMessage());
        }
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public String export(String id) {
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            File file = new File(pim.getLog());
            if (!file.exists()) {
                return JsonMessage.getString(400, "没有日志文件:" + file.getPath());
            }
            HttpServletResponse response = getResponse();
            // 设置强制下载不打开
            response.setContentType("application/force-download");
            // 设置文件名
            response.addHeader("Content-Disposition", "attachment;fileName=" + file.getName());
            OutputStream os = response.getOutputStream();
            byte[] bytes = IoUtil.readBytes(new FileInputStream(file));
            IoUtil.write(os, false, bytes);
            os.flush();
            os.close();
            return "ok";
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "导出失败");
    }

    @RequestMapping(value = "clear")
    @ResponseBody
    public String clear(String id) {
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            File file = new File(pim.getLib());
            if (FileUtil.clean(file)) {
                return JsonMessage.getString(200, "清除成功");
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "删除失败");
    }


    /**
     * 删除文件
     *
     * @param filename
     * @return
     */
    @RequestMapping(value = "deleteFile")
    @ResponseBody
    public String deleteFile(String id, String filename) {
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            File file = new File(pim.getLib(), filename);
            if (file.exists() && file.delete()) {
                return JsonMessage.getString(200, "删除成功");
            } else {
                return JsonMessage.getString(404, "文件不存在");
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "删除失败");
    }
}
