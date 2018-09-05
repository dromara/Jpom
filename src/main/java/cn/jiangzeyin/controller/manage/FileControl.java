package cn.jiangzeyin.controller.manage;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/file/")
public class FileControl extends AbstractBaseControl {

    @Resource
    private ManageService manageService;

    /**
     * 文件管理页面
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "filemanage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String fileManage(String id) {
        setAttribute("id", id);
        return "manage/filemanage";
    }

    /**
     * 读取启动文件
     *
     * @return
     */
    @RequestMapping(value = "getRunBoot")
    @ResponseBody
    public String getRunBoot() {
        File file = new File(SpringUtil.getEnvironment().getProperty("command.conf"));
        if (!file.exists()) {
            return JsonMessage.getString(500, "启动文件不存在");
        }
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
        File file = new File(SpringUtil.getEnvironment().getProperty("command.conf"));

        if (!file.exists()) {
            return JsonMessage.getString(500, "启动文件不存在");
        }

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
            List<Map<String, String>> listFile = new ArrayList<>();
            if (filesAll != null) {
                for (int i = 0, size = filesAll.length; i < size; i++) {
                    File file = filesAll[i];
                    Map<String, String> mapFile = new HashMap<>(size);
                    mapFile.put("index", String.valueOf(i + 1));
                    mapFile.put("filename", file.getName());
                    mapFile.put("projectid", id);
                    mapFile.put("modifytime", DateUtil.date(file.lastModified()).toString());
                    mapFile.put("filesize", FileUtil.readableFileSize(file.length()));
                    listFile.add(mapFile);
                }
            }
            JSONArray arrayFile = (JSONArray) JSONArray.toJSON(listFile);
            return PageUtil.getPaginate(200, "查询成功", arrayFile);
        } catch (IOException e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, "查询失败");
        }
    }

    /**
     * 上传文件
     *
     * @return
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
                    File saveFile = new File(pim.getLib() + "/" + file.getOriginalFilename());
                    FileUtil.writeFromStream(file.getInputStream(), saveFile);
                }
            }
            return JsonMessage.getString(200, "上传成功");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, "上传失败:" + e.getMessage());
        }
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
            File file = new File(pim.getLib() + "/" + filename);
            if (file.exists()) {
                if (file.delete()) {
                    return JsonMessage.getString(200, "删除成功");
                }
            } else {
                return JsonMessage.getString(404, "文件不存在");
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "删除失败");
    }

}
