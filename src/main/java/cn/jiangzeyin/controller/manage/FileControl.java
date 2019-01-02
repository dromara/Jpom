package cn.jiangzeyin.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
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

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/file/")
public class FileControl extends BaseController {

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
    public String fileManage(String id) {
        setAttribute("id", id);
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
        Map<String, String> map = new HashMap<>(1);
        map.put("content", content);
        return JsonMessage.getString(200, "success", map);
    }

    /**
     * 修改启动文件
     *
     * @param content 内容
     * @return json
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
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
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
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, "查询失败");
        }
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String upload() throws Exception {
        String id = getParameter("id");
        ProjectInfoModel pim = manageService.getProjectInfo(id);
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file")
                .setSavePath(pim.getLib())
                .setUseOriginalFilename(true);
        // 保存
        multipartFileBuilder.save();
        // 修改使用状态
        ProjectInfoModel modify = new ProjectInfoModel();
        modify.setId(pim.getId());
        modify.setUseLibDesc("upload");
        manageService.updateProject(modify);
        return JsonMessage.getString(200, "上传成功");
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
