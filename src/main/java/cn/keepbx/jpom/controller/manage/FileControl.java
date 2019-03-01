package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.PageUtil;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ManageService;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * 文件管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/file/")
public class FileControl extends BaseController {

    @Resource
    private ManageService manageService;

    /**
     * 文件管理页面
     *
     * @param id 项目id
     */
    @RequestMapping(value = "filemanage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String fileManage(String id) {
        setAttribute("id", id);
        return "manage/filemanage";
    }

//    /**
//     * 读取启动文件
//     *
//     * @return json
//     */
//    @RequestMapping(value = "getRunBoot")
//    @ResponseBody
//    public String getRunBoot() throws ConfigException {
//        String file = ConfigBean.getInstance().getRunCommandPath();
//        String content = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
//        Map<String, String> map = new HashMap<>(1);
//        map.put("content", content);
//        return JsonMessage.getString(200, "success", map);
//    }

    /**
     * 列出目录下的文件
     *
     * @param id 项目id
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getFileList(String id) {
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(400, "你没有该操作权限操作!");
        }
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
    public String upload(String id) throws Exception {
//        String id = getParameter("id");
//        boolean manager = userService.isManager(id, getUserName());
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(400, "你没有该操作权限操作!");
        }
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

    /**
     * 下载文件
     *
     * @return File
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    public String download() {
        String id = getParameter("id");
        String filename = getParameter("filename");
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
            String path = pim.getLib() + "/" + filename;
            File file = new File(path);
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }


    @RequestMapping(value = "clear", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String clear(String id) {
        if (ConfigBean.getInstance().safeMode) {
            return JsonMessage.getString(400, "安全模式不能清除文件");
        }
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(400, "你没有对应操作权限操作!");
        }
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
     * @param filename 文件名称
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String deleteFile(String id, String filename) {
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(400, "你没有对应操作权限操作!");
        }
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


//    /**
//     * 修改启动文件
//     *
//     * @param content 内容
//     * @return json
//     */
//    @RequestMapping(value = "saveRunBoot")
//    @ResponseBody
//    public String saveRunBoot(String content) {
//        File file = commandService.getCommandFile();
//        // 写入文件
//        try {
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(content.getBytes());
//            fos.flush();
//            fos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            DefaultSystemLog.ERROR().error(e.getMessage(), e);
//        }
//        return JsonMessage.getString(200, "success");
//    }

}
