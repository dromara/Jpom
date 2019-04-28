package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ConsoleService;
import cn.keepbx.jpom.socket.ConsoleCommandOp;
import cn.keepbx.jpom.system.AgentConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/file/")
public class ProjectFileControl extends BaseAgentController {
    @Resource
    private ConsoleService consoleService;

    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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

    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String upload() throws Exception {
        ProjectInfoModel pim = getProjectInfoModel();
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file");
        String type = getParameter("type");
        if ("unzip".equals(type)) {
            multipartFileBuilder.setInputStreamType("zip");
            multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
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
        //
        String after = getParameter("after");
        if ("restart".equalsIgnoreCase(after)) {
            String result = consoleService.execCommand(ConsoleCommandOp.restart, pim);
            return JsonMessage.getString(200, "上传成功并重启：" + result);
        }

        return JsonMessage.getString(200, "上传成功");
    }


    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteFile(String filename, String type) {
        ProjectInfoModel pim = getProjectInfoModel();
        if ("clear".equalsIgnoreCase(type)) {
            // 清空文件
            File file = new File(pim.getLib());
            if (FileUtil.clean(file)) {
                return JsonMessage.getString(200, "清除成功");
            }
            if (pim.isStatus(true)) {
                return JsonMessage.getString(501, "文件被占用，请先停止项目");
            }
            return JsonMessage.getString(500, "删除失败：" + file.getAbsolutePath());
        } else {
            // 删除文件
            String fileName = pathSafe(filename);
            if (StrUtil.isEmpty(fileName)) {
                return JsonMessage.getString(405, "非法操作");
            }
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

    @RequestMapping(value = "download", method = RequestMethod.GET)
    public String download(String id, String filename) {
        String safeFileName = pathSafe(filename);
        if (StrUtil.isEmpty(safeFileName)) {
            return JsonMessage.getString(405, "非法操作");
        }
        try {
            ProjectInfoModel pim = projectInfoService.getItem(id);
            File file = FileUtil.file(pim.getLib(), safeFileName);
            if (file.isDirectory()) {
                return "暂不支持下载文件夹";
            }
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    @RequestMapping(value = "lsFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String listFile(String id) {
        // 查询项目路径
        ProjectInfoModel pim = projectInfoService.getItem(id);
        if (pim == null) {
            return JsonMessage.getString(500, "查询失败：项目不存在");
        }
        String path = pim.getLib();
        File fileDir = new File(path);
        if (!fileDir.exists()) {
            return JsonMessage.getString(500, "目录不存在");
        }
        JSONObject object = lsFile(path);
        return JsonMessage.getString(200, "", object);
    }

    /**
     * 递归查询文件夹下的所有文件夹和文件
     *
     * @param path 文件路径
     * @return JSONObject
     */
    private JSONObject lsFile(String path) {
        File parentFile = FileUtil.file(path);
        JSONObject object = new JSONObject();
        object.put("filename", parentFile.getName());
        long mTime = parentFile.lastModified();
        object.put("modifytime", DateUtil.date(mTime).toString());
        object.put("modifytimelong", mTime);
        if (parentFile.isFile()) {
            object.put("filesize", FileUtil.readableFileSize(parentFile.length()));
            return object;
        } else {
            object.put("isDirectory", true);
            long sizeFile = FileUtil.size(parentFile);
            object.put("filesize", FileUtil.readableFileSize(sizeFile));
        }
        File[] ls = FileUtil.ls(path);
        if (ls == null) {
            return object;
        }
        JSONArray array = new JSONArray();
        JSONObject item;
        for (int i = 0; i < ls.length; i++) {
            File file = ls[i];
            if (file.isDirectory()) {
                //文件夹 递归查询
                item = lsFile(file.getAbsolutePath());
                item.put("isDirectory", true);
            } else {
                item = new JSONObject();
                item.put("filename", file.getName());
                item.put("filesize", FileUtil.readableFileSize(file.length()));
                long time = file.lastModified();
                item.put("modifytimelong", time);
                item.put("modifytime", DateUtil.date(time).toString());
            }
            item.put("index", i + 1);
            array.add(item);
        }
        object.put("open", true);
        object.put("children", array);
        return object;
    }
}
