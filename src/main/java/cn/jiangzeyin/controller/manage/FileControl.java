package cn.jiangzeyin.controller.manage;


import cn.jiangzeyin.DateUtil;
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
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
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

        StringBuilder sb = new StringBuilder();
        // 读取文件
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str_temp;

            while ((str_temp = br.readLine()) != null) {

                sb.append(str_temp).append("\r\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> map = new HashMap<>();
        map.put("content", sb.toString());
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

            File file_dir = new File(pim.getLib());

            if (!file_dir.exists()) {
                return JsonMessage.getString(500, "目录不存在");
            }

            File[] files_all = file_dir.listFiles();
            List<Map<String, String>> list_file = new ArrayList<>();
            for (File file : files_all) {
                Map<String, String> map_file = new HashMap<>();
                map_file.put("filename", file.getName());
                map_file.put("projectid", id);
                map_file.put("modifytime", DateUtil.formatTime("yyyy-MM-dd HH:mm:ss", file.lastModified()));
                map_file.put("filesize", file.length() + "");
                list_file.add(map_file);
            }

            JSONArray array_file = JSONArray.parseArray(JSONArray.toJSONString(list_file));

            return PageUtil.getPaginate(200, "查询成功", array_file);
        } catch (IOException e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, "查询失败");
        }
    }

    /**
     * 上传文件
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "upload")
    @ResponseBody
    public String upload(HttpServletRequest request) {
        String id = getRequest().getParameter("id");
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);

            for (MultipartFile file : files) {
                if (null != file) {
                    File saveFile = new File(pim.getLib() + "/" + file.getOriginalFilename());

                    file.transferTo(saveFile);
                }
            }
            return JsonMessage.getString(200, "上传成功");
        } catch (IOException e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, "上传失败");
        }
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
                file.delete();
                return JsonMessage.getString(200, "删除成功");
            } else {
                return JsonMessage.getString(404, "文件不存在");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return JsonMessage.getString(500, "删除失败");
        }
    }

}
