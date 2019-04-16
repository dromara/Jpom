package cn.keepbx.jpom.controller.node.manage.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
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

/**
 * 文件管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/file/")
public class ProjectFileControl extends BaseController {

    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 文件管理页面
     *
     * @param id 项目id
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String fileManage(String id) {
        setAttribute("id", id);
        return "node/manage/filemanage";
    }

    /**
     * 列出目录下的文件
     *
     * @param id 项目id
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    public String getFileList(String id) {
        // 查询项目路径
        ProjectInfoModel pim = projectInfoService.getItem(id);
        if (pim == null) {
            return JsonMessage.getString(500, "查询失败：项目不存在");
        }
        File fileDir = new File(pim.getLib());
        if (!fileDir.exists()) {
            return JsonMessage.getString(500, "目录不存在");
        }
        File[] filesAll = fileDir.listFiles();
        if (filesAll == null) {
            return JsonMessage.getString(500, "目录是空");
        }
        JSONArray arrayFile = parseInfo(filesAll, false);
        return JsonMessage.getString(200, "查询成功", arrayFile);
    }

    public static JSONArray parseInfo(File[] files, boolean time) {
        int size = files.length;
        JSONArray arrayFile = new JSONArray(size);
        for (File file : files) {
            JSONObject jsonObject = new JSONObject(6);
            if (file.isDirectory()) {
                jsonObject.put("isDirectory", true);
                long sizeFile = FileUtil.size(file);
                jsonObject.put("filesize", FileUtil.readableFileSize(sizeFile));
            } else {
                jsonObject.put("filesize", FileUtil.readableFileSize(file.length()));
            }
            jsonObject.put("filename", file.getName());
            long mTime = file.lastModified();
            jsonObject.put("modifytimelong", mTime);
            jsonObject.put("modifytime", DateUtil.date(mTime).toString());
            arrayFile.add(jsonObject);
        }
        arrayFile.sort((o1, o2) -> {
            JSONObject jsonObject1 = (JSONObject) o1;
            JSONObject jsonObject2 = (JSONObject) o2;
            if (time) {
                return jsonObject2.getLong("modifytimelong").compareTo(jsonObject1.getLong("modifytimelong"));
            }
            return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
        });
        final int[] i = {0};
        arrayFile.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            jsonObject.put("index", ++i[0]);
        });
        return arrayFile;
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkUpload = true)
    public String upload() throws Exception {
        ProjectInfoModel pim = getProjectInfoModel();
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file");
        String type = getParameter("type");
        if ("unzip".equals(type)) {
            multipartFileBuilder.setSavePath(ConfigBean.getInstance().getTempPathName());
            String path = multipartFileBuilder.save();
            //
            File lib = new File(pim.getLib());
            if (!FileUtil.clean(lib)) {
                return JsonMessage.getString(500, "清除旧lib失败");
            }
            File file = new File(path);
            ZipUtil.unzip(file, lib);
            if (!file.delete()) {
                DefaultSystemLog.LOG().info("删除失败：" + file.getPath());
            }
        } else {
            multipartFileBuilder.setSavePath(pim.getLib())
                    .setUseOriginalFilename(true);
            // 保存
            multipartFileBuilder.save();
        }
        // 修改使用状态
        pim.setUseLibDesc("upload");
        projectInfoService.updateItem(pim);
        return JsonMessage.getString(200, "上传成功");
    }

    /**
     * 下载文件
     *
     * @param id 项目id
     * @return File
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    public String download(String id, String filename) {
        filename = pathSafe(filename);
        if (StrUtil.isEmpty(filename)) {
            return JsonMessage.getString(405, "非法操作");
        }
        try {
            ProjectInfoModel pim = projectInfoService.getItem(id);
//            String path = + "/" + filename;
            File file = FileUtil.file(pim.getLib(), filename);
            if (file.isDirectory()) {
                return "暂不支持下载文件夹";
            }
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    /**
     * 清除文件
     *
     * @return json
     */
    @RequestMapping(value = "clear", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkDelete = true)
    public String clear() {
        ProjectInfoModel pim = getProjectInfoModel();
        File file = new File(pim.getLib());
        if (FileUtil.clean(file)) {
            return JsonMessage.getString(200, "清除成功");
        }
        if (pim.isStatus(true)) {
            return JsonMessage.getString(501, "文件被占用，请先停止项目");
        }
        return JsonMessage.getString(500, "删除失败：" + file.getAbsolutePath());
    }


    /**
     * 删除文件
     *
     * @param filename 文件名称
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkDelete = true)
    public String deleteFile(String filename) {
        String fileName = pathSafe(filename);
        if (StrUtil.isEmpty(fileName)) {
            return JsonMessage.getString(405, "非法操作");
        }
        ProjectInfoModel pim = getProjectInfoModel();
        File file = FileUtil.file(pim.getLib(), fileName);
        if (file.exists()) {
            if (FileUtil.del(file)) {
                return JsonMessage.getString(200, "删除成功");
            }
        } else {
            return JsonMessage.getString(404, "文件不存在");
        }
        return JsonMessage.getString(500, "删除失败");
    }
}
